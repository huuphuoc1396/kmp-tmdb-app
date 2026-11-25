@testable import TmdbNative
import XCTest

final class NameLinesTests: XCTestCase {
    private func lines(_ name: String, width: CGFloat, lineLimit: Int? = nil) -> [String] {
        NameLines.lines(of: name, width: width, lineLimit: lineLimit) { CGFloat($0.count) }
    }

    func testFitsOnOneLine() {
        XCTAssertEqual(lines("Viola Davis", width: 20), ["Viola Davis"])
    }

    func testBreaksOnlyBetweenWords() {
        XCTAssertEqual(lines("Robert Downey Jr.", width: 10), ["Robert", "Downey Jr."])
    }

    func testCutsWordWiderThanLineWithEllipsis() {
        XCTAssertEqual(lines("Doomsday", width: 6), ["Dooms…"])
    }

    func testStopsAtCutWord() {
        XCTAssertEqual(lines("Doomsday is near", width: 7), ["Doomsd…"])
    }

    func testKeepsLinesBeforeCutWord() {
        XCTAssertEqual(lines("Chris Hemsworth", width: 8), ["Chris", "Hemswor…"])
    }

    func testCutsLastLineWhenLimitReached() {
        XCTAssertEqual(lines("A title with a long name", width: 8, lineLimit: 2), ["A title", "with a…"])
    }

    func testCutLastLineKeepsSingleEllipsis() {
        XCTAssertEqual(lines("Short Doomsday tail", width: 6, lineLimit: 2), ["Short", "Dooms…"])
    }

    func testKeepsAllLinesWithoutLimit() {
        XCTAssertEqual(lines("One two three four", width: 5), ["One", "two", "three", "four"])
    }

    func testEmptyNameGivesNoLines() {
        XCTAssertEqual(lines("  ", width: 10), [])
    }
}
