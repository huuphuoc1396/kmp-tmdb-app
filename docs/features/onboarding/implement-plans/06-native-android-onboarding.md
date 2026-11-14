# Plan: Onboarding on Android (native)

Requirement: [06 - Onboarding on Android (native)](../subtasks/06-native-android-onboarding.md)
Status: Done

## Summary
The onboarding of `apps/cmp` is ported to the standalone `native-android/` app with Jetpack Compose and Android
resources. It keeps the same MVI shape, layout, copy, drawables and start-screen logic:
- `AppViewModel` reads `SettingsRepository.observeOnboardingCompleted()` once, from the published
  `service-settings-domain`.
- The SplashScreen API stays up until that read is done, so no wrong screen flashes (AC-3).
- "Get started" / "Acessar" records completion and then replaces onboarding with the placeholder Home (AC-2, AC-4,
  EC-1, EC-3).

Everything stays in the single `:app` module, in an `onboarding` package, because this is the app's first feature
(architecture 11: modules are split out when a second feature needs them). Tests are ViewModel unit tests with MockK
(from user). The UI is checked manually on the emulator.

## Decisions
- **ViewModel tests only** (from user):
  - `OnboardingViewModelTest` and `AppViewModelTest` run on JVM with MockK for the published `SettingsRepository`.
  - Screen behaviour (copy per locale, semantics, 48dp button, large font, landscape and tablet, back stack) is
    checked manually on the emulator with the subtask's "How to test".
  - Rejected: Robolectric + Compose UI tests, and instrumented tests.
- **Main dispatcher in tests: a local `MainDispatcherRule`** (from user). It is a JUnit 4 `TestWatcher` in
  `app/src/test` that calls `Dispatchers.setMain(StandardTestDispatcher())` and `resetMain()`. `core:testing` is
  not published, and the test-only modules stay unpublished (native-android-app task). Rejected: publishing
  `core:testing`.
- **Same design decisions as cmp**, from plans 04 and 05 and design/onboarding.md, not re-asked:
  - The official TMDB logo keeps its own colours.
  - The headline is one Light-weight string marked as a heading.
  - The artwork is a gradient placeholder drawable with a cover crop (`Crop`, `TopCenter`), under a scrim drawable.
  - The content block is bottom-anchored, max width 315dp, and scrolls inside `safeDrawing` insets.
  - The button is a gradient pill with min height 48dp and a ripple, and `Role.Button`.
  - The ViewModel navigates after `completeOnboarding()` returns, whether it succeeded or failed.
  - `isNavigating` ignores repeated taps.
  - `AppViewModel` reads the state once with `first()`.
  - Navigation pops onboarding with `popUpTo<OnboardingRoute> { inclusive = true }`.
  - The splash screen is a plain dark screen with a transparent icon.
- **Package layout** (CLAUDE.md conventions, by screen, inside `:app`):
  - `com.banhmilabs.tmdb.nativeapp.onboarding`:
    - `navigation/` holds `OnboardingRoute` and `NavGraphBuilder.onboardingScreen(onFinished)`.
    - `di/` holds `onboardingModule`.
    - `OnboardingContract.kt`, `OnboardingViewModel.kt` and `OnboardingScreen.kt` (with `OnboardingScreen` and
      `OnboardingContent`).
    - `component/GetStartedButton.kt`.
  - The app shell stays at the package root: `App.kt`, `AppViewModel.kt`, `StartDestination.kt`, `AppNavHost.kt`,
    `HomeRoute.kt`, `PlaceholderHomeScreen.kt`, `MainActivity.kt` and `TmdbApplication.kt`.
  - `di/NativeAppModule.kt` includes `onboardingModule` and declares `AppViewModel`.
- **Android resources**:
  - Strings go in `res/values/strings.xml` (en) and `res/values-pt-rBR/strings.xml`. pt-BR gets Portuguese; any
    other language falls back to English (AC-5, EC-6).
  - The drawables `tmdb_logo.xml`, `onboarding_artwork_placeholder.xml` and `onboarding_scrim.xml` are copied from
    the cmp module's `composeResources/drawable`. They are already Android vector drawables.
  - Copied, not shared: `native-android/` shares nothing with the KMP build except the published core.
