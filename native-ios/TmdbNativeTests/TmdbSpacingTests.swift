@testable import TmdbNative
import XCTest

final class TmdbSpacingTests: XCTestCase {
    private let steps: [CGFloat] = [
        TmdbSpacing.xxs, TmdbSpacing.xs, TmdbSpacing.sm, TmdbSpacing.md,
        TmdbSpacing.lg, TmdbSpacing.xl, TmdbSpacing.xxl, TmdbSpacing.xxxl,
    ]

    func testEveryStepIsAMultipleOfFour() {
        for step in steps {
            XCTAssertEqual(step.truncatingRemainder(dividingBy: 4), 0, "\(step) is not on the 4pt grid")
        }
    }

    func testStepsGrow() {
        for (smaller, larger) in zip(steps, steps.dropFirst()) {
            XCTAssertLessThan(smaller, larger)
        }
    }
}
