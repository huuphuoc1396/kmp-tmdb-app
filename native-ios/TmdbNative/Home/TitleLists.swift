import Foundation
import TmdbCore

/// A title as Home shows it; `id` is unique across films and series (`movie-550`, `tv-1396`).
struct TitleItem: Hashable, Identifiable {
    let id: String
    let name: String
    let posterURL: URL?
}

struct TitleListKey: Hashable {
    let category: TitleCategory
    let listType: TitleListType
    let language: Language
}

@MainActor
protocol TitleLists {
    /// Cached titles of the list; `nil` until a refresh of this list and language has succeeded.
    func observe(_ key: TitleListKey) -> AsyncStream<[TitleItem]?>

    /// Fetches the list when stale or `force`d; `false` when it failed (the cache is kept).
    func refresh(_ key: TitleListKey, force: Bool) async -> Bool
}

@MainActor
struct SharedCoreTitleLists: TitleLists {
    func observe(_ key: TitleListKey) -> AsyncStream<[TitleItem]?> {
        let flow = SharedCore.shared.titleListsRepository.observe(
            category: key.category,
            listType: key.listType,
            language: key.language
        )
        return AsyncStream { continuation in
            let task = Task { @MainActor in
                for await titles in flow {
                    continuation.yield(titles.map { $0.map(TitleItem.init) })
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }

    func refresh(_ key: TitleListKey, force: Bool) async -> Bool {
        let result = try? await SharedCore.shared.titleListsRepository.refresh(
            category: key.category,
            listType: key.listType,
            language: key.language,
            force: force
        )
        return result is AppResultSuccess<AnyObject>
    }
}

private extension TitleItem {
    init(_ title: Title) {
        self.init(
            id: "\(title.id.type.name.lowercased())-\(title.id.tmdbId)",
            name: title.name,
            posterURL: title.posterUrl.flatMap(URL.init(string:))
        )
    }
}
