@testable import TmdbNative
import XCTest

@MainActor
final class OnboardingViewModelTests: XCTestCase {
    private struct SaveError: Error {}

    func testInitialStateIsNotNavigating() {
        let viewModel = OnboardingViewModel(settings: OnboardingSettingsStub())

        XCTAssertEqual(viewModel.state, OnboardingUiState(isNavigating: false))
    }

    func testTapCompletesOnboardingAndNavigatesToHome() async {
        let settings = OnboardingSettingsStub()
        let viewModel = OnboardingViewModel(settings: settings)

        viewModel.onIntent(.getStartedTapped)
        let effect = await firstEffect(of: viewModel)

        XCTAssertEqual(effect, .navigateToHome)
        XCTAssertEqual(settings.completeCalls, 1)
        XCTAssertTrue(settings.completed)
        XCTAssertTrue(viewModel.state.isNavigating)
    }

    func testRepeatedTapsCompleteOnce() async {
        let settings = OnboardingSettingsStub()
        let viewModel = OnboardingViewModel(settings: settings)

        viewModel.onIntent(.getStartedTapped)
        viewModel.onIntent(.getStartedTapped)
        _ = await firstEffect(of: viewModel)

        XCTAssertEqual(settings.completeCalls, 1)
    }

    func testFailedSaveStillNavigatesToHome() async {
        let settings = OnboardingSettingsStub()
        settings.completeError = SaveError()
        let viewModel = OnboardingViewModel(settings: settings)

        viewModel.onIntent(.getStartedTapped)
        let effect = await firstEffect(of: viewModel)

        XCTAssertEqual(effect, .navigateToHome)
        XCTAssertFalse(settings.completed)
    }

    private func firstEffect(of viewModel: OnboardingViewModel) async -> OnboardingEffect? {
        for await effect in viewModel.effects {
            return effect
        }
        return nil
    }
}
