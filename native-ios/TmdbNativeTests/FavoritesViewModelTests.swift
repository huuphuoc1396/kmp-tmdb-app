@testable import TmdbNative
import TmdbCore
import XCTest

@MainActor
final class FavoritesViewModelTests: XCTestCase {
    private var favoriteTitles: FavoriteTitlesStub!
    private var favorites: FavoritesStub!
    private var viewModel: FavoritesViewModel!

    override func setUp() async throws {
        favoriteTitles = FavoriteTitlesStub()
        favorites = FavoritesStub()
        viewModel = FavoritesViewModel(favoriteTitles: favoriteTitles, favorites: favorites)
    }

    func testListsFavoritesInOrder() async {
        let list = [favorite(3), favorite(2), favorite(1)]
        favoriteTitles.send(list, for: .enUs)

        viewModel.onIntent(.languageChanged(.enUs))

        let listed = await eventually { self.viewModel.state.favorites == list }
        XCTAssertTrue(listed)
    }

    func testEmptyWithoutFavorites() async {
        viewModel.onIntent(.languageChanged(.enUs))
        await settle()

        XCTAssertEqual(viewModel.state, FavoritesUiState())
    }

    func testFollowsLanguageChange() async {
        favoriteTitles.send([favorite(1, "Clube da Luta")], for: .ptBr)
        favoriteTitles.send([favorite(1, "Fight Club")], for: .enUs)
        viewModel.onIntent(.languageChanged(.ptBr))
        let portuguese = await eventually { self.viewModel.state.favorites.first?.name == "Clube da Luta" }

        viewModel.onIntent(.languageChanged(.enUs))
        let english = await eventually { self.viewModel.state.favorites.first?.name == "Fight Club" }

        XCTAssertTrue(portuguese)
        XCTAssertTrue(english)
        XCTAssertEqual(favoriteTitles.observedLanguages, [.ptBr, .enUs])
    }

    func testTitleTapOpensTitle() async {
        let tapped = favorite(1)
        let effects = viewModel.effects

        viewModel.onIntent(.titleTapped(tapped))

        var iterator = effects.makeAsyncIterator()
        let effect = await iterator.next()
        XCTAssertEqual(effect, .openTitle(tapped.titleItem))
    }

    func testRemoveTapAsksToConfirm() {
        viewModel.onIntent(.removeTapped(favorite(1)))

        XCTAssertEqual(viewModel.state.pendingRemoval, favorite(1))
        XCTAssertEqual(favorites.writes, [])
    }

    func testSecondRemoveTapKeepsFirstRequest() {
        viewModel.onIntent(.removeTapped(favorite(1)))
        viewModel.onIntent(.removeTapped(favorite(2)))

        XCTAssertEqual(viewModel.state.pendingRemoval, favorite(1))
    }

    func testConfirmRemovesFavoriteAndCloses() async {
        viewModel.onIntent(.removeTapped(favorite(1)))

        viewModel.onIntent(.removeConfirmed)

        XCTAssertNil(viewModel.state.pendingRemoval)
        let written = await eventually { self.favorites.writes == [false] }
        XCTAssertTrue(written)
        XCTAssertEqual(favorites.writtenIds, [TitleId(tmdbId: 1, type: .movie)])
    }

    func testConfirmTwiceWritesOnce() async {
        viewModel.onIntent(.removeTapped(favorite(1)))

        viewModel.onIntent(.removeConfirmed)
        viewModel.onIntent(.removeConfirmed)
        await settle()

        XCTAssertEqual(favorites.writes, [false])
    }

    func testDismissKeepsFavorite() async {
        viewModel.onIntent(.removeTapped(favorite(1)))

        viewModel.onIntent(.removeDismissed)
        await settle()

        XCTAssertNil(viewModel.state.pendingRemoval)
        XCTAssertEqual(favorites.writes, [])
    }
}
