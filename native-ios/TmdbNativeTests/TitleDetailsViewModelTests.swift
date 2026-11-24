@testable import TmdbNative
import TmdbCore
import XCTest

@MainActor
final class TitleDetailsViewModelTests: XCTestCase {
    private var source: TitleDetailsStub!
    private var favorites: FavoritesStub!
    private var viewModel: TitleDetailsViewModel!

    override func setUp() async throws {
        source = TitleDetailsStub()
        favorites = FavoritesStub()
        viewModel = TitleDetailsViewModel(titleId: detailsTitleId, details: source, favorites: favorites)
    }

    func testNothingHappensBeforeLanguage() async {
        await settle()

        XCTAssertEqual(source.observedLanguages, [])
        XCTAssertEqual(source.refreshRequests, [])
        XCTAssertEqual(viewModel.state.load, .loading)
    }

    func testLoadingUntilDetailsArrive() async {
        source.holdRefreshes = true
        viewModel.onIntent(.languageChanged(.enUs))
        await settle()
        XCTAssertEqual(viewModel.state.load, .loading)

        source.send(details(), for: .enUs)

        let shown = await eventually { viewModel.state.load == .loaded(details()) }
        XCTAssertTrue(shown)
        source.releaseRefreshes()
    }

    func testCachedDetailsAreShown() async {
        source.send(details(), for: .enUs)
        source.refreshResults[.enUs] = false

        viewModel.onIntent(.languageChanged(.enUs))

        let refreshed = await eventually { source.refreshRequests.count == 1 }
        XCTAssertTrue(refreshed)
        await settle()
        XCTAssertEqual(viewModel.state.load, .loaded(details()))
    }

    func testOpeningRefreshesWithoutForce() async {
        viewModel.onIntent(.languageChanged(.ptBr))

        let refreshed = await eventually { source.refreshRequests.count == 1 }
        XCTAssertTrue(refreshed)
        XCTAssertEqual(source.refreshRequests, [.init(language: .ptBr, force: false)])
        XCTAssertEqual(source.observedLanguages, [.ptBr])
    }

    func testFailedLoadWithoutCacheShowsError() async {
        source.refreshResults[.enUs] = false

        viewModel.onIntent(.languageChanged(.enUs))

        let failed = await eventually { viewModel.state.load == .error }
        XCTAssertTrue(failed)
    }

    func testFailedRefreshKeepsCachedDetails() async {
        source.refreshResults[.enUs] = false
        viewModel.onIntent(.languageChanged(.enUs))
        let failed = await eventually { viewModel.state.load == .error }
        XCTAssertTrue(failed)

        source.send(details(), for: .enUs)
        viewModel.onIntent(.retry)

        let retried = await eventually { source.refreshRequests.count == 2 }
        XCTAssertTrue(retried)
        await settle()
        XCTAssertEqual(viewModel.state.load, .loaded(details()))
    }

    func testRetryForcesRefreshAndLeavesError() async {
        source.refreshResults[.enUs] = false
        viewModel.onIntent(.languageChanged(.enUs))
        let failed = await eventually { viewModel.state.load == .error }
        XCTAssertTrue(failed)
        source.refreshResults[.enUs] = true
        source.holdRefreshes = true

        viewModel.onIntent(.retry)

        let loading = await eventually { viewModel.state.load == .loading && source.refreshRequests.count == 2 }
        XCTAssertTrue(loading)
        XCTAssertEqual(source.refreshRequests.last, .init(language: .enUs, force: true))
        source.send(details(), for: .enUs)
        source.releaseRefreshes()
        let shown = await eventually { viewModel.state.load == .loaded(details()) }
        XCTAssertTrue(shown)
    }

    func testLanguageChangeObservesNewLanguage() async {
        source.send(details("Clube da Luta"), for: .ptBr)
        viewModel.onIntent(.languageChanged(.enUs))
        let english = await eventually { source.refreshRequests.count == 1 }
        XCTAssertTrue(english)
        XCTAssertEqual(viewModel.state.load, .loading)

        viewModel.onIntent(.languageChanged(.ptBr))

        let shown = await eventually { viewModel.state.load == .loaded(details("Clube da Luta")) }
        XCTAssertTrue(shown)
        XCTAssertEqual(source.observedLanguages, [.enUs, .ptBr])
        XCTAssertEqual(source.refreshRequests.last, .init(language: .ptBr, force: false))
    }

    func testErrorIsPerLanguage() async {
        source.refreshResults[.enUs] = false
        viewModel.onIntent(.languageChanged(.enUs))
        let failed = await eventually { viewModel.state.load == .error }
        XCTAssertTrue(failed)
        source.holdRefreshes = true

        viewModel.onIntent(.languageChanged(.ptBr))

        let loading = await eventually { viewModel.state.load == .loading }
        XCTAssertTrue(loading)
        source.releaseRefreshes()
    }

