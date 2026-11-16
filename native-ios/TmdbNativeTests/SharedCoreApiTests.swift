import TmdbCore
import XCTest

final class SharedCoreApiTests: XCTestCase {
    override func setUp() {
        super.setUp()
        SharedCore.shared.start()
    }

    func testSharedCoreProvidesSettingsRepository() {
        XCTAssertNotNil(SharedCore.shared.settingsRepository)
    }

    func testCompletedOnboardingIsReadThroughPublishedApi() async throws {
        let repository = SharedCore.shared.settingsRepository

        let result = try await repository.completeOnboarding()

        XCTAssertTrue(result is AppResultSuccess<AnyObject>)
        let completed = await firstValue(of: repository.observeOnboardingCompleted())
        XCTAssertEqual(completed, true)
    }

    private func firstValue(of flow: SkieSwiftFlow<KotlinBoolean>) async -> Bool? {
        for await value in flow {
            return value.boolValue
        }
        return nil
    }
}
