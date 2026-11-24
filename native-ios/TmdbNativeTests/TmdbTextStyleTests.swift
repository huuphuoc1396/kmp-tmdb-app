@testable import TmdbNative
import SwiftUI
import UIKit
import XCTest

final class TmdbTextStyleTests: XCTestCase {
    func testSizesAndLineHeightsMatchMaterial() {
        let expected: [TmdbTextStyle: (size: CGFloat, lineHeight: CGFloat)] = [
            .displayLarge: (57, 64), .displayMedium: (45, 52), .displaySmall: (36, 44),
            .headlineLarge: (32, 40), .headlineMedium: (28, 36), .headlineSmall: (24, 32),
            .titleLarge: (22, 28), .titleMedium: (16, 24), .titleSmall: (14, 20),
            .bodyLarge: (16, 24), .bodyMedium: (14, 20), .bodySmall: (12, 16),
            .labelLarge: (14, 20), .labelMedium: (12, 16), .labelSmall: (11, 16),
        ]
        XCTAssertEqual(expected.count, TmdbTextStyle.allCases.count)
        for style in TmdbTextStyle.allCases {
            XCTAssertEqual(style.size, expected[style]?.size, "\(style)")
            XCTAssertEqual(style.lineHeight, expected[style]?.lineHeight, "\(style)")
        }
    }

    func testLineSpacingFillsTheMaterialLineHeight() {
        for style in TmdbTextStyle.allCases {
            let natural = UIFont.systemFont(ofSize: style.size).lineHeight
            XCTAssertEqual(natural + style.lineSpacing, max(natural, style.lineHeight), accuracy: 0.01, "\(style)")
        }
    }

    func testDisplayHeadlineAndTitleLargeAreBold() {
        let styles: [TmdbTextStyle] = [
            .displayLarge, .displayMedium, .displaySmall,
            .headlineLarge, .headlineMedium, .headlineSmall, .titleLarge,
        ]
        for style in styles {
            XCTAssertEqual(style.weight, .bold, "\(style)")
        }
    }

    func testTitleMediumTitleSmallAndLabelsAreSemibold() {
        let styles: [TmdbTextStyle] = [.titleMedium, .titleSmall, .labelLarge, .labelMedium, .labelSmall]
        for style in styles {
            XCTAssertEqual(style.weight, .semibold, "\(style)")
        }
    }

    func testBodyRolesAreRegular() {
        for style in [TmdbTextStyle.bodyLarge, .bodyMedium, .bodySmall] {
            XCTAssertEqual(style.weight, .regular, "\(style)")
        }
    }
}
