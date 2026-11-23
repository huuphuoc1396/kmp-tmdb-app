# Plan: Remove with confirmation (cmp)

Requirement: [03 - Remove with confirmation (cmp)](../subtasks/03-cmp-remove-favorite.md)
Status: Done

## Summary
A shared `RemoveFavoriteDialog` in `cmp/designsystem` (with its own en / pt-BR strings) is opened from three
places: the heart on Home's favorite items, the heart on My favorites' cards, and the filled heart on Title
details. Each ViewModel keeps the pending removal in its UI state (MVI), removes on "Remove" with
`FavoritesRepository.setFavorite(id, false)` and closes on "Cancel", outside tap or back. My favorites gets its
empty message; items and hearts get their screen reader labels. EC-6 is updated to the new popup behavior.

## Decisions
- **Popup state in the ViewModel** (from user; rejected: `remember` in the screen). It is part of each UI state,
  so the popup stays open on rotation (Android and iOS) and is gone after process death (not stored in
  `SavedStateHandle`); the title is still a favorite then. **This changes EC-6**; Step 6 updates task.md and the
  subtask: "Rotation while the popup is open -> the popup stays open and nothing changes; process death -> the
  popup is closed and the title is still a favorite."
- **Dialog in `cmp/designsystem`** (from user; rejected: a copy per module):
  `@Composable fun RemoveFavoriteDialog(titleName: String?, onConfirm: () -> Unit, onDismiss: () -> Unit)`,
  built on `androidx.compose.ui.window.Dialog` with `DialogProperties(dismissOnBackPress = true,
  dismissOnClickOutside = true)` (AC-9). The dialog window takes accessibility focus (AC-13). Strings live in
  `cmp/designsystem/src/commonMain/composeResources` (`Res` in `com.banhmilabs.tmdb.cmp.designsystem.generated.resources`).
- **Unknown name** (a favorite without stored data): the message drops the quoted name —
  "This title will be removed from your list. You can add it again later." /
  "Este título será removido da sua lista. Você pode adicioná-lo novamente depois."
- **Removing is a direct repository call** from each ViewModel (`FavoritesRepository.setFavorite(id, false)`); no
  use case, as it would only forward (CLAUDE.md). `HomeViewModel` and `FavoritesViewModel` get
  `FavoritesRepository` as a new constructor parameter (Koin `viewModelOf` resolves it).
- **UI state**:
  - Home and My favorites: `pendingRemoval: FavoriteTitle? = null`.
  - Title details: `isRemoveConfirmationShown: Boolean = false`; the dialog takes the name from `details.name`.
- **Intents**: Home and My favorites `RemoveFavoriteClicked(favorite: FavoriteTitle)`, `RemoveFavoriteConfirmed`,
  `RemoveFavoriteDismissed`. Title details keeps `FavoriteClicked` (empty heart → add at once; filled heart → show
  the confirmation) and adds `RemoveFavoriteConfirmed`, `RemoveFavoriteDismissed`.
- **EC-1**: a second `RemoveFavoriteClicked` / `FavoriteClicked` while a removal is pending is ignored (one popup);
  `RemoveFavoriteConfirmed` clears the pending removal before writing, so a second confirm does nothing (one write).
- **Title details' own EC-1** ("each tap toggles") no longer holds for removal: removing now goes through the
  popup (favorites AC-7). Its tests `secondClickRemovesFavorite` and `quickTapsEndOnLastTap` are replaced (Step 4).
- **Hearts** (Figma assets missing; drawn from the heart path already in
  `cmp/feature/title-details/ui/.../drawable/ic_favorite_off.xml` / `ic_favorite_on.xml`):
  - Home item: `ic_favorite_remove.xml` in `home/ui` — 28dp circle `#34263C` (alpha 0.9) with the outline heart in
    `#FF1F8A`; placed 6dp from the poster's top and end; touch target 48dp via
    `minimumInteractiveComponentSize()`.
  - My favorites card: `ic_heart_filled.xml` in `favorites/ui` — the filled heart alone, 20×18dp, `#FF1F8A`,
    vertically centered, 24dp from the card's end edge (48dp touch target).
- **Semantics** (AC-13): the item / card is one button labelled with the name (or "Loading title"); the heart is a
  separate button labelled "Remove from favorites" / "Remover dos favoritos" (designsystem string), so the item
  no longer uses `clearAndSetSemantics` over the heart.
- **Empty state of My favorites** (AC-10, `design/my-favorites.md`): when the list is empty, one `LazyColumn` item
  under the header with the message, centered, 14sp `#A6A1B5`, 48dp top padding:
  "No favorites yet. Tap the heart on a title to save it here." /
  "Nenhum favorito ainda. Toque no coração de um título para salvá-lo aqui."
