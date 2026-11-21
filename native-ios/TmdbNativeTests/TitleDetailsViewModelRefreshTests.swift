@testable import TmdbNative
import TmdbCore
import XCTest

@MainActor
final class TitleDetailsViewModelRefreshTests: XCTestCase {
    private var source: TitleDetailsStub!
    private var viewModel: TitleDetailsViewModel!

    override func setUp() async throws {
        source = TitleDetailsStub()
        viewModel = TitleDetailsViewModel(titleId: detailsTitleId, details: source, favorites: FavoritesStub())
    }

    func testPullToRefreshForcesAndEndsIndicator() async {
        await showDetails()

        await viewModel.refresh()

        XCTAssertEqual(source.refreshRequests.last, .init(language: .enUs, force: true))
        XCTAssertFalse(viewModel.state.isRefreshing)
    }

    func testPullWhileRefreshingIsIgnored() async {
        await showDetails()
        source.holdRefreshes = true

        let first = Task { await viewModel.refresh() }
        let running = await eventually { viewModel.state.isRefreshing && source.refreshRequests.count == 2 }
        XCTAssertTrue(running)
        let second = Task { await viewModel.refresh() }
        await settle()
        XCTAssertEqual(source.refreshRequests.count, 2)

        source.releaseRefreshes()
        await first.value
        await second.value

        XCTAssertEqual(source.refreshRequests.count, 2)
        XCTAssertFalse(viewModel.state.isRefreshing)
    }

    func testPullWithoutDetailsDoesNothing() async {
        source.refreshResults[.enUs] = false
        viewModel.onIntent(.languageChanged(.enUs))
        let failed = await eventually { viewModel.state.load == .error }
        XCTAssertTrue(failed)

        await viewModel.refresh()

        XCTAssertEqual(source.refreshRequests.count, 1)
        XCTAssertFalse(viewModel.state.isRefreshing)
    }

    func testFailedPullKeepsDetails() async {
        await showDetails()
        source.refreshResults[.enUs] = false

        await viewModel.refresh()

        XCTAssertEqual(viewModel.state.load, .loaded(details()))
        XCTAssertFalse(viewModel.state.isRefreshing)
    }

    private func showDetails() async {
        source.send(details(), for: .enUs)
        viewModel.onIntent(.languageChanged(.enUs))
        let loaded = await eventually {
            viewModel.state.load == .loaded(details()) && source.refreshRequests.count == 1
        }
        XCTAssertTrue(loaded)
    }
}
