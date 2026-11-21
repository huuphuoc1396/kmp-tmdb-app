import Foundation
import TmdbCore

/// A calendar date as TMDB gives it, with no time zone.
struct ReleaseDate: Equatable {
    let year: Int
    let month: Int
    let day: Int
}

struct CastItem: Equatable {
    let name: String
    let photoURL: URL?
}

/// Everything the details screen shows about a title, in one language; a `nil` field is unknown on TMDB.
struct TitleDetailsItem: Equatable {
    let name: String
    /// Release date of a film, first air date of a series.
    let releaseDate: ReleaseDate?
    /// Runtime of a film, or of one episode of a series.
    let runtimeMinutes: Int?
    /// `nil` when nobody has voted.
    let ratingPercent: Int?
    let synopsis: String?
    /// Poster, or the backdrop when there is no poster.
    let artworkURL: URL?
    let genres: [String]
    let cast: [CastItem]
    let trailerURL: URL?
    let recommendations: [TitleItem]
}

@MainActor
protocol TitleDetailsSource {
    /// Cached details; `nil` until a refresh of this title and language has succeeded.
    func observe(_ id: TitleId, language: Language) -> AsyncStream<TitleDetailsItem?>

    /// Fetches the details when stale or `force`d; `false` when it failed (the cache is kept).
    func refresh(_ id: TitleId, language: Language, force: Bool) async -> Bool
}

@MainActor
struct SharedCoreTitleDetails: TitleDetailsSource {
    func observe(_ id: TitleId, language: Language) -> AsyncStream<TitleDetailsItem?> {
        let flow = SharedCore.shared.titleDetailsRepository.observe(id: id, language: language)
        return AsyncStream { continuation in
            let task = Task { @MainActor in
                for await details in flow {
                    continuation.yield(details.map(TitleDetailsItem.init))
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }

    func refresh(_ id: TitleId, language: Language, force: Bool) async -> Bool {
        await Self.refresh(tmdbId: id.tmdbId, type: id.type, language: language, force: force)
    }

    /// Kotlin objects aren't `Sendable`, so the id is built again off the main actor from its parts.
    private nonisolated static func refresh(
        tmdbId: Int64,
        type: TitleType,
        language: Language,
        force: Bool
    ) async -> Bool {
        let id = TitleId(tmdbId: tmdbId, type: type)
        let repository = SharedCore.shared.titleDetailsRepository
        let result = try? await repository.refresh(id: id, language: language, force: force)
        return result is AppResultSuccess<AnyObject>
    }
}

private extension TitleDetailsItem {
    init(_ details: TitleDetails) {
        self.init(
            name: details.name,
            releaseDate: details.releaseDate.map {
                ReleaseDate(year: Int($0.year), month: Int($0.month.ordinal) + 1, day: Int($0.day))
            },
            runtimeMinutes: details.runtimeMinutes.map { Int(truncating: $0) },
            ratingPercent: details.ratingPercent.map { Int(truncating: $0) },
            synopsis: details.synopsis,
            artworkURL: details.artworkUrl.flatMap(URL.init(string:)),
            genres: details.genres,
            cast: details.cast.map { CastItem(name: $0.name, photoURL: $0.photoUrl.flatMap(URL.init(string:))) },
            trailerURL: details.trailer.flatMap { URL(string: $0.url) },
            recommendations: details.recommendations.map(TitleItem.init)
        )
    }
}
