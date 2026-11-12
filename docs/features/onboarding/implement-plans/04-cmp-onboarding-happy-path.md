# Plan: Onboarding happy path (cmp)

Requirement: [04 - Onboarding happy path (cmp)](../subtasks/04-cmp-onboarding-happy-path.md)
Status: Done

## Summary
This plan adds the first feature module, `apps/cmp/feature/onboarding/ui`. It holds `OnboardingViewModel`, which
records completion through `SettingsRepository`, and a stateless `OnboardingScreen` drawn as in the design: placeholder
gradient artwork, scrim, the official TMDB logo, and en / pt-BR Compose resources. `composeApp` gets an `AppViewModel`
that reads the onboarding-completed state once and picks the start destination. The NavHost is drawn only after that
read, and the Android splash screen stays up until then, so no wrong screen flashes (AC-3). "Get started" / "Acessar"
navigates to the placeholder Home and pops onboarding off the back stack, whether or not the save succeeded (AC-4,
EC-1). Tests cover both ViewModels and run Compose UI tests on JVM.

## Decisions
- **Logo**: the official TMDB "long" logo with its own gradient (`#90cea1` → `#3cbec9` → `#00b3e5`), not recoloured
  white (from user). The design's white wordmark would alter the logo, which TMDB's attribution guidelines forbid.
  Rejected: the white `group_11.svg` from Figma.
- **Start screen**: Android uses the SplashScreen API (`androidx.core:core-splashscreen`) with
  `setKeepOnScreenCondition` until `AppViewModel` knows the start destination. iOS shows a launch screen in the app
  background colour, then Compose draws the same colour until the start destination is known (from user). Rejected: a
  blank dark background with no splash.
- **Splash icon**: transparent, so the splash is a plain dark screen. There is no app icon yet; without this the
  system would show its default icon.
- **Headline weights**: Light + Bold only (from user). The translated string marks keywords with `<b>…</b>` and is
  parsed with `AnnotatedString.fromHtml`. Commas and " e" / " and" are Light instead of Medium. Rejected: a custom
  three-weight markup.
- **Compose UI tests now**: `runComposeUiTest` on JVM for the screen and the app NavHost (from user, architecture 8).
  Rejected: ViewModel tests only.
- **Feature module**: `apps/cmp/feature/onboarding/ui` only, with no `nav` module. Only the app shell navigates to
  onboarding, so `OnboardingRoute` lives in `ui` (architecture 5.5).
- **Strings and logo live in the feature module's Compose resources**, not in `designsystem`. Only onboarding uses
  them; `designsystem` holds resources shared by two or more features.
- **The generated `Res` class goes in `<module namespace>.generated.resources`**, set in `Compose.kt`. Compose's
  default package is built from `group` and the project name. Pinning it under the module's own package keeps the
  Konsist rules true: "files use their module package", and "feature ui imports no other feature ui", which allows
  imports under the module's own package (`architecture-test/.../DependencyRulesTest.kt`).
- **Locale qualifier `values-pt-rBR`**, not `values-pt`. The requirement is pt-BR → Portuguese and anything else →
  English (AC-5, EC-6).
- **Navigation is a callback**: the screen exposes `onFinished`, and `composeApp` navigates to `HomeRoute` with
  `popUpTo<OnboardingRoute> { inclusive = true }`. `ui` never sees `HomeRoute` (5.4: the shell builds the graph).
- **The ViewModel navigates after `completeOnboarding()` returns**, success or failure. Navigating first would pop the
  destination and cancel `viewModelScope` mid-save (EC-1).
- **`AppViewModel` reads the state once** (`first()`), not continuously. Completing onboarding must not swap the graph
  under the user; navigation does that.
- **Text styles and the button are local to the feature.** There is one user, and Inter / typography tokens are not
  set yet (plan 03: system font for now). Sizes, line heights and weights come from the design. Colours come from
  `MaterialTheme.colorScheme` (`primary` = `#8000FF`, `primaryContainer` = `#540BA1`); the artwork and scrim colours
  are one-off private values.
- **Backdrop blur skipped**: a 2dp blur over a smooth gradient placeholder has no visible effect. Add it with the
  licensed artwork.
- **Koin wiring**: `onboardingUiModule` (in `ui`) and `cmpAppModule` (in `composeApp`, which includes it) declare the
  ViewModels with `viewModelOf`. Both app entry points pass `cmpAppModule` to `initKoin { modules(...) }`.
  `koinViewModel()` resolves them.
