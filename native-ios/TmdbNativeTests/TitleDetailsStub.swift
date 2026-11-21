import Foundation
@testable import TmdbNative
import TmdbCore

@MainActor
final class TitleDetailsStub: TitleDetailsSource {
    struct RefreshRequest: Equatable {
        let language: Language
        let force: Bool
    }

    /// Result of `refresh` per language; `true` when missing.
    var refreshResults: [Language: Bool] = [:]
    /// While `true`, `refresh` calls wait until `releaseRefreshes()`.
    var holdRefreshes = false
    private(set) var refreshRequests: [RefreshRequest] = []
    private(set) var observedLanguages: [Language] = []

    private var values: [Language: TitleDetailsItem?] = [:]
    private var observers: [Language: [AsyncStream<TitleDetailsItem?>.Continuation]] = [:]
    private var heldRefreshes: [CheckedContinuation<Void, Never>] = []

    /// Sets the cached details in a language; observers get them now and new observers get them first.
    func send(_ details: TitleDetailsItem?, for language: Language) {
        values[language] = .some(details)
        observers[language]?.forEach { $0.yield(details) }
    }

    func releaseRefreshes() {
        holdRefreshes = false
        heldRefreshes.forEach { $0.resume() }
        heldRefreshes = []
    }

    func observe(_: TitleId, language: Language) -> AsyncStream<TitleDetailsItem?> {
        observedLanguages.append(language)
        let (stream, continuation) = AsyncStream.makeStream(of: TitleDetailsItem?.self)
        continuation.yield(values[language] ?? nil)
        observers[language, default: []].append(continuation)
        return stream
    }

    func refresh(_: TitleId, language: Language, force: Bool) async -> Bool {
        refreshRequests.append(RefreshRequest(language: language, force: force))
        if holdRefreshes {
            await withCheckedContinuation { heldRefreshes.append($0) }
        }
        return refreshResults[language] ?? true
    }
}

@MainActor
final class FavoritesStub: Favorites {
    private(set) var writes: [Bool] = []
    private var value: Bool?
    private var observers: [AsyncStream<Bool>.Continuation] = []

    /// Sets the stored value; observers get it now and new observers get it first. `nil` sends nothing yet.
    init(stored: Bool? = false) {
        value = stored
    }

    func store(_ isFavorite: Bool) {
        value = isFavorite
        observers.forEach { $0.yield(isFavorite) }
    }

    func observeIsFavorite(_: TitleId) -> AsyncStream<Bool> {
        let (stream, continuation) = AsyncStream.makeStream(of: Bool.self)
        if let value { continuation.yield(value) }
        observers.append(continuation)
        return stream
    }

    func setFavorite(_: TitleId, _ isFavorite: Bool) async {
        writes.append(isFavorite)
        store(isFavorite)
    }
}

/// A new instance each time: Kotlin objects aren't `Sendable`, so they can't be shared globals.
var detailsTitleId: TitleId { TitleId(tmdbId: 550, type: .movie) }

func details(
    _ name: String = "Fight Club",
    trailerURL: URL? = URL(string: "https://www.youtube.com/watch?v=key"),
    recommendations: [TitleItem] = []
) -> TitleDetailsItem {
    TitleDetailsItem(
        name: name,
        releaseDate: ReleaseDate(year: 1999, month: 10, day: 15),
        runtimeMinutes: 139,
        ratingPercent: 84,
        synopsis: "A synopsis.",
        artworkURL: nil,
        genres: ["Drama"],
        cast: [CastItem(name: "Brad Pitt", photoURL: nil)],
        trailerURL: trailerURL,
        recommendations: recommendations
    )
}