- **Dialog values** (`design/remove-favorite-dialog.md`, ≈ values): width = screen − 2 × 20dp, radius 20dp, fill
  `#211A30`, padding 24dp (bottom 20dp); title white bold 22sp; message `#B0AABF` 14sp / 20sp line height, wraps in
  full (EC-5); buttons in a row, equal weight, 12dp apart, 48dp high, radius 10dp: "Cancel" outlined 1dp `#655F74`
  with white label, "Remove" filled `secondary` with white label, both SemiBold 16sp. The content column scrolls
  if it doesn't fit (EC-11). Scrim: the platform's default dialog dim.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Remove a favorite | `FavoritesRepository.setFavorite(id, false)` | `service/favorites/domain` |
| Favorite state on details | `TitleDetailsViewModel` (`isFavorite`, `FavoriteClicked`) | `cmp/feature/title-details/ui` |
| Home section / My favorites list | `FavoritesSection`, `FavoriteCard`, `FavoritesScreen` (plan 02) | `cmp/feature/home/ui`, `cmp/feature/favorites/ui` |
| Heart vector path | `ic_favorite_off.xml`, `ic_favorite_on.xml` | `cmp/feature/title-details/ui/src/commonMain/composeResources/drawable` |
| Theme pink | `MaterialTheme.colorScheme.secondary` | `cmp/designsystem/.../Theme.kt` |
| Test doubles | `FakeFavoritesRepository` (`writes`), `FakeTitleDetailsRepository`, `MainDispatcherTest` | `service/*/fake`, `core/testing` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `cmp/designsystem` | `component/RemoveFavoriteDialog.kt` (new); `composeResources/values/strings.xml`, `values-pt-rBR/strings.xml` (new) | Shared dialog and its copy |
| `cmp/feature/home/ui` | `HomeContract.kt`, `HomeViewModel.kt`, `HomeScreen.kt`, `component/FavoritesSection.kt` (modify); `drawable/ic_favorite_remove.xml` (new); tests `HomeTestFixtures.kt`, `HomeViewModelFavoritesTest.kt` (modify) | Heart + popup on Home |
| `cmp/feature/favorites/ui` | `FavoritesContract.kt`, `FavoritesViewModel.kt`, `FavoritesScreen.kt`, `component/FavoriteCard.kt`, strings (modify); `drawable/ic_heart_filled.xml` (new); `FavoritesViewModelTest.kt` (modify) | Heart, popup, empty state |
| `cmp/feature/title-details/ui` | `TitleDetailsContract.kt`, `TitleDetailsViewModel.kt`, `TitleDetailsScreen.kt` (modify); `TitleDetailsViewModelFavoriteTest.kt` (modify) | Confirm before removing on details |
| Docs | `docs/features/favorites/task.md`, `subtasks/03-cmp-remove-favorite.md` (EC-6), `CLAUDE.md` (reuse row) | Requirement change, reuse |

## Steps
### Step 1: `RemoveFavoriteDialog` in the design system  (AC-6, AC-9, AC-12, AC-13, EC-5, EC-11) ✅
- Files: `cmp/designsystem/src/commonMain/kotlin/com/banhmilabs/tmdb/cmp/designsystem/component/RemoveFavoriteDialog.kt`
  (new); `cmp/designsystem/src/commonMain/composeResources/values/strings.xml`, `values-pt-rBR/strings.xml` (new)
- What:
  - `RemoveFavoriteDialog(titleName: String?, onConfirm: () -> Unit, onDismiss: () -> Unit)` per Decisions; title
    has `heading()`; buttons are `Role.Button` with `minimumInteractiveComponentSize()`.
  - Strings: `remove_favorite_title` Remove from favorites? / Remover dos favoritos?;
    `remove_favorite_message` “%1$s” will be removed from your list. You can add it again later. / “%1$s” será
    removido da sua lista. Você pode adicioná-lo novamente depois.; `remove_favorite_message_unknown` (Decisions);
    `remove_favorite_cancel` Cancel / Cancelar; `remove_favorite_confirm` Remove / Remover;
    `remove_favorite_heart` Remove from favorites / Remover dos favoritos.
- Unit tests: none (composable).
- Compile: `./gradlew :cmp:designsystem:compileAndroidMain :cmp:designsystem:compileKotlinIosSimulatorArm64`