- **Test Main dispatcher in `core:testing`**: `MainDispatcherTest`, a base class that sets and resets
  `Dispatchers.Main`. Two modules need it (onboarding `ui` and `composeApp`, CLAUDE.md "Test dispatchers").
- **iOS localizations**: `CFBundleLocalizations` = `en`, `pt-BR` in `Info.plist`, so iOS reports pt-BR as the app's
  preferred language when the device uses it.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Onboarding state | `SettingsRepository` (`observeOnboardingCompleted`, `completeOnboarding`) | `service/settings/domain` |
| Repository test double | `FakeSettingsRepository` (`failNextComplete`) | `service/settings/fake` |
| Theme and brand colours | `TmdbTheme`, `MaterialTheme.colorScheme` | `apps/cmp/designsystem` |
| Placeholder Home and its route | `PlaceholderHomeScreen`, `HomeRoute` | `apps/cmp/composeApp` |
| Koin start-up | `initKoin(appDeclaration)` | `core:di` |
| Compose setup per module | `configureComposeMultiplatform()` | `build-logic/.../Compose.kt` |
| Module type, namespace, dependency check | `tmdb.module` (`:apps:<app>:feature:<x>:ui` → `UI`) | `build-logic/convention` |
| Koin module shape | `settingsDataModule` (`val … = module { … }` in a `di/` package) | `service/settings/data/.../di/SettingsDataModule.kt` |
| Module namespace for the `Res` package | `derivedNamespace()` | `build-logic/.../ModuleConventionPlugin.kt`, `KotlinMultiplatform.kt` |
| Edge-to-edge with light bars | `MainActivity` `enableEdgeToEdge(SystemBarStyle.dark(...))` | `apps/cmp/androidApp` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| catalog | `gradle/libs.versions.toml` (modify) | Compose resources and UI test, lifecycle ViewModel / runtime compose, Koin ViewModel, core-splashscreen |
| build-logic | `buildlogic/Compose.kt` (modify) | Resources, Android resources and JVM UI tests for every cmp Compose module |
| `core:testing` | `MainDispatcherTest.kt` (new), `build.gradle.kts` (modify), `TestingSmokeTest.kt` (delete) | Test Main dispatcher for ViewModel tests |
| `apps/cmp/feature/onboarding/ui` | new module: `build.gradle.kts`, `OnboardingRoute.kt`, `OnboardingUiState.kt`, `OnboardingViewModel.kt`, `OnboardingScreen.kt`, `OnboardingNavigation.kt`, `di/OnboardingUiModule.kt`, `composeResources/` (strings en + pt-BR, `drawable/tmdb_logo.xml`), tests | The feature |
| `apps/cmp/composeApp` | `App.kt` (modify), `AppViewModel.kt`, `StartDestination.kt`, `AppNavHost.kt`, `di/CmpAppModule.kt` (new), `SharedCore.kt` (modify), `build.gradle.kts` (modify), tests | Start destination, graph, Koin |
| `apps/cmp/androidApp` | `MainActivity.kt`, `TmdbApplication.kt`, `AndroidManifest.xml`, `build.gradle.kts` (modify), `res/values/themes.xml`, `res/values/colors.xml` (new) | Splash screen, Koin modules |
| `apps/cmp/iosApp` | `Info.plist`, `project.pbxproj` (modify), `Assets.xcassets/` (new) | Launch screen colour, localizations |
| root | `settings.gradle.kts` (modify) | Include the feature module |
| docs | `design/onboarding.md`, `subtasks/04-…`, `CLAUDE.md` (modify) | Logo asset, status, current state |

## Steps
### Step 1: Catalog, Compose build logic and the test Main dispatcher ✅
- Files: `gradle/libs.versions.toml`, `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/Compose.kt`,
  `core/testing/build.gradle.kts`,
  `core/testing/src/commonMain/kotlin/com/banhmilabs/tmdb/core/testing/MainDispatcherTest.kt` (new); delete
  `core/testing/src/jvmTest/.../TestingSmokeTest.kt`
