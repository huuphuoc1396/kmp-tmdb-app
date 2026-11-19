@testable import TmdbNative
import TmdbCore
import XCTest

final class HomeLanguageTests: XCTestCase {
    func testPortugueseBrazilMapsToPtBr() {
        XCTAssertEqual(HomeLanguage.from(preferredLocalizations: ["pt-BR"]), .ptBr)
    }

    func testEnglishMapsToEnUs() {
        XCTAssertEqual(HomeLanguage.from(preferredLocalizations: ["en"]), .enUs)
    }

    func testOtherLanguageMapsToEnUs() {
        XCTAssertEqual(HomeLanguage.from(preferredLocalizations: ["es"]), .enUs)
    }

    func testNoLocalizationMapsToEnUs() {
        XCTAssertEqual(HomeLanguage.from(preferredLocalizations: []), .enUs)
    }
}
