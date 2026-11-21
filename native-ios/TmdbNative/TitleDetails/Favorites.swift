import TmdbCore

@MainActor
protocol Favorites {
    /// Whether the title is a favorite; emits again on every change.
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
        // A failed write leaves the stored value, which the observed stream keeps showing.
        await Self.write(tmdbId: id.tmdbId, type: id.type, isFavorite: isFavorite)
    }

    /// Kotlin objects aren't `Sendable`, so the id is built again off the main actor from its parts.
    private nonisolated static func write(tmdbId: Int64, type: TitleType, isFavorite: Bool) async {
        let id = TitleId(tmdbId: tmdbId, type: type)
        try? await SharedCore.shared.favoritesRepository.setFavorite(id: id, isFavorite: isFavorite)
    }
}
