import Foundation
@testable import TmdbNative
import TmdbCore
import XCTest

final class TitleItemTests: XCTestCase {
    func testIdRoundTripsForMovie() {
        let id = TitleId(tmdbId: 550, type: .movie)

        let item = TitleItem(id: TitleItem.id(for: id), name: "Fight Club", posterURL: nil)

        XCTAssertEqual(item.id, "movie-550")
        XCTAssertEqual(item.titleId, id)
    }

    func testIdRoundTripsForSeries() {
        let id = TitleId(tmdbId: 1396, type: .tv)

        let item = TitleItem(id: TitleItem.id(for: id), name: "Breaking Bad", posterURL: nil)

        XCTAssertEqual(item.id, "tv-1396")
        XCTAssertEqual(item.titleId, id)
    }

    func testMalformedIdHasNoTitleId() {
        for id in ["", "movie", "movie-", "movie-abc", "person-1", "550"] {
            XCTAssertNil(TitleItem(id: id, name: "", posterURL: nil).titleId, id)
        }
    }

    func testRoutesRoundTripThroughJson() throws {
        let routes = [
            DetailsRoute(
                title: TitleItem(id: "movie-550", name: "Fight Club", posterURL: URL(string: "https://a/b.jpg")),
                index: 0
            ),
            DetailsRoute(title: TitleItem(id: "tv-1396", name: "Breaking Bad", posterURL: nil), index: 1),
        ]

        let decoded = try JSONDecoder().decode([DetailsRoute].self, from: JSONEncoder().encode(routes))

        XCTAssertEqual(decoded, routes)
    }
}
