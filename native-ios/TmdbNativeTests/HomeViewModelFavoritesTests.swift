@testable import TmdbNative
import TmdbCore
import XCTest

@MainActor
final class HomeViewModelFavoritesTests: XCTestCase {
    private var favoriteTitles: FavoriteTitlesStub!
    private var favorites: FavoritesStub!
    private var viewModel: HomeViewModel!

    override func setUp() async throws {
        favoriteTitles = FavoriteTitlesStub()
        favorites = FavoritesStub()
        viewModel = HomeViewModel(titleLists: TitleListsStub(), favoriteTitles: favoriteTitles, favorites: favorites)
    }

    func testNoFavoritesGivesEmptySection() async {
        viewModel.onIntent(.languageChanged(.enUs))
        await settle()

        XCTAssertEqual(viewModel.state.favorites, [])
        XCTAssertEqual(viewModel.state.favoritesCount, 0)
    }

    func testShowsTenNewestFavoritesWithTotalCount() async {
        let list = (1...12).reversed().map { favorite(Int64($0)) }
        favoriteTitles.send(list, for: .enUs)

        viewModel.onIntent(.languageChanged(.enUs))

        let shown = await eventually { self.viewModel.state.favoritesCount == 12 }
        XCTAssertTrue(shown)
        XCTAssertEqual(viewModel.state.favorites, Array(list.prefix(10)))
    }

    func testSectionIsTheSameForEveryCategory() async {
        favoriteTitles.send([favorite(1)], for: .enUs)
        viewModel.onIntent(.languageChanged(.enUs))
        _ = await eventually { self.viewModel.state.favoritesCount == 1 }

        viewModel.onIntent(.categorySelected(.anime))
        await settle()

        XCTAssertEqual(viewModel.state.favorites, [favorite(1)])
        XCTAssertEqual(favoriteTitles.observedLanguages, [.enUs])
    }

    func testFavoritesFollowLanguageChange() async {
        favoriteTitles.send([favorite(1, "Clube da Luta")], for: .ptBr)
        favoriteTitles.send([favorite(1, "Fight Club")], for: .enUs)
        viewModel.onIntent(.languageChanged(.ptBr))
        let portuguese = await eventually { self.viewModel.state.favorites.first?.name == "Clube da Luta" }

        viewModel.onIntent(.languageChanged(.enUs))
        let english = await eventually { self.viewModel.state.favorites.first?.name == "Fight Club" }

        XCTAssertTrue(portuguese)
        XCTAssertTrue(english)
    }

    func testFavoriteTapOpensTitle() async {
        let tapped = favorite(1)
        let effects = viewModel.effects

        viewModel.onIntent(.favoriteTapped(tapped))

        var iterator = effects.makeAsyncIterator()
        let effect = await iterator.next()
        XCTAssertEqual(effect, .openTitle(tapped.titleItem))
    }

    func testSeeAllOpensFavorites() async {
        let effects = viewModel.effects

        viewModel.onIntent(.seeAllFavoritesTapped)

        var iterator = effects.makeAsyncIterator()
        let effect = await iterator.next()
        XCTAssertEqual(effect, .openFavorites)
    }

    func testRemoveTapAsksToConfirm() {
        viewModel.onIntent(.removeFavoriteTapped(favorite(1)))
        viewModel.onIntent(.removeFavoriteTapped(favorite(2)))

        XCTAssertEqual(viewModel.state.pendingRemoval, favorite(1))
        XCTAssertEqual(favorites.writes, [])
    }

    func testConfirmRemovesFavoriteAndCloses() async {
        viewModel.onIntent(.removeFavoriteTapped(favorite(1)))

        viewModel.onIntent(.removeFavoriteConfirmed)

        XCTAssertNil(viewModel.state.pendingRemoval)
        let written = await eventually { self.favorites.writes == [false] }
        XCTAssertTrue(written)
        XCTAssertEqual(favorites.writtenIds, [TitleId(tmdbId: 1, type: .movie)])
    }

    func testConfirmTwiceWritesOnce() async {
        viewModel.onIntent(.removeFavoriteTapped(favorite(1)))

        viewModel.onIntent(.removeFavoriteConfirmed)
        viewModel.onIntent(.removeFavoriteConfirmed)
        await settle()

        XCTAssertEqual(favorites.writes, [false])
    }

    func testDismissKeepsFavorite() async {
        viewModel.onIntent(.removeFavoriteTapped(favorite(1)))

        viewModel.onIntent(.removeFavoriteDismissed)
        await settle()

        XCTAssertNil(viewModel.state.pendingRemoval)
        XCTAssertEqual(favorites.writes, [])
    }
}
