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

    @MainActor
    func testTitleDetailsAdapterEmitsWithoutWaitingForNetwork() async {
        var iterator = SharedCoreTitleDetails().observe(detailsTitleId, language: .enUs).makeAsyncIterator()

        // The first value is the cache, `nil` when never loaded; the stream does not end.
        let first: TitleDetailsItem?? = await iterator.next()

        guard case .some = first else { return XCTFail("The stream ended without a value") }
    }

    @MainActor
    func testTitleDetailsAdapterRefreshCompletes() async {
        // `true` or `false` depending on network and token; it must return instead of throwing.
        _ = await SharedCoreTitleDetails().refresh(detailsTitleId, language: .enUs, force: false)
    }

    @MainActor
    func testFavoritesAdapterStoresAndClearsFavorite() async {
        let favorites = SharedCoreFavorites()
        let id = TitleId(tmdbId: 9_999_001, type: .tv)

        await favorites.setFavorite(id, true)
        let stored = await firstValue(of: favorites.observeIsFavorite(id))
        await favorites.setFavorite(id, false)
        let cleared = await firstValue(of: favorites.observeIsFavorite(id))

        XCTAssertEqual(stored, true)
        XCTAssertEqual(cleared, false)
    }

    @MainActor
    func testTitleDatabaseIsEncrypted() async throws {
        var iterator = SharedCoreTitleLists().observe(listKey(.movies, .featured, .enUs)).makeAsyncIterator()

        // Reading the cache opens the title database with its Keychain-protected key.
        _ = await iterator.next()

        let file = try XCTUnwrap(titleDatabaseFile(), "title.db was not created")
        let header = try FileHandle(forReadingFrom: file).read(upToCount: 16)
        XCTAssertNotEqual(header, Data("SQLite format 3\0".utf8))
    }

    @MainActor
    func testDatabaseFilesAreExcludedFromBackup() async throws {
        var iterator = SharedCoreTitleLists().observe(listKey(.movies, .featured, .enUs)).makeAsyncIterator()

        // Reading the cache creates the title database and its key file.
        _ = await iterator.next()

        let databases = try XCTUnwrap(titleDatabaseFile(), "title.db was not created").deletingLastPathComponent()
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        let keys = support.appendingPathComponent("database-keys", isDirectory: true)
        for directory in [databases, keys] {
            let values = try directory.resourceValues(forKeys: [.isExcludedFromBackupKey])
            XCTAssertEqual(values.isExcludedFromBackup, true, "\(directory.lastPathComponent) is backed up")
        }
    }

    private func titleDatabaseFile() -> URL? {
        let library = FileManager.default.urls(for: .libraryDirectory, in: .userDomainMask)[0]
        let files = FileManager.default.enumerator(at: library, includingPropertiesForKeys: nil)
        return files?.compactMap { $0 as? URL }.first { $0.lastPathComponent == "title.db" }
    }

    @MainActor
    private func firstValue(of stream: AsyncStream<Bool>) async -> Bool? {
        for await value in stream {
            return value
        }
        return nil
    }

    private func firstValue(of flow: SkieSwiftFlow<KotlinBoolean>) async -> Bool? {
        for await value in flow {
            return value.boolValue
        }
        return nil
    }
}
