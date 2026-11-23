import TmdbCore

@MainActor
protocol Favorites {
    func observeIsFavorite(_ id: TitleId) -> AsyncStream<Bool>

    func setFavorite(_ id: TitleId, _ isFavorite: Bool) async
}

@MainActor
struct SharedCoreFavorites: Favorites {
    func observeIsFavorite(_ id: TitleId) -> AsyncStream<Bool> {
        let flow = SharedCore.shared.favoritesRepository.observeIsFavorite(id: id)
        return AsyncStream { continuation in
            let task = Task { @MainActor in
                for await isFavorite in flow {
                    continuation.yield(isFavorite.boolValue)
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }

    func setFavorite(_ id: TitleId, _ isFavorite: Bool) async {
        await Self.write(tmdbId: id.tmdbId, type: id.type, isFavorite: isFavorite)
    }

    private nonisolated static func write(tmdbId: Int64, type: TitleType, isFavorite: Bool) async {
        let id = TitleId(tmdbId: tmdbId, type: type)
        try? await SharedCore.shared.favoritesRepository.setFavorite(id: id, isFavorite: isFavorite)
    }
}
