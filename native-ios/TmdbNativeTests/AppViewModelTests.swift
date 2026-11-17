@testable import TmdbNative
import XCTest

@MainActor
final class AppViewModelTests: XCTestCase {
    func testStartsLoading() {
        let viewModel = AppViewModel(settings: OnboardingSettingsStub())

        XCTAssertEqual(viewModel.startDestination, .loading)
    }

    func testNotCompletedStartsOnOnboarding() async {
        let viewModel = AppViewModel(settings: OnboardingSettingsStub())

        await viewModel.load()

        XCTAssertEqual(viewModel.startDestination, .onboarding)
    }

    func testCompletedStartsOnHome() async {
        let settings = OnboardingSettingsStub()
        settings.completed = true
        let viewModel = AppViewModel(settings: settings)

        await viewModel.load()

        XCTAssertEqual(viewModel.startDestination, .home)
    }

    func testOnboardingFinishedShowsHome() async {
        let viewModel = AppViewModel(settings: OnboardingSettingsStub())
        await viewModel.load()

        viewModel.onboardingFinished()

        XCTAssertEqual(viewModel.startDestination, .home)
    }
}