- What:
  - Catalog versions: `androidx-lifecycle = "2.11.0"` (JetBrains), `androidx-core-splashscreen = "1.2.0"`.
  - Catalog libraries:
    - `compose-components-resources` (`org.jetbrains.compose.components:components-resources`) and
      `compose-ui-test` (`org.jetbrains.compose.ui:ui-test`), both on `compose-multiplatform`.
    - `androidx-lifecycle-viewmodel-compose` and `androidx-lifecycle-runtime-compose`
      (`org.jetbrains.androidx.lifecycle:*`).
    - `koin-compose-viewmodel` (`io.insert-koin:koin-compose-viewmodel`).
    - `androidx-core-splashscreen`.
  - `configureComposeMultiplatform()`:
    - Add `compose-components-resources` to `commonMain`.
    - Set `androidResources { enable = true }` on the `KotlinMultiplatformAndroidLibraryExtension`; the AGP KMP
      library plugin packages no resources without it.
    - Set `compose.resources { packageOfResClass = "${derivedNamespace()}.generated.resources" }`. The class stays
      `internal` (the default).
    - Add `compose-ui-test` and the host's Compose desktop runtime to `jvmTest`, through
      `ComposeExtension.dependencies.desktop.currentOs`, the same version as the plugin.
  - `core:testing`:
    - `commonMain`: `api(libs.kotlinx.coroutines.test)` and `api(libs.kotlin.test)`.
    - `abstract class MainDispatcherTest(protected val testDispatcher: TestDispatcher = UnconfinedTestDispatcher())`,
      which calls `Dispatchers.setMain(testDispatcher)` in `@BeforeTest` and `Dispatchers.resetMain()` in
      `@AfterTest`.
- Unit tests: none of its own; it is exercised by the ViewModel tests in steps 3 and 5. Delete the smoke test.
- Compile: `./gradlew :core:testing:compileKotlinJvm :apps:cmp:composeApp:compileKotlinJvm :apps:cmp:designsystem:compileKotlinJvm`

### Step 2: Onboarding feature module, strings and logo  (AC-5, AC-6, EC-6) ✅
- Files: `settings.gradle.kts` (include `:apps:cmp:feature:onboarding:ui`),
  `apps/cmp/feature/onboarding/ui/build.gradle.kts`, and `src/commonMain/composeResources/`:
  `values/strings.xml`, `values-pt-rBR/strings.xml`, `drawable/tmdb_logo.xml`;
  `docs/features/onboarding/design/assets/tmdb_logo_long.svg` (new, already downloaded)
- What:
  - `build.gradle.kts`:
    - `commonMain`: `projects.service.settings.domain`, `projects.apps.cmp.designsystem`,
      `libs.androidx.navigation.compose`, `libs.kotlinx.serialization.core`,
      `libs.androidx.lifecycle.viewmodel.compose`, `libs.androidx.lifecycle.runtime.compose`,
      `libs.koin.compose.viewmodel`.
    - `jvmTest`: `projects.service.settings.fake`, `projects.core.testing`, `libs.turbine`.
  - Strings, in English for `values` and pt-BR for `values-pt-rBR`, copied from design/onboarding.md "Copy":
    - `onboarding_headline`: keywords wrapped in `&lt;b&gt;…&lt;/b&gt;`.
    - `onboarding_body`.
    - `onboarding_get_started`: "Get started" / "Acessar".
  - `tmdb_logo.xml`: an Android vector converted from TMDB's `blue_long_1` SVG (single path, horizontal linear
    gradient). Viewport `423.04×35.4`, size `179×15dp`; the gradient goes in an `aapt:attr` `<gradient>`. Record the
    source URL in an XML comment.
  - `design/onboarding.md`: Assets row "TMDB official logo" → `assets/tmdb_logo_long.svg`, status shipped as
    `drawable/tmdb_logo.xml`, gradient colours kept. Add a change log entry.
- Unit tests: none (resources); used in the step 4 tests.
- Compile: `./gradlew :apps:cmp:feature:onboarding:ui:compileKotlinJvm` (also generates `Res`)

### Step 3: OnboardingViewModel  (AC-2, EC-1, EC-3) ✅
- Files: `apps/cmp/feature/onboarding/ui/src/commonMain/kotlin/com/banhmilabs/tmdb/apps/cmp/feature/onboarding/ui/`
  `OnboardingUiState.kt`, `OnboardingViewModel.kt`; `.../di/OnboardingUiModule.kt`
