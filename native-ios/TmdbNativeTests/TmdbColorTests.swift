@testable import TmdbNative
import SwiftUI
import UIKit
import XCTest

final class TmdbColorTests: XCTestCase {
    func testPrimaryIsTheBrandPurple() {
        assertHex(TmdbColor.primary, 0x8000FF)
    }

    func testSecondaryIsTheBrandPink() {
        assertHex(TmdbColor.secondary, 0xFF1F8A)
    }

    func testSurfacesUseTheFavoritesPurpleFamily() {
        assertHex(TmdbColor.surface, 0x130E1F)
        assertHex(TmdbColor.background, 0x130E1F)
        assertHex(TmdbColor.surfaceContainer, 0x211A30)
        assertHex(TmdbColor.surfaceContainerHigh, 0x312A3F)
        assertHex(TmdbColor.onSurfaceVariant, 0xA6A1B5)
        assertHex(TmdbColor.outline, 0x655F74)
    }

    func testLabelsOnTheBrandPinkReachThreeToOne() {
        XCTAssertGreaterThanOrEqual(contrast(TmdbColor.onSecondary, TmdbColor.secondary), 3)
    }

    func testSecondaryTextOnSurfaceContainerReachesAa() {
        XCTAssertGreaterThanOrEqual(contrast(TmdbColor.secondary, TmdbColor.surfaceContainer), 4.5)
    }

    func testTextRolesReachAaOnTheirBackgrounds() {
        let pairs: [(Color, Color)] = [
            (TmdbColor.onPrimary, TmdbColor.primary),
            (TmdbColor.onPrimaryContainer, TmdbColor.primaryContainer),
            (TmdbColor.onSecondaryContainer, TmdbColor.secondaryContainer),
            (TmdbColor.onSurface, TmdbColor.surface),
            (TmdbColor.onSurface, TmdbColor.surfaceContainer),
            (TmdbColor.onSurface, TmdbColor.surfaceVariant),
            (TmdbColor.onSurfaceVariant, TmdbColor.surface),
            (TmdbColor.onSurfaceVariant, TmdbColor.surfaceContainer),
        ]
        for (text, background) in pairs {
            XCTAssertGreaterThanOrEqual(contrast(text, background), 4.5)
        }
    }

    private func components(_ color: Color) -> (red: CGFloat, green: CGFloat, blue: CGFloat) {
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 0
        UIColor(color).getRed(&red, green: &green, blue: &blue, alpha: &alpha)
        return (red, green, blue)
    }

    private func assertHex(_ color: Color, _ hex: UInt32, file: StaticString = #filePath, line: UInt = #line) {
        let rgb = components(color)
        let tolerance: CGFloat = 1 / 255
        XCTAssertEqual(rgb.red, CGFloat((hex >> 16) & 0xFF) / 255, accuracy: tolerance, file: file, line: line)
        XCTAssertEqual(rgb.green, CGFloat((hex >> 8) & 0xFF) / 255, accuracy: tolerance, file: file, line: line)
        XCTAssertEqual(rgb.blue, CGFloat(hex & 0xFF) / 255, accuracy: tolerance, file: file, line: line)
    }

    private func luminance(_ color: Color) -> CGFloat {
        let rgb = components(color)
        func linear(_ channel: CGFloat) -> CGFloat {
            channel <= 0.03928 ? channel / 12.92 : pow((channel + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * linear(rgb.red) + 0.7152 * linear(rgb.green) + 0.0722 * linear(rgb.blue)
    }

    private func contrast(_ first: Color, _ second: Color) -> CGFloat {
        let lighter = max(luminance(first), luminance(second))
        let darker = min(luminance(first), luminance(second))
        return (lighter + 0.05) / (darker + 0.05)
    }
}