- **Remove `SharedCoreApiTest`**: `AppViewModelTest` now reads `observeOnboardingCompleted()` through the published
  domain API with MockK. That still covers native-android-app AC-4, so the smoke test would duplicate it.
- **Architecture doc, section 8**: the native Android row changes to "JUnit 4 + MockK ViewModel tests against the
  published domain API; UI checked manually on device". This records the choice of testing ViewModels only.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Onboarding state | `SettingsRepository.observeOnboardingCompleted()`, `completeOnboarding(): AppResult<Unit>` | published `com.banhmilabs.tmdb:service-settings-domain` (+ `core-common` transitively) |
| Koin start-up and settings data | `initKoin { androidContext(...); modules(...) }` | published `com.banhmilabs.tmdb:core-di` |
| Screen, contract, ViewModel, button, layout values | `OnboardingScreen`, `OnboardingContract`, `OnboardingViewModel`, `GetStartedButton` | `apps/cmp/feature/onboarding/ui/src/commonMain/...` (ported) |
| Start screen and splash | `AppViewModel`, `StartDestination`, `App`, `AppNavHost`, `MainActivity` splash, `themes.xml`, `colors.xml` | `apps/cmp/composeApp`, `apps/cmp/androidApp` (ported) |
| Strings and drawables | `strings.xml` (en, pt-rBR), `tmdb_logo.xml`, `onboarding_artwork_placeholder.xml`, `onboarding_scrim.xml` | `apps/cmp/feature/onboarding/ui/src/commonMain/composeResources/` (copied) |
| Test cases | `OnboardingViewModelTest`, `AppViewModelTest` | `apps/cmp/.../jvmTest` (ported to MockK) |
| Theme, placeholder Home, NavHost, edge-to-edge | `TmdbTheme`, `PlaceholderHomeScreen`, `AppNavHost`, `MainActivity` | `native-android/app` (existing) |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `native-android` catalog | `gradle/libs.versions.toml` (modify) | core-splashscreen, lifecycle compose, koin-androidx-compose |
| `native-android/app` build + resources | `build.gradle.kts`, `AndroidManifest.xml` (modify); `res/values/{strings,colors,themes}.xml`, `res/values-pt-rBR/strings.xml`, `res/drawable/{tmdb_logo,onboarding_artwork_placeholder,onboarding_scrim}.xml` (new) | Copy, artwork, splash |
| `native-android/app` onboarding | `onboarding/{OnboardingContract,OnboardingViewModel,OnboardingScreen}.kt`, `onboarding/component/GetStartedButton.kt`, `onboarding/navigation/{OnboardingRoute,OnboardingNavigation}.kt`, `onboarding/di/OnboardingModule.kt` (new) | The feature |
| `native-android/app` shell | `App.kt`, `AppViewModel.kt`, `StartDestination.kt`, `di/NativeAppModule.kt` (new); `AppNavHost.kt`, `MainActivity.kt`, `TmdbApplication.kt` (modify) | Start screen, splash, Koin |
| `native-android/app` tests | `MainDispatcherRule.kt`, `onboarding/OnboardingViewModelTest.kt`, `AppViewModelTest.kt` (new); `SharedCoreApiTest.kt` (delete) | Unit tests |
| docs | `docs/architecture.md` (section 8 row), subtask 06 | Test approach, status |

The KMP build and the shared core are not changed.

## Steps
### Step 1: Dependencies, resources and splash theme  (AC-1, AC-5, AC-6, EC-6) ✅
- Files: `native-android/gradle/libs.versions.toml`, `native-android/app/build.gradle.kts`,
  `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/{strings,colors,themes}.xml`,
  `app/src/main/res/values-pt-rBR/strings.xml`, `app/src/main/res/drawable/*.xml` (3, copied)