- What:
  - `internal enum class OnboardingUiState { Content, Navigating, Finished }`
  - `internal class OnboardingViewModel(private val settingsRepository: SettingsRepository) : ViewModel()`:
    - `val uiState: StateFlow<OnboardingUiState>`
    - `fun onGetStartedClick()`
  - `val onboardingUiModule = module { viewModelOf(::OnboardingViewModel) }`
- Pseudo-code:
  ```
  onGetStartedClick():
    if (_uiState.value != Content) return          // EC-3: only the first tap counts
    _uiState.value = Navigating
    viewModelScope.launch {
      settingsRepository.completeOnboarding()      // result ignored: EC-1 enters Home anyway
      _uiState.value = Finished
    }
  ```
- Unit tests (`jvmTest`): `OnboardingViewModelTest : MainDispatcherTest(StandardTestDispatcher())`, with
  `FakeSettingsRepository` and Turbine:
  - `startsInContent`
  - `getStartedCompletesOnboardingThenFinishes`: states Content → Navigating → Finished; the fake's
    `observeOnboardingCompleted()` is now `true`.
  - `getStartedFinishesWhenSaveFails`: `failNextComplete(AppError.Unknown)` → Finished; completed stays `false`.
  - `repeatedTapsFinishOnce`: two taps before `advanceUntilIdle()` → states Content, Navigating, Finished, with no
    further emissions.
- Compile: `./gradlew :apps:cmp:feature:onboarding:ui:compileKotlinJvm :apps:cmp:feature:onboarding:ui:compileTestKotlinJvm`

### Step 4: OnboardingScreen and its navigation entry  (AC-1, AC-2, AC-5, AC-6, EC-3, EC-6) ✅
- Files: `OnboardingScreen.kt`, `OnboardingRoute.kt`, `OnboardingNavigation.kt` (same package)
- What:
  - `@Serializable data object OnboardingRoute` (public: the shell uses it).
  - `fun NavGraphBuilder.onboardingScreen(onFinished: () -> Unit)`:
    - `composable<OnboardingRoute>` gets `koinViewModel<OnboardingViewModel>()` and collects `uiState` with
      `collectAsStateWithLifecycle()`.
    - `LaunchedEffect(state) { if (state == Finished) onFinished() }`.
    - It calls `OnboardingScreen`.
  - `@Composable internal fun OnboardingScreen(isNavigating: Boolean, onGetStartedClick: () -> Unit, modifier: Modifier = Modifier)`
    stacks, in a full-screen `Box`:
    1. Artwork placeholder: `Brush.verticalGradient(0f to #303243, 0.56527f to #3D458F)`.
    2. Scrim: `verticalGradient(0f to Transparent, 0.3089f to Transparent, 0.62685f to #D915151D, 1f to #D915151D)`.
    3. Content `Column`, aligned bottom-start:
       - Modifiers: `fillMaxWidth()`, `windowInsetsPadding(WindowInsets.navigationBars)`,
         `padding(start = 29.dp, end = 31.dp, bottom = 31.dp)`, `verticalArrangement = spacedBy(19.dp)`.
       - Logo: `Image(painterResource(Res.drawable.tmdb_logo), contentDescription = null)`.
       - Headline: `AnnotatedString.fromHtml(stringResource(onboarding_headline))`, 32sp, line height 37.3sp,
         `FontWeight.Light`, white.
       - Body: 15sp, line height 18.75sp, Normal, white.
       - Private `GetStartedButton(enabled = !isNavigating, onClick)`:
         - Size: `fillMaxWidth()`, `height(43.dp)`, `clip(CircleShape)`.
         - Fill: `Brush.linearGradient(primary, primaryContainer)`, top-start → bottom-end.
         - `clickable(enabled, role = Role.Button)` with the theme's ripple; `padding(12.dp)`.
         - Label: 16sp, SemiBold, white, single line, centred.
    - Test tags: `onboarding_get_started`.
- Unit tests (`jvmTest`), `OnboardingScreenTest` with `runComposeUiTest`:
  - `showsLogoHeadlineBodyAndButtonInEnglish`: `Locale.setDefault(Locale.US)`; the English texts are displayed.
  - `showsPortugueseCopyForPtBr`: `Locale.setDefault(Locale.of("pt", "BR"))`; "Acessar" and the pt-BR body.
  - `fallsBackToEnglishForOtherLanguages`: `Locale.FRANCE` → "Get started".
  - `clickCallsOnGetStartedClick`, `clickIgnoredWhileNavigating`.
  - Restore the default `Locale` in `@AfterTest`.
