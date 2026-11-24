@testable import TmdbNative
import XCTest

final class MetaLineTests: XCTestCase {
    func testYearAndGenreAreJoinedWithDot() {
        XCTAssertEqual(metaLine(year: 2021, genre: "Drama"), "2021 · Drama")
    }

    func testMissingYearLeavesGenreOnly() {
        XCTAssertEqual(metaLine(year: nil, genre: "Drama"), "Drama")
    }

    func testMissingGenreLeavesYearOnly() {
        XCTAssertEqual(metaLine(year: 2021, genre: nil), "2021")
    }

    func testBothMissingIsEmpty() {
        XCTAssertEqual(metaLine(year: nil, genre: nil), "")
    }
}
