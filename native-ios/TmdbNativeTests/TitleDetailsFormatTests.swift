@testable import TmdbNative
import TmdbCore
import XCTest

final class TitleDetailsFormatTests: XCTestCase {
    private let date = ReleaseDate(year: 2021, month: 12, day: 5)

    func testReleaseDateInPortugueseIsDayFirst() {
        XCTAssertEqual(TitleDetailsFormat.releaseDate(date, language: .ptBr), "05/12/2021")
    }

    func testReleaseDateInEnglishIsMonthFirst() {
        XCTAssertEqual(TitleDetailsFormat.releaseDate(date, language: .enUs), "12/05/2021")
    }

    func testReleaseDateInVietnamese() {
        XCTAssertEqual(TitleDetailsFormat.releaseDate(date, language: .viVn), "5 thg 12, 2021")
    }

    func testRuntimeInVietnamese() {
        XCTAssertEqual(TitleDetailsFormat.runtime(minutes: 130, language: .viVn), "2 giờ 10 phút")
        XCTAssertEqual(TitleDetailsFormat.runtime(minutes: 120, language: .viVn), "2 giờ")
        XCTAssertEqual(TitleDetailsFormat.runtime(minutes: 45, language: .viVn), "45 phút")
    }

    func testRuntimeInPortugueseIsUnchanged() {
        XCTAssertEqual(TitleDetailsFormat.runtime(minutes: 113, language: .ptBr), "1h 53m")
    }

    func testRuntimeWithHoursAndMinutes() {
        XCTAssertEqual(TitleDetailsFormat.runtime(minutes: 113, language: .enUs), "1h 53m")
    }

    func testRuntimeOfWholeHours() {
        XCTAssertEqual(TitleDetailsFormat.runtime(minutes: 120, language: .enUs), "2h")
    }

    func testRuntimeUnderAnHour() {
        XCTAssertEqual(TitleDetailsFormat.runtime(minutes: 45, language: .enUs), "45m")
    }

    func testNameWithYearKeepsYearAttached() {
        XCTAssertEqual(TitleDetailsFormat.nameWithYear("Imperdoável", year: 2021), "Imperdoável\u{00A0}(2021)")
    }

    func testNameWithoutYear() {
        XCTAssertEqual(TitleDetailsFormat.nameWithYear("Imperdoável", year: nil), "Imperdoável")
    }
}
