import Foundation
import TmdbCore

struct FavoriteTitleItem: Hashable, Identifiable {
    let id: String
    let name: String?
    let posterURL: URL?
    let releaseYear: Int?
    let firstGenre: String?

    var titleItem: TitleItem {
        TitleItem(id: id, name: name ?? "", posterURL: posterURL)
    }

    var meta: String {
        metaLine(year: releaseYear, genre: firstGenre)
    }
}

extension FavoriteTitleItem {
    init(_ favorite: FavoriteTitle) {
        let summary = favorite.summary
        self.init(
            id: TitleItem.id(for: favorite.id),
            name: summary?.name,
            posterURL: summary?.posterUrl.flatMap(URL.init(string:)),
            releaseYear: summary?.releaseYear.map { Int($0.int32Value) },
            firstGenre: summary?.firstGenre
        )
    }
}

@MainActor
protocol FavoriteTitles {
    func observe(_ language: Language) -> AsyncStream<[FavoriteTitleItem]>
}

@MainActor
struct SharedCoreFavoriteTitles: FavoriteTitles {
    func observe(_ language: Language) -> AsyncStream<[FavoriteTitleItem]> {
        let flow = SharedCore.shared.observeFavoriteTitles.invoke(language: language)
        return AsyncStream { continuation in
            let task = Task { @MainActor in
                for await favorites in flow {
                    continuation.yield(favorites.map(FavoriteTitleItem.init))
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }
}