- Compile: `./gradlew :apps:cmp:feature:onboarding:ui:compileKotlinJvm :apps:cmp:feature:onboarding:ui:compileTestKotlinJvm`

### Step 5: Start destination and graph in composeApp  (AC-1, AC-2, AC-3, AC-4, EC-2) ✅
- Files: `apps/cmp/composeApp/src/commonMain/kotlin/com/banhmilabs/tmdb/apps/cmp/composeApp/`
  `StartDestination.kt`, `AppViewModel.kt`, `AppNavHost.kt`, `di/CmpAppModule.kt` (new); `App.kt` (modify);
  `build.gradle.kts` (modify)
- What:
  - `build.gradle.kts`:
    - `commonMain`: `projects.apps.cmp.feature.onboarding.ui`, `projects.service.settings.domain`,
      `libs.androidx.lifecycle.viewmodel.compose`, `libs.androidx.lifecycle.runtime.compose`,
      `libs.koin.compose.viewmodel`.
    - `jvmTest`: `projects.service.settings.fake`, `projects.core.testing`, `libs.turbine`.
  - `enum class StartDestination { Onboarding, Home }`.
  - `class AppViewModel(settingsRepository: SettingsRepository) : ViewModel()` (public: `MainActivity` reads it):
    `val startDestination: StateFlow<StartDestination?> = flow { emit(if (repo.observeOnboardingCompleted().first())
    Home else Onboarding) }.stateIn(viewModelScope, SharingStarted.Eagerly, null)`.
  - `@Composable internal fun AppNavHost(startDestination: StartDestination, navController: NavHostController = rememberNavController())`:
    - Start route: `OnboardingRoute` or `HomeRoute`.
    - `onboardingScreen(onFinished = { navController.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } } })`.
    - `composable<HomeRoute> { PlaceholderHomeScreen() }`.
  - `@Composable fun App(viewModel: AppViewModel = koinViewModel())`: `TmdbTheme`, then a full-size `Box` with the
    background colour. Inside it, `startDestination?.let { AppNavHost(it) }`, so nothing is drawn until the state is
    known (AC-3).
  - `val cmpAppModule = module { includes(onboardingUiModule); viewModelOf(::AppViewModel) }`.
- Unit tests (`jvmTest`):
  - `AppViewModelTest : MainDispatcherTest()`: `startsOnOnboardingWhenNotCompleted`, `startsOnHomeWhenCompleted`.
  - `AppNavHostTest` (`runComposeUiTest`; Koin started in `@BeforeTest` with `FakeSettingsRepository` and
    `onboardingUiModule`, `stopKoin()` in `@AfterTest`):
    - `onboardingStartShowsOnboarding`
    - `homeStartShowsHomeWithoutOnboarding` (AC-3)
    - `getStartedOpensHomeAndRemovesOnboardingFromBackStack`: click the button; "Home" is displayed,
      `navController.previousBackStackEntry == null`, and the fake reports completed.
- Compile: `./gradlew :apps:cmp:composeApp:compileKotlinJvm :apps:cmp:composeApp:compileTestKotlinJvm`

### Step 6: Android splash and Koin; iOS launch screen and Koin  (AC-3, AC-5) ✅
- Files: `apps/cmp/androidApp/build.gradle.kts`, `AndroidManifest.xml`, `MainActivity.kt`, `TmdbApplication.kt`,
  `src/main/res/values/themes.xml` (new), `src/main/res/values/colors.xml` (new);
  `apps/cmp/composeApp/src/iosMain/.../SharedCore.kt`; `apps/cmp/iosApp/iosApp/Info.plist`,
  `apps/cmp/iosApp/iosApp/Assets.xcassets/{Contents.json, LaunchBackground.colorset/Contents.json}` (new),
  `apps/cmp/iosApp/iosApp.xcodeproj/project.pbxproj`
