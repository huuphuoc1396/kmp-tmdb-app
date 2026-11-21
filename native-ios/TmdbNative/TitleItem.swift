import Foundation
import TmdbCore

/// A title as Home and the details' recommendations show it; `id` is unique across films and series (`movie-550`,
/// `tv-1396`).
struct TitleItem: Hashable, Identifiable, Codable {
    let id: String
    let name: String
    let posterURL: URL?
}

extension TitleItem {
    init(_ title: Title) {
        self.init(
            id: Self.id(for: title.id),
            name: title.name,
            posterURL: title.posterUrl.flatMap(URL.init(string:))
        )
    }

    /// The shared core's id of this title; `nil` when `id` is not `<movie|tv>-<tmdbId>`.
    var titleId: TitleId? {
        let parts = id.split(separator: "-", maxSplits: 1)
        guard parts.count == 2, let tmdbId = Int64(parts[1]) else { return nil }
        guard let type = TitleType.allCases.first(where: { $0.name.lowercased() == parts[0] }) else { return nil }
        return TitleId(tmdbId: tmdbId, type: type)
    }

    static func id(for titleId: TitleId) -> String {
        "\(titleId.type.name.lowercased())-\(titleId.tmdbId)"
    }
}