- What:
  - **Catalog**:
    - `androidx-core-splashscreen = "1.2.0"`.
    - `lifecycle`: the latest stable AndroidX lifecycle that navigation-compose 2.10.2 accepts.
    - Libraries: `core-splashscreen`, `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose` and
      `koin-androidx-compose` (version `koin`).
  - **`app/build.gradle.kts`**: `implementation` of the four libraries.
  - **Strings** (`onboarding_headline`, `onboarding_body`, `onboarding_get_started`): the same text as the cmp
    `composeResources` files, en in `values/` and pt-BR in `values-pt-rBR/`.
  - **Splash**:
    - `colors.xml` has `tmdb_night` `#FF15151D`.
    - `themes.xml` has `Theme.Tmdb.Starting`: parent `Theme.SplashScreen`, background `@color/tmdb_night`,
      animated icon `@android:color/transparent`, and `postSplashScreenTheme` =
      `@android:style/Theme.Material.NoActionBar`.
    - The manifest `application` theme becomes `@style/Theme.Tmdb.Starting`.
  - **Drawables**: copy the three vector drawables unchanged.
- Unit tests: none.
- Compile: `cd native-android && ./gradlew :app:assembleDebug`

### Step 2: Onboarding feature package  (AC-1, AC-2, AC-6, AC-7, AC-8, EC-1, EC-3, EC-4, EC-5) ✅
- Files: `app/src/main/kotlin/com/banhmilabs/tmdb/nativeapp/onboarding/` with `OnboardingContract.kt`,
  `OnboardingViewModel.kt`, `OnboardingScreen.kt`, `component/GetStartedButton.kt`,
  `navigation/OnboardingRoute.kt`, `navigation/OnboardingNavigation.kt` and `di/OnboardingModule.kt`
- What (ported from the cmp module; `R.string` / `R.drawable` with `stringResource` / `painterResource` from
  `androidx.compose.ui.res`):
  - `internal data class OnboardingUiState(val isNavigating: Boolean = false)`.
  - `internal sealed interface OnboardingIntent { data object GetStartedClicked }`.
  - `internal sealed interface OnboardingEffect { data object NavigateToHome }`.
  - `internal class OnboardingViewModel(private val settingsRepository: SettingsRepository) : ViewModel()`:
    - `state: StateFlow<OnboardingUiState>`.
    - `effects: Flow<OnboardingEffect>`, from `Channel(BUFFERED).receiveAsFlow()`.
    - `onIntent(intent)`, which calls `getStarted()`: if already navigating, return. Otherwise set
      `isNavigating`, then launch `completeOnboarding()` and send `NavigateToHome`.
  - `OnboardingScreen(onFinished, viewModel = koinViewModel())` collects `state`
    (`collectAsStateWithLifecycle`) and `effects` (`LaunchedEffect`), then draws
    `OnboardingContent(state, onIntent, modifier)`. The layout is the same as cmp:
    - The artwork `Image` with `Crop` / `TopCenter` and the scrim with `FillBounds`, both with
      `contentDescription = null`.
    - `BoxWithConstraints` → `Column` with `verticalScroll`, `heightIn(min = maxHeight)`,
      `windowInsetsPadding(WindowInsets.safeDrawing)` and padding 29 / 31 / 31dp, bottom-arranged and centred.
    - An inner column with `widthIn(max = 315.dp)` and a 19dp gap holding the logo (decorative), the headline
      (`semantics { heading() }`, 32sp / 37.3sp, Light, white), the body (15sp / 18.75sp, white) and
      `GetStartedButton(enabled = !state.isNavigating)`.
  - `GetStartedButton`: `fillMaxWidth`, `heightIn(min = 48.dp)`, `clip(CircleShape)`, a linear gradient of
    `primary` → `primaryContainer`, then `clickable(enabled, role = Role.Button)` and 12dp padding. The label is
    16sp SemiBold, white and centred.
  - `@Serializable internal data object OnboardingRoute`;
    `internal fun NavGraphBuilder.onboardingScreen(onFinished: () -> Unit)` =
    `composable<OnboardingRoute> { OnboardingScreen(onFinished) }`.
  - `internal val onboardingModule = module { viewModelOf(::OnboardingViewModel) }`.