- What:
  - Android:
    - `implementation(libs.androidx.core.splashscreen)`.
    - `colors.xml`: `tmdb_night` = `#15151D`.
    - `themes.xml`: `Theme.Tmdb.Starting`, parent `Theme.SplashScreen`, with
      `windowSplashScreenBackground` = `@color/tmdb_night`,
      `windowSplashScreenAnimatedIcon` = `@android:color/transparent`, and
      `postSplashScreenTheme` = `@android:style/Theme.Material.NoActionBar`.
    - Manifest: the application theme becomes `@style/Theme.Tmdb.Starting`.
    - `MainActivity`:
      - `private val appViewModel: AppViewModel by viewModel()` (koin-android).
      - `installSplashScreen().setKeepOnScreenCondition { appViewModel.startDestination.value == null }` before
        `super.onCreate`.
      - Keep the existing `enableEdgeToEdge(...)` and call `setContent { App(appViewModel) }`.
    - `TmdbApplication`: `initKoin { androidContext(...); modules(cmpAppModule) }`.
  - iOS:
    - `setUpSharedCore()`: `initKoin { modules(cmpAppModule) }`.
    - `Info.plist`: `UILaunchScreen` → `UIColorName` = `LaunchBackground`; `CFBundleLocalizations` = `en`, `pt-BR`.
    - Asset catalog with the `LaunchBackground` colour set (`#15151D`), added to the project's file references,
      group and Resources build phase (new `A100…` IDs, the same scheme as plan 03).
- Unit tests: none (platform wiring); covered by the manual checks.
- Compile: `./gradlew :apps:cmp:androidApp:assembleDebug :apps:cmp:composeApp:compileKotlinIosSimulatorArm64`, then
  `xcodebuild -project apps/cmp/iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`

### Step 7: Docs ✅
- Files: `CLAUDE.md`, `docs/features/onboarding/subtasks/04-cmp-onboarding-happy-path.md`
- What:
  - `CLAUDE.md` "Current state": the onboarding feature exists in `apps/cmp`.
  - `CLAUDE.md` "Reuse": `MainDispatcherTest` (`core:testing`) for ViewModel tests; drop `core:testing` from the
    "planned" note.
  - Subtask status as set by forge.
- Unit tests: none.
- Compile: none (docs only).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-1 first launch shows onboarding | 4, 5 | `AppViewModelTest.startsOnOnboardingWhenNotCompleted`, `AppNavHostTest.onboardingStartShowsOnboarding`, `OnboardingScreenTest.showsLogoHeadlineBodyAndButtonInEnglish`; manual 1 |
| AC-2 button enters the app | 3, 4, 5 | `OnboardingViewModelTest.getStartedCompletesOnboardingThenFinishes`, `OnboardingScreenTest.clickCallsOnGetStartedClick`, `AppNavHostTest.getStartedOpensHomeAndRemovesOnboardingFromBackStack`; manual 2 |
| AC-3 skipped after completion, no flash | 5, 6 | `AppViewModelTest.startsOnHomeWhenCompleted`, `AppNavHostTest.homeStartShowsHomeWithoutOnboarding`; manual 4 (splash / launch screen, Android + iOS) |
| AC-4 not in back stack | 5 | `AppNavHostTest.getStartedOpensHomeAndRemovesOnboardingFromBackStack`; manual 3 (system Back) |
| AC-5 localized copy | 2, 4, 6 | `OnboardingScreenTest.showsPortugueseCopyForPtBr`; manual 6 (device language, Android + iOS) |
| AC-6 offline | 2, 4 | No network code or resources in the feature (review); manual 7 |
| EC-1 save fails | 3 | `OnboardingViewModelTest.getStartedFinishesWhenSaveFails`; manual 8 |
| EC-2 killed before tap | 5 | `AppViewModelTest.startsOnOnboardingWhenNotCompleted` (state only changes on tap); manual 5 |
| EC-3 repeated taps | 3, 4 | `OnboardingViewModelTest.repeatedTapsFinishOnce`, `OnboardingScreenTest.clickIgnoredWhileNavigating`; manual 2 |
| EC-6 other language → English | 2, 4 | `OnboardingScreenTest.fallsBackToEnglishForOtherLanguages`; manual 6 |

## Out of scope
- Accessibility (decorative artwork, merged headline semantics, 48dp touch target), landscape / tablet layout, large
  fonts with scrolling, rotation and process death → 05-cmp-onboarding-robustness.
- Inter font and typography tokens → when the design system grows (plan 03 decision).
- Licensed artwork and its backdrop blur → when the artwork is supplied.
- App icon → separate task.
- The real Home screen → Home feature.

