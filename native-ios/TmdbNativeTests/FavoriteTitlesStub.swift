import Foundation
@testable import TmdbNative
import TmdbCore

@MainActor
final class FavoriteTitlesStub: FavoriteTitles {
    private(set) var observedLanguages: [Language] = []
    private var values: [Language: [FavoriteTitleItem]] = [:]
    private var observers: [Language: [AsyncStream<[FavoriteTitleItem]>.Continuation]] = [:]

    func send(_ favorites: [FavoriteTitleItem], for language: Language) {
        values[language] = favorites
        observers[language]?.forEach { $0.yield(favorites) }
    }

    func observe(_ language: Language) -> AsyncStream<[FavoriteTitleItem]> {
        observedLanguages.append(language)
        let (stream, continuation) = AsyncStream.makeStream(of: [FavoriteTitleItem].self)
        continuation.yield(values[language] ?? [])
        observers[language, default: []].append(continuation)
        return stream
    }
}

func favorite(_ tmdbId: Int64, _ name: String? = nil) -> FavoriteTitleItem {
    FavoriteTitleItem(
        id: TitleItem.id(for: TitleId(tmdbId: tmdbId, type: .movie)),
        name: name ?? "Title \(tmdbId)",
        posterURL: nil,
        releaseYear: 2021,
        firstGenre: "Drama"
    )
}