- Unit tests: in step 4.
- Compile: `cd native-android && ./gradlew :app:compileDebugKotlin`

### Step 3: Start screen, splash and Koin wiring  (AC-2, AC-3, AC-4, EC-2) ✅
- Files: `App.kt`, `AppViewModel.kt`, `StartDestination.kt`, `di/NativeAppModule.kt` (new); `AppNavHost.kt`,
  `MainActivity.kt`, `TmdbApplication.kt` (modify), all in `com.banhmilabs.tmdb.nativeapp`
- What:
  - `internal enum class StartDestination { Loading, Onboarding, Home }`.
  - `internal class AppViewModel(settingsRepository: SettingsRepository) : ViewModel()`:
    `startDestination: StateFlow<StartDestination>` comes from `flow { emit(if (first()) Home else Onboarding) }`
    and `stateIn(viewModelScope, Eagerly, Loading)`, as in cmp (if / else).
  - `@Composable internal fun App(viewModel: AppViewModel)`: `TmdbTheme` and a background `Box`. The `when` on
    `startDestination` gives `Loading` → nothing, `Onboarding` → `AppNavHost(OnboardingRoute)` and
    `Home` → `AppNavHost(HomeRoute)`.
  - `AppNavHost(startRoute: Any, navController = rememberNavController())`:
    - `onboardingScreen(onFinished = { navController.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive =
      true } } })`.
    - `composable<HomeRoute> { PlaceholderHomeScreen() }`.
  - `val nativeAppModule = module { includes(onboardingModule); viewModelOf(::AppViewModel) }`.
  - `TmdbApplication`: `initKoin { androidContext(this@TmdbApplication); modules(nativeAppModule) }`.
  - `MainActivity`:
    - `private val appViewModel: AppViewModel by viewModel()`.
    - `installSplashScreen().setKeepOnScreenCondition { appViewModel.startDestination.value ==
      StartDestination.Loading }`.
    - Then the existing `enableEdgeToEdge(...)` and `setContent { App(appViewModel) }`.
- Unit tests: in step 4.
- Compile: `cd native-android && ./gradlew :app:assembleDebug`

### Step 4: ViewModel tests  (AC-2, AC-3, EC-1, EC-3) ✅
- Files: `app/src/test/kotlin/com/banhmilabs/tmdb/nativeapp/MainDispatcherRule.kt`,
  `.../onboarding/OnboardingViewModelTest.kt`, `.../AppViewModelTest.kt` (new); `.../SharedCoreApiTest.kt` (delete)
- What:
  - `class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher()`:
    `starting` → `Dispatchers.setMain(dispatcher)`, `finished` → `Dispatchers.resetMain()`.
  - `OnboardingViewModelTest`, using the rule's dispatcher in `runTest` and a MockK `SettingsRepository`:
    - `startsNotNavigating`.
    - `getStartedCompletesOnboardingThenNavigatesHome`: `coEvery { completeOnboarding() } returns
      AppResult.Success(Unit)`. Expect `isNavigating`, `NavigateToHome` from `effects.first()`, and
      `coVerify(exactly = 1)`.
    - `getStartedNavigatesHomeWhenSaveFails`: returns `AppResult.Failure(AppError.Unknown)` and still emits
      `NavigateToHome` (EC-1).
    - `repeatedTapsNavigateOnce`: two `GetStartedClicked` give one `completeOnboarding` call and one effect
      (EC-3).
  - `AppViewModelTest`:
    - `loadingUntilOnboardingStateIsRead`: the flow never emits (`flow { awaitCancellation() }`), so the state
      stays `Loading`.
    - `startsOnOnboardingWhenNotCompleted`: `flowOf(false)` gives `Onboarding`.
    - `startsOnHomeWhenCompleted`: `flowOf(true)` gives `Home` (AC-3).
- Compile: `cd native-android && ./gradlew :app:compileDebugUnitTestKotlin`

### Step 5: Architecture doc  (—) ✅
- Files: `docs/architecture.md`
- What: the section 8 row for native Android becomes "JUnit 4 + MockK ViewModel tests against the published domain
  API; UI checked manually on device | JVM, run locally".