## Risks
- **Compose resources on iOS** with a static framework and `embedAndSignAppleFrameworkForXcode`: the resources must
  be copied into the app bundle. Mitigation: step 6's `xcodebuild` and a simulator run show the logo and strings. If
  they are missing, add the Compose plugin's iOS resource sync to the Run Script (record it as a deviation).
- **`androidResources` on the AGP KMP library plugin**: the exact DSL may differ in AGP 9.5. Mitigation: step 1
  compiles, and step 6's `assembleDebug` checks that the APK contains the strings.
- **Vector gradient in Compose resources**: `aapt:attr` gradients in common vector XML may not be supported on every
  target. Mitigation: if not, split the gradient into the vector's fill with `Brush` in code, or use a PNG/WebP at 3x
  (deviation).
- **Locale in JVM UI tests**: Compose resources on JVM read `Locale.getDefault()`. If they cache it per process, run
  the pt-BR test in its own class, or verify pt-BR manually only (deviation).
- **Lifecycle 2.11.0 vs Navigation 2.10.0**: transitive versions must agree. Mitigation: step 1 / 5 compile;
  otherwise align to the version navigation pulls in.
- **Design fidelity**: the logo is TMDB's gradient logo, not the design's white wordmark (user decision). The
  headline's Medium connectors are Light (user decision).

## Deviations
- `core:testing` also adds `kotlin-test-junit` to `jvmMain` and `androidMain`. `MainDispatcherTest` uses
  `@BeforeTest` / `@AfterTest` in main code, and `kotlin-test` picks a JUnit variant only for test compilations. This
  is the same fix as `service/settings/fake`.
- The headline uses `boldMarkupToAnnotatedString` (`BoldMarkup.kt`, tested in `BoldMarkupTest`) instead of
  `AnnotatedString.fromHtml`. Compose Multiplatform 1.12 ships `fromHtml` on Android only, not on JVM or iOS. The
  strings keep the planned `<b>…</b>` markup.
- The logo `Image` has a test tag (`onboarding_logo`) so `OnboardingScreenTest` can check that it is displayed.
- `AppNavHostTest` repeats the button's test tag string (`"onboarding_get_started"`), because `GET_STARTED_TAG` is
  `internal` to the feature module.
- `AppNavHostTest` also extends `MainDispatcherTest`: `kotlinx-coroutines-test` on the JVM test classpath replaces
  `Dispatchers.Main`, which the NavHost's ViewModels and `collectAsStateWithLifecycle` use.
- After review: `startDestination` is non-null, `StateFlow<StartDestination>` with a `Loading` default, instead of
  `null` (from user). `App()` draws nothing for `Loading` and passes the route to `AppNavHost(startRoute)`;
  `MainActivity` keeps the splash while it is `Loading`. `AppViewModelTest` gained
  `loadingUntilOnboardingStateIsRead`.
- After review: onboarding follows MVI (from user).
  - `OnboardingUiState` is a data class (`isNavigating`).
  - `OnboardingIntent.GetStartedClicked` is handled by `onIntent`.
  - Navigation is a one-off `OnboardingEffect.NavigateToHome`, sent through a buffered `Channel` instead of a
    `Finished` state.
  - `OnboardingScreen` takes `(state, onIntent)`.
  - `onboardingScreen(onFinished)` is unchanged.
  - The convention is recorded in `docs/architecture.md` 11 and `CLAUDE.md`.
- After review: the ViewModel wiring moved from `onboardingScreen()` into a stateful
  `OnboardingScreen(onFinished, viewModel = koinViewModel())` (from user). The stateless UI is now
  `OnboardingContent(state, onIntent)`, and `OnboardingScreenTest` became `OnboardingContentTest`.
- After review: packages are organized by screen, not by layer (from user, following Now in Android and Kotlin
  conventions).
  - `navigation/` holds `OnboardingRoute` and `onboardingScreen`.
  - `di/` holds the Koin module.
  - The module root holds `OnboardingContract.kt` (state, intent, effect), `OnboardingViewModel.kt` and
    `OnboardingScreen.kt` (screen and content, with the design values private).
  - `component/` holds `GetStartedButton`.
  - Tests mirror these packages. The layout is recorded in `docs/architecture.md` 11.
- After review: the headline is no longer bold (from user). The `<b>` markup, `BoldMarkup.kt` and `BoldMarkupTest`
  are removed. The headline is one plain string in a single Light weight; `design/onboarding.md` records this.
