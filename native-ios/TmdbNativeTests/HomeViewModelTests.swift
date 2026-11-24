@testable import TmdbNative
import TmdbCore
import XCTest

@MainActor
final class HomeViewModelTests: XCTestCase {
    private var titleLists: TitleListsStub!
    private var viewModel: HomeViewModel!

    override func setUp() async throws {
        titleLists = TitleListsStub()
        viewModel = HomeViewModel(titleLists: titleLists, favoriteTitles: FavoriteTitlesStub(), favorites: FavoritesStub())
    }

    func testStartsLoadingWithMoviesSelected() {
        XCTAssertEqual(viewModel.state, HomeUiState(selectedCategory: .movies, featured: .loading, popular: .loading))
    }

    func testNothingIsObservedBeforeLanguage() async {
        viewModel.onIntent(.categorySelected(.series))
        await settle()

        XCTAssertEqual(titleLists.observedKeys, [])
        XCTAssertEqual(titleLists.refreshRequests, [])
        XCTAssertEqual(viewModel.state.selectedCategory, .series)
    }

    func testListsAreLoadedInTheGivenLanguage() async {
        viewModel.onIntent(.languageChanged(.ptBr))

        let refreshed = await eventually { titleLists.refreshRequests.count == 2 }
        XCTAssertTrue(refreshed)
        XCTAssertEqual(
            Set(titleLists.observedKeys),
            [listKey(.movies, .featured, .ptBr), listKey(.movies, .popular, .ptBr)]
        )
        XCTAssertTrue(titleLists.refreshRequests.allSatisfy { $0.key.language == .ptBr })
    }

    func testCachedTitlesAreShownPerRow() async {
        titleLists.send(titles("featured"), for: listKey(.featured))
        titleLists.send(titles("popular"), for: listKey(.popular))

        viewModel.onIntent(.languageChanged(.enUs))

        let shown = await eventually {
            viewModel.state.featured == .titles(titles("featured"))
                && viewModel.state.popular == .titles(titles("popular"))
        }
        XCTAssertTrue(shown)
    }

    func testSelectingCategoryShowsItsListsAndRefreshesWithoutForce() async {
        titleLists.send(titles("anime"), for: listKey(.anime, .featured))
        titleLists.send(titles("anime popular"), for: listKey(.anime, .popular))
        viewModel.onIntent(.languageChanged(.enUs))

        viewModel.onIntent(.categorySelected(.anime))

        let shown = await eventually {
            viewModel.state.selectedCategory == .anime
                && viewModel.state.featured == .titles(titles("anime"))
                && viewModel.state.popular == .titles(titles("anime popular"))
        }
        XCTAssertTrue(shown)
        let refreshed = await eventually { titleLists.refreshRequests.count == 4 }
        XCTAssertTrue(refreshed)
        XCTAssertEqual(
            Set(titleLists.refreshRequests.suffix(2)),
            [
                TitleListsStub.RefreshRequest(key: listKey(.anime, .featured), force: false),
                TitleListsStub.RefreshRequest(key: listKey(.anime, .popular), force: false),
            ]
        )
    }

    func testFailedLoadWithoutCacheShowsError() async {
        titleLists.refreshResults[listKey(.featured)] = false
        titleLists.refreshResults[listKey(.popular)] = false

        viewModel.onIntent(.languageChanged(.enUs))

        let failed = await eventually { viewModel.state.featured == .error && viewModel.state.popular == .error }
        XCTAssertTrue(failed)
    }

    func testFailedRefreshWithCacheKeepsTitles() async {
        titleLists.send(titles("cached"), for: listKey(.featured))
        titleLists.refreshResults[listKey(.featured)] = false

        viewModel.onIntent(.languageChanged(.enUs))
        let refreshed = await eventually { titleLists.refreshRequests.count == 2 }
        await settle()

        XCTAssertTrue(refreshed)
        XCTAssertEqual(viewModel.state.featured, .titles(titles("cached")))
    }