- Unit tests: none.
- Compile: none.

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 onboarding on first launch | 1, 2, 3 | `AppViewModelTest.startsOnOnboardingWhenNotCompleted`; manual "How to test" 1 |
| AC-2 button enters Home | 2, 3 | `OnboardingViewModelTest.getStartedCompletesOnboardingThenNavigatesHome`; manual 2 |
| AC-3 skipped after completion, no flash | 3 | `AppViewModelTest.startsOnHomeWhenCompleted`, `loadingUntilOnboardingStateIsRead`; manual 4 |
| AC-4 onboarding not in back stack; Back leaves app | 3 | Manual 3 |
| AC-5 localized copy | 1 | Manual 6 |
| AC-6 works offline | 1, 2 | Manual 7 (no network code in onboarding) |
| AC-7 accessible | 2 | Manual 9, 10 (TalkBack, largest font) |
| AC-8 orientation and screen sizes | 2 | Manual 11 |
| EC-1 save fails → still Home | 2 | `OnboardingViewModelTest.getStartedNavigatesHomeWhenSaveFails`; manual 8 |
| EC-2 killed before tap → onboarding again | 3 | Manual 5 |
| EC-3 repeated taps → Home once | 2 | `OnboardingViewModelTest.repeatedTapsNavigateOnce`; manual 2 |
| EC-4 rotation / process death restored | 2, 3 | Manual 12 ("Don't keep activities") |
| EC-5 long copy / large font scrolls | 2 | Manual 10 |
| EC-6 other language → English | 1 | Manual 6 |

Checks after the last step: `cd native-android && ./gradlew :app:detekt :app:testDebugUnitTest`.

## Out of scope
- iOS → 07-native-ios-onboarding.
- The real Home screen → Home feature.
- Splitting `:app` into feature / designsystem modules (when a second feature needs them).
- Robolectric or instrumented UI tests (user chose ViewModel tests only).
- Licensed artwork and the 2dp backdrop blur (same as cmp).

## Risks
- **UI behaviour is not covered by automated tests**. Copy per locale, semantics, the touch target, large fonts and
  the back stack rely on the manual checklist. Mitigation: the forge report lists which manual checks were run on
  the emulator, and the layout is a direct port of the cmp screen, which is covered by Compose UI tests in the KMP
  build.
- **AndroidX lifecycle version**: it must be compatible with navigation-compose 2.10.2 and Kotlin 2.4.21.
  Mitigation: pick the version that navigation-compose already resolves (`./gradlew :app:dependencies`) at forge
  time, and note it as a deviation if it differs.
- **Published core is stale**: the app needs `completeOnboarding()` from the published `service-settings-domain`.
  Mitigation: it was published in native-android-app 01. Run `./gradlew publishSharedCore` first if
  `build/maven-repo` is missing.

## Deviations
- AndroidX lifecycle is `2.11.0`, the version navigation-compose already resolves.
- The cmp `testTag`s (`LOGO_TAG`, `CONTENT_TAG`, `GET_STARTED_TAG`) are not ported, because there are no UI tests
  to use them.
- `OnboardingViewModelTest.repeatedTapsNavigateOnce` collects effects in `backgroundScope` with an
  `UnconfinedTestDispatcher`, because `advanceUntilIdle` does not run background tasks. Turbine is not in the
  standalone catalog.

## Manual checks (Pixel_8 emulator)
- Done: a fresh launch shows onboarding (AC-1). A double tap opens Home once (AC-2, EC-3). Back from Home and from
  onboarding leaves the app (AC-4). A relaunch, and a relaunch after force-stop, opens Home (AC-3). pt-BR shows the
  Portuguese copy and "Acessar", and fr-FR shows English (AC-5, EC-6). Landscape with font scale 2.0 keeps the button
  reachable (AC-7, AC-8, EC-5).
- Not done: TalkBack (AC-7), airplane mode (AC-6), a forced save failure on the device (EC-1, covered by a unit
  test), killing before the tap (EC-2), "Don't keep activities" (EC-4), and a tablet.
