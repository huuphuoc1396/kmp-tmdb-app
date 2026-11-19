@testable import TmdbNative
import TmdbCore

@MainActor
final class TitleListsStub: TitleLists {
    struct RefreshRequest: Hashable {
        let key: TitleListKey
        let force: Bool
    }

    /// Result of `refresh` per list; `true` when missing.
    var refreshResults: [TitleListKey: Bool] = [:]
    /// While `true`, `refresh` calls wait until `releaseRefreshes()`.
    var holdRefreshes = false
    private(set) var refreshRequests: [RefreshRequest] = []
    private(set) var observedKeys: [TitleListKey] = []

    private var values: [TitleListKey: [TitleItem]?] = [:]
    private var observers: [TitleListKey: [AsyncStream<[TitleItem]?>.Continuation]] = [:]
    private var heldRefreshes: [CheckedContinuation<Void, Never>] = []

    /// Sets the cached value of a list; observers get it now and new observers get it first.
    func send(_ titles: [TitleItem]?, for key: TitleListKey) {
        values[key] = .some(titles)
        observers[key]?.forEach { $0.yield(titles) }
    }

    func releaseRefreshes() {
        holdRefreshes = false
        heldRefreshes.forEach { $0.resume() }
        heldRefreshes = []
    }

    func observe(_ key: TitleListKey) -> AsyncStream<[TitleItem]?> {
        observedKeys.append(key)
        let (stream, continuation) = AsyncStream.makeStream(of: [TitleItem]?.self)
        continuation.yield(values[key] ?? nil)
        observers[key, default: []].append(continuation)
        return stream
    }

    func refresh(_ key: TitleListKey, force: Bool) async -> Bool {
        refreshRequests.append(RefreshRequest(key: key, force: force))
        if holdRefreshes {
            await withCheckedContinuation { heldRefreshes.append($0) }
        }
        return refreshResults[key] ?? true
    }
}

func listKey(_ category: TitleCategory, _ listType: TitleListType, _ language: Language = .enUs) -> TitleListKey {
    TitleListKey(category: category, listType: listType, language: language)
}

/// A Movies list.
func listKey(_ listType: TitleListType, _ language: Language = .enUs) -> TitleListKey {
    listKey(.movies, listType, language)
}

func titles(_ prefix: String, count: Int = 2) -> [TitleItem] {
    (1...count).map { TitleItem(id: "movie-\(prefix)\($0)", name: "\(prefix) \($0)", posterURL: nil) }
}

/// Waits until `condition` holds, for at most `timeout`; `false` when it never did.
@MainActor
func eventually(timeout: Duration = .seconds(2), _ condition: () -> Bool) async -> Bool {
    let deadline = ContinuousClock.now + timeout
    while !condition() {
        if ContinuousClock.now > deadline { return false }
        try? await Task.sleep(for: .milliseconds(10))
    }
    return true
}

/// Lets started tasks run, for checks that something did not happen.
func settle() async {
    try? await Task.sleep(for: .milliseconds(100))
}