    func testRetryForcesOnlyThatList() async {
        titleLists.refreshResults[listKey(.popular)] = false
        viewModel.onIntent(.languageChanged(.enUs))
        let failed = await eventually { viewModel.state.popular == .error }
        XCTAssertTrue(failed)
        titleLists.refreshResults[listKey(.popular)] = true

        viewModel.onIntent(.retry(.popular))
        titleLists.send(titles("popular"), for: listKey(.popular))

        let shown = await eventually { viewModel.state.popular == .titles(titles("popular")) }
        XCTAssertTrue(shown)
        XCTAssertEqual(titleLists.refreshRequests.last, .init(key: listKey(.popular), force: true))
        XCTAssertEqual(titleLists.refreshRequests.filter(\.force).count, 1)
    }

    func testEmptyListShowsEmptyRow() async {
        titleLists.send([], for: listKey(.novelas, .featured))
        titleLists.send(titles("novelas"), for: listKey(.novelas, .popular))
        viewModel.onIntent(.categorySelected(.novelas))

        viewModel.onIntent(.languageChanged(.enUs))

        let shown = await eventually {
            viewModel.state.featured == .titles([]) && viewModel.state.popular == .titles(titles("novelas"))
        }
        XCTAssertTrue(shown)
    }

    func testOneRowFailsWhileOtherShowsTitles() async {
        titleLists.send(titles("featured"), for: listKey(.featured))
        titleLists.refreshResults[listKey(.popular)] = false

        viewModel.onIntent(.languageChanged(.enUs))

        let shown = await eventually {
            viewModel.state.featured == .titles(titles("featured")) && viewModel.state.popular == .error
        }
        XCTAssertTrue(shown)
    }

    func testFailureForPreviousTabDoesNotMarkNewTab() async {
        titleLists.holdRefreshes = true
        titleLists.refreshResults[listKey(.movies, .featured)] = false
        titleLists.refreshResults[listKey(.movies, .popular)] = false
        viewModel.onIntent(.languageChanged(.enUs))
        let started = await eventually { titleLists.refreshRequests.count == 2 }
        XCTAssertTrue(started)

        viewModel.onIntent(.categorySelected(.series))
        titleLists.releaseRefreshes()
        await settle()

        XCTAssertEqual(viewModel.state.selectedCategory, .series)
        XCTAssertEqual(viewModel.state.featured, .loading)
        XCTAssertEqual(viewModel.state.popular, .loading)
    }

    func testLanguageChangeShowsNewLanguageOnly() async {
        titleLists.send(titles("english"), for: listKey(.featured, .enUs))
        viewModel.onIntent(.languageChanged(.enUs))
        let english = await eventually { viewModel.state.featured == .titles(titles("english")) }
        XCTAssertTrue(english)

        viewModel.onIntent(.languageChanged(.ptBr))
        await settle()

        XCTAssertEqual(viewModel.state.featured, .loading)
        titleLists.send(titles("portuguese"), for: listKey(.featured, .ptBr))
        let portuguese = await eventually { viewModel.state.featured == .titles(titles("portuguese")) }
        XCTAssertTrue(portuguese)
        XCTAssertTrue(titleLists.refreshRequests.contains(.init(key: listKey(.featured, .ptBr), force: false)))
    }

    func testCategoryRestoredBeforeLanguageLoadsOnlyThatCategory() async {
        viewModel.onIntent(.categorySelected(.documentaries))
        viewModel.onIntent(.languageChanged(.enUs))

        let refreshed = await eventually { titleLists.refreshRequests.count == 2 }
        await settle()

        XCTAssertTrue(refreshed)
        XCTAssertEqual(titleLists.refreshRequests.count, 2)
        XCTAssertTrue(titleLists.refreshRequests.allSatisfy { $0.key.category == .documentaries })
        XCTAssertTrue(titleLists.observedKeys.allSatisfy { $0.category == .documentaries })
    }

    func testTitleTappedOpensThatTitle() async {
        let title = titles("tapped", count: 1)[0]
        let effects = viewModel.effects

        viewModel.onIntent(.titleTapped(title))

        var iterator = effects.makeAsyncIterator()
        let effect = await iterator.next()
        XCTAssertEqual(effect, .openTitle(title))
    }

    func testEffectsReachTheLatestCollector() async {
        let title = titles("tapped", count: 1)[0]
        _ = viewModel.effects
        let effects = viewModel.effects

        viewModel.onIntent(.titleTapped(title))

        var iterator = effects.makeAsyncIterator()
        let effect = await iterator.next()
        XCTAssertEqual(effect, .openTitle(title))
    }
}