    func testFavoriteUnknownUntilStoredValue() async {
        favorites = FavoritesStub(stored: nil)
        viewModel = TitleDetailsViewModel(titleId: detailsTitleId, details: source, favorites: favorites)
        await settle()
        XCTAssertNil(viewModel.state.isFavorite)

        favorites.store(true)

        let known = await eventually { viewModel.state.isFavorite == true }
        XCTAssertTrue(known)
    }

    func testFavoriteTapAddsWhenOff() async {
        await showDetails()

        viewModel.onIntent(.favoriteTapped)

        let stored = await eventually { viewModel.state.isFavorite == true }
        XCTAssertTrue(stored)
        XCTAssertEqual(favorites.writes, [true])
        XCTAssertFalse(viewModel.state.isRemoveConfirmationShown)
    }

    func testFavoriteTapAsksToConfirmWhenOn() async {
        favorites = FavoritesStub(stored: true)
        viewModel = TitleDetailsViewModel(titleId: detailsTitleId, details: source, favorites: favorites)
        await showDetails()

        viewModel.onIntent(.favoriteTapped)
        viewModel.onIntent(.favoriteTapped)
        await settle()

        XCTAssertTrue(viewModel.state.isRemoveConfirmationShown)
        XCTAssertEqual(viewModel.state.isFavorite, true)
        XCTAssertEqual(favorites.writes, [])
    }

    func testConfirmRemovesFavorite() async {
        favorites = FavoritesStub(stored: true)
        viewModel = TitleDetailsViewModel(titleId: detailsTitleId, details: source, favorites: favorites)
        await showDetails()
        viewModel.onIntent(.favoriteTapped)

        viewModel.onIntent(.removeFavoriteConfirmed)

        XCTAssertFalse(viewModel.state.isRemoveConfirmationShown)
        let removed = await eventually { viewModel.state.isFavorite == false }
        XCTAssertTrue(removed)
        XCTAssertEqual(favorites.writes, [false])
    }

    func testConfirmTwiceWritesOnce() async {
        favorites = FavoritesStub(stored: true)
        viewModel = TitleDetailsViewModel(titleId: detailsTitleId, details: source, favorites: favorites)
        await showDetails()
        viewModel.onIntent(.favoriteTapped)

        viewModel.onIntent(.removeFavoriteConfirmed)
        viewModel.onIntent(.removeFavoriteConfirmed)
        await settle()

        XCTAssertEqual(favorites.writes, [false])
    }

    func testDismissKeepsFavorite() async {
        favorites = FavoritesStub(stored: true)
        viewModel = TitleDetailsViewModel(titleId: detailsTitleId, details: source, favorites: favorites)
        await showDetails()
        viewModel.onIntent(.favoriteTapped)

        viewModel.onIntent(.removeFavoriteDismissed)
        await settle()

        XCTAssertFalse(viewModel.state.isRemoveConfirmationShown)
        XCTAssertEqual(viewModel.state.isFavorite, true)
        XCTAssertEqual(favorites.writes, [])
    }

    func testFavoriteTapIgnoredWithoutDetails() async {
        source.holdRefreshes = true
        viewModel.onIntent(.languageChanged(.enUs))
        let known = await eventually { viewModel.state.isFavorite == false }
        XCTAssertTrue(known)

        viewModel.onIntent(.favoriteTapped)
        await settle()

        XCTAssertEqual(favorites.writes, [])
        source.releaseRefreshes()
    }

    func testTrailerTapOpensTrailer() async {
        await showDetails()
        var effects = viewModel.effects.makeAsyncIterator()

        viewModel.onIntent(.trailerTapped)

        let effect = await effects.next()
        XCTAssertEqual(effect, .openTrailer(URL(string: "https://www.youtube.com/watch?v=key")!))
    }

    func testTrailerTapWithoutTrailerDoesNothing() async {
        await showDetails(details(trailerURL: nil))
        let effects = viewModel.effects
        var received: [TitleDetailsEffect] = []
        let collector = Task { for await effect in effects { received.append(effect) } }

        viewModel.onIntent(.trailerTapped)
        await settle()

        XCTAssertEqual(received, [])
        collector.cancel()
    }

    func testRecommendationTapOpensTitle() async {
        let recommended = TitleItem(id: "tv-1396", name: "Breaking Bad", posterURL: nil)
        await showDetails(details(recommendations: [recommended]))
        var effects = viewModel.effects.makeAsyncIterator()

        viewModel.onIntent(.recommendationTapped(recommended))

        let effect = await effects.next()
        XCTAssertEqual(effect, .openTitle(recommended))
    }

    private func showDetails(_ shown: TitleDetailsItem = details()) async {
        source.send(shown, for: .enUs)
        viewModel.onIntent(.languageChanged(.enUs))
        let loaded = await eventually { viewModel.state.load == .loaded(shown) && viewModel.state.isFavorite != nil }
        XCTAssertTrue(loaded)
    }
}