### Step 2: Home: heart and confirmation  (AC-4, AC-6, AC-8, AC-9, AC-11, AC-13, EC-1, EC-6) ✅
- Files: `cmp/feature/home/ui/.../HomeContract.kt`, `HomeViewModel.kt`, `HomeScreen.kt`,
  `component/FavoritesSection.kt` (modify); `composeResources/drawable/ic_favorite_remove.xml` (new);
  `androidHostTest/.../HomeTestFixtures.kt`, `HomeViewModelFavoritesTest.kt` (modify)
- What:
  - `HomeUiState.pendingRemoval: FavoriteTitle? = null`; intents `RemoveFavoriteClicked(favorite)`,
    `RemoveFavoriteConfirmed`, `RemoveFavoriteDismissed`.
  - `HomeViewModel(repository, observeFavoriteTitles, favoritesRepository: FavoritesRepository, savedStateHandle)`;
    `pendingRemoval` is a `MutableStateFlow` combined into `state`.
  - `FavoritesSection` gets `onRemoveClick: (FavoriteTitle) -> Unit`; each item is a `Box`: the clickable item
    (button, name label) and, on top, the heart button (`ic_favorite_remove`, label `remove_favorite_heart`).
  - `HomeContent` shows `RemoveFavoriteDialog(pending.summary?.name, …)` when `state.pendingRemoval != null`.
  - Fixture: `homeViewModel(...)` passes the `FakeFavoritesRepository` to the new parameter too.
- Pseudo-code:
  ```kotlin
  RemoveFavoriteClicked -> if (pendingRemoval.value == null) pendingRemoval.value = intent.favorite
  RemoveFavoriteConfirmed -> pendingRemoval.getAndUpdate { null }?.let { viewModelScope.launch { favorites.setFavorite(it.id, false) } }
  RemoveFavoriteDismissed -> pendingRemoval.value = null
  ```
- Unit tests (`HomeViewModelFavoritesTest`): `removeClickAsksToConfirm`, `secondRemoveClickKeepsFirstRequest`,
  `confirmRemovesFavoriteAndCloses`, `confirmTwiceWritesOnce`, `dismissKeepsFavorite`,
  `removingLastFavoriteHidesSection`.
- Compile: `./gradlew :cmp:feature:home:ui:compileAndroidMain :cmp:feature:home:ui:compileAndroidHostTest :cmp:composeApp:compileAndroidMain`

### Step 3: My favorites: heart, confirmation, empty state  (AC-4, AC-6, AC-8, AC-9, AC-10, AC-11, AC-12, AC-13, EC-1, EC-6) ✅
- Files: `cmp/feature/favorites/ui/.../FavoritesContract.kt`, `FavoritesViewModel.kt`, `FavoritesScreen.kt`,
  `component/FavoriteCard.kt`, `composeResources/values/strings.xml`, `values-pt-rBR/strings.xml` (modify);
  `composeResources/drawable/ic_heart_filled.xml` (new); `androidHostTest/.../FavoritesViewModelTest.kt` (modify)
- What:
  - `FavoritesUiState(favorites, pendingRemoval: FavoriteTitle? = null)`; same three intents as Home.
  - `FavoritesViewModel(observeFavoriteTitles, favoritesRepository: FavoritesRepository)`; same logic as Step 2.
  - `FavoriteCard(favorite, onClick, onRemoveClick)`: the heart (`ic_heart_filled`) is a separate button at the
    end; the name / meta column keeps `weight(1f)`.
  - Empty item and `favorites_empty` strings (Decisions); dialog shown from `FavoritesContent`.
- Unit tests (`FavoritesViewModelTest`): `removeClickAsksToConfirm`, `secondRemoveClickKeepsFirstRequest`,
  `confirmRemovesFavoriteAndCloses`, `confirmTwiceWritesOnce`, `dismissKeepsFavorite`,
  `removingLastFavoriteLeavesEmptyList`.
- Compile: `./gradlew :cmp:feature:favorites:ui:compileAndroidMain :cmp:feature:favorites:ui:compileAndroidHostTest :cmp:composeApp:compileAndroidMain`

### Step 4: Title details: confirm before removing  (AC-7, AC-8, AC-9, EC-1, EC-6) ✅
- Files: `cmp/feature/title-details/ui/.../TitleDetailsContract.kt`, `TitleDetailsViewModel.kt`,
  `TitleDetailsScreen.kt` (modify); `androidHostTest/.../TitleDetailsViewModelFavoriteTest.kt` (modify)
  (`title-details/ui` already depends on `cmp:designsystem`)
- What:
  - `TitleDetailsUiState.isRemoveConfirmationShown: Boolean = false`; intents `RemoveFavoriteConfirmed`,
    `RemoveFavoriteDismissed`.
  - `FavoriteClicked`: `isFavorite == false` → `setFavorite(true)`; `isFavorite == true` → show the confirmation;
    `null` → nothing (unchanged).
  - `TitleDetailsScreen` shows `RemoveFavoriteDialog(state.details?.name, …)` when the flag is set.
