@testable import TmdbNative
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

    @MainActor
    func testAdapterReadsCompletionThroughSharedCore() async throws {
        let settings = SharedCoreOnboardingSettings()

        try await settings.completeOnboarding()

        let completed = await settings.isOnboardingCompleted()
        XCTAssertTrue(completed)
    }

    func testSharedCoreProvidesTitleListsRepository() {
        XCTAssertNotNil(SharedCore.shared.titleListsRepository)
    }

    @MainActor
    func testTitleListsAdapterEmitsWithoutWaitingForNetwork() async {
        let titleLists = SharedCoreTitleLists()
        var iterator = titleLists.observe(listKey(.movies, .featured, .enUs)).makeAsyncIterator()

        // The first value is the cache, `nil` when never loaded; the stream does not end.
        let first: [TitleItem]?? = await iterator.next()

        guard case .some = first else { return XCTFail("The stream ended without a value") }
    }

    @MainActor
    func testTitleListsAdapterRefreshCompletes() async {
        let titleLists = SharedCoreTitleLists()

        // `true` or `false` depending on network and token; it must return instead of throwing.
        _ = await titleLists.refresh(listKey(.movies, .featured, .enUs), force: false)
    }

    private func firstValue(of flow: SkieSwiftFlow<KotlinBoolean>) async -> Bool? {
        for await value in flow {
            return value.boolValue
        }
        return nil
    }
}
