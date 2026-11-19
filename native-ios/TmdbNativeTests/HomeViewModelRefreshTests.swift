@testable import TmdbNative
import TmdbCore
import XCTest

@MainActor
final class HomeViewModelRefreshTests: XCTestCase {
    private var titleLists: TitleListsStub!
    private var viewModel: HomeViewModel!

    override func setUp() async throws {
        titleLists = TitleListsStub()
        viewModel = HomeViewModel(titleLists: titleLists)
    }

    func testPullToRefreshForcesBothListsAndEndsIndicator() async {
        viewModel.onIntent(.languageChanged(.enUs))
        let automatic = await eventually { titleLists.refreshRequests.count == 2 }
        XCTAssertTrue(automatic)

        await viewModel.refresh()

        XCTAssertEqual(
            Set(titleLists.refreshRequests.suffix(2)),
            [.init(key: listKey(.featured), force: true), .init(key: listKey(.popular), force: true)]
        )
        XCTAssertFalse(viewModel.state.isRefreshing)
    }

    func testPullWhileRefreshingIsIgnored() async {
        viewModel.onIntent(.languageChanged(.enUs))
        let automatic = await eventually { titleLists.refreshRequests.count == 2 }
        XCTAssertTrue(automatic)
        titleLists.holdRefreshes = true

        let first = Task { await viewModel.refresh() }
        let running = await eventually { viewModel.state.isRefreshing && titleLists.refreshRequests.count == 4 }
        XCTAssertTrue(running)
        let second = Task { await viewModel.refresh() }
        await settle()
        XCTAssertEqual(titleLists.refreshRequests.count, 4)

        titleLists.releaseRefreshes()
        await first.value
        await second.value

        XCTAssertEqual(titleLists.refreshRequests.count, 4)
        XCTAssertFalse(viewModel.state.isRefreshing)
    }

    func testPullBeforeLanguageDoesNothing() async {
        await viewModel.refresh()

        XCTAssertEqual(titleLists.refreshRequests, [])
        XCTAssertFalse(viewModel.state.isRefreshing)
    }
}