- Unit tests (`TitleDetailsViewModelFavoriteTest`): replace `secondClickRemovesFavorite` with
  `clickOnFavoriteAsksToConfirm` and `confirmRemovesFavorite`; replace `quickTapsEndOnLastTap` with
  `quickTapsOnFavoriteKeepOneConfirmation`; add `dismissKeepsFavorite`, `confirmTwiceWritesOnce`. Keep
  `favoriteClickStoresTrue` (add with no popup) and the others.
- Compile: `./gradlew :cmp:feature:title-details:ui:compileAndroidMain :cmp:feature:title-details:ui:compileAndroidHostTest :cmp:composeApp:compileAndroidMain :cmp:composeApp:compileKotlinIosSimulatorArm64`

### Step 5: Docs  (EC-6) ✅
- Files: `docs/features/favorites/task.md`, `docs/features/favorites/subtasks/03-cmp-remove-favorite.md`, `CLAUDE.md`
- What:
  - task.md and subtask 03: EC-6 text per Decisions; task.md Change log line "EC-6: popup stays open on rotation,
    closes on process death (user, plan 03)".
  - `CLAUDE.md` reuse table: "Confirm removing a favorite (cmp) | `RemoveFavoriteDialog` | `cmp/designsystem`".
- Compile: none.

After the last step: `testAndroidHostTest` and `detekt` on `cmp:designsystem`, `cmp:feature:home:ui`,
`cmp:feature:favorites:ui`, `cmp:feature:title-details:ui`, `cmp:composeApp`; `./gradlew :architecture-test:test`.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-4 (heart) | 2, 3 | Manual: heart on Home items and My favorites cards |
| AC-6 | 1, 2, 3 | `removeClickAsksToConfirm` (Home, My favorites); manual: popup copy |
| AC-7 | 4 | `clickOnFavoriteAsksToConfirm`, `favoriteClickStoresTrue` |
| AC-8 | 2, 3, 4 | `confirmRemovesFavoriteAndCloses` (Home, My favorites), `confirmRemovesFavorite`, `removingLastFavoriteHidesSection` |
| AC-9 | 1, 2, 3, 4 | `dismissKeepsFavorite` (all three); manual: outside tap and back |
| AC-10 | 3 | `removingLastFavoriteLeavesEmptyList`; manual: empty message, back → no section |
| AC-11 (remove offline) | 2, 3 | Covered by the local-only repository (plan 01); manual: airplane mode |
| AC-12 (popup and empty copy) | 1, 3 | Manual: en / pt-BR |
| AC-13 | 1, 2, 3 | Manual: TalkBack / VoiceOver, touch targets |
| EC-1 | 2, 3, 4 | `secondRemoveClickKeepsFirstRequest`, `confirmTwiceWritesOnce`, `quickTapsOnFavoriteKeepOneConfirmation` |
| EC-5 (popup) | 1 | Manual: long name wraps in full |
| EC-6 (updated) | 2, 3, 4, 5 | Manual: rotate with the popup open (stays), kill the process (closed, still a favorite) |
| EC-11 | 1, 2, 3 | Manual: largest font size |

## Out of scope
- Native apps → 04, 05.
- Undo after removing (task.md: out of scope).
- Exact Figma values and assets (rate limit; sizes are ≈ from screenshots).

## Risks
- The platform's default dialog dim may differ from the design's ≈68% black scrim; accept it unless the user asks
  for a custom scrim.
- The hearts are drawn from the existing details heart path, not the Figma assets (missing: rate limit; owner:
  user, after the limit resets).
- Changing Title details' favorite behavior alters that feature's EC-1; its task.md is not edited here — the
  favorites task (AC-7) supersedes it, noted in the Decisions.

## Deviations
- designsystem's generated `Res` is `internal`, so Home and My favorites can't read `remove_favorite_heart`;
  `RemoveFavoriteDialog.kt` also exposes `@Composable fun removeFavoriteLabel(): String` for the heart buttons.
- Detekt `TooManyFunctions` (max 11): the removal tests are in their own classes,
  `HomeViewModelRemoveFavoriteTest` and `FavoritesViewModelRemoveTest`, like the existing
  `HomeViewModelRefreshTest` split; test names are as planned.
- Home's heart sits in a `Box` over the item and is offset 4dp (`HeartTouchOffset`) so the 28dp circle keeps its
  6dp inset while the touch target is 48dp; the My favorites card's clickable area is an inner `Row` (poster and
  text) so the heart stays a separate button.
