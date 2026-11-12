# Plan: Onboarding robustness (cmp)

Requirement: [05 - Onboarding robustness (cmp)](../subtasks/05-cmp-onboarding-robustness.md)
Status: Done

## Summary
This plan makes the onboarding screen from plan 04 robust.
- **Accessibility**: the artwork, scrim and logo are decorative, the headline is a heading, and the button is
  announced as a button.
- **Large fonts and long copy**: the button grows with its text, and the content scrolls when it doesn't fit.
- **Orientation and screen sizes**: the artwork is cover-cropped. The content stays bottom-anchored, centred, at
  most 315dp wide, and inside the safe area (status bar, navigation bar, display cutout).
- **Rotation and process death**: no saved state is needed. The only state, `isNavigating`, must not survive
  process death, and the navigation back stack is already restored by `NavHost`.

Everything is verified with Compose UI tests on JVM (`DeviceConfigurationOverride`, `StateRestorationTester`) and
with the manual TalkBack / VoiceOver checks from the subtask.

## Decisions
- **Button touch target**: minimum height 48dp (`heightIn(min = 48.dp)` instead of a fixed `height(43.dp)`) (from
  user). The visible button and the touch area match, and the button grows with large text. This is 5dp taller
  than the design. Rejected: keeping 43dp visible with a 48dp touch area (`minimumInteractiveComponentSize`).
- **Button label wraps**: drop `maxLines = 1` so a long translation or a 200% font wraps instead of being cut off
  (AC-7 "without clipping").
- **Artwork uses `ContentScale.Crop` with `Alignment.TopCenter`**, as in the design (centred horizontally,
  top-aligned, bottom cropped). AC-8 requires a crop, not a stretch, and the licensed artwork will need it. **The
  scrim keeps `ContentScale.FillBounds`**: it is an overlay whose stops are fractions of the screen height, so it
  must stretch to keep the text side dark in every orientation.
- **Scrollable bottom-anchored content**:
  - Layout: `BoxWithConstraints` → `Column(verticalScroll, then heightIn(min = maxHeight))` with
    `Arrangement.Bottom`; the 19dp gap is inside the content block.
  - The content sits at the bottom when it fits and scrolls when it doesn't (EC-5).
  - Rejected: a `weight` spacer, which can't be used inside `verticalScroll`.
- **Insets**:
  - `WindowInsets.safeDrawing` replaces `navigationBars`, so the content also avoids the status bar when it
    scrolls up and the display cutout in landscape.
  - The 31dp bottom margin stays on top of the inset (design "System bars").
  - The artwork and scrim stay full-bleed.
- **Max width 315dp, centred**:
  - The outer column keeps the design padding (29dp start, 31dp end, 31dp bottom).
  - An inner column has `widthIn(max = 315.dp)` and is centred horizontally.
  - On a 375dp phone this gives the same 315dp width as today.
- **Headline is a heading** (`semantics { heading() }`), so screen readers can jump to it. It is now one plain
  string, so it is read as one sentence. This closes the design gap "screen-reader text for the headline".
- **Decorative images stay `contentDescription = null`** (artwork, scrim, logo). Compose adds no accessibility
  node for them, on Android and iOS alike. The logo's test tag stays for display checks.
- **No `SavedStateHandle` for `isNavigating`**: the save coroutine does not survive process death, so a restored
  `isNavigating = true` would leave the button disabled forever. After process death, onboarding comes back in
  `Content` and the user taps again. A completed save is already persisted (EC-4: "no state is lost").
- **Rotation needs no code**: `OnboardingViewModel` and `AppViewModel` survive configuration changes, and
  `NavHost` saves its back stack. A test proves it (step 3).
- **iPad orientations**: the project already builds for iPad (`TARGETED_DEVICE_FAMILY = "1,2"`). Add
  `UISupportedInterfaceOrientations~ipad` with all four orientations, which iPad multitasking expects (AC-8
  tablet).
- **Contrast is already met**, so no change:
  - White label on the button gradient (`#8000FF` → `#540BA1`): about 6.3:1 at its lightest end, above the 4.5:1
    AA threshold.
  - White text on the scrim (`#15151D` at 85%) is higher.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Onboarding UI | `OnboardingContent`, `GetStartedButton` | `apps/cmp/feature/onboarding/ui/.../OnboardingScreen.kt`, `component/GetStartedButton.kt` |
| Artwork and scrim | `onboarding_artwork_placeholder.xml`, `onboarding_scrim.xml` | `.../composeResources/drawable/` |
| App graph | `AppNavHost(startRoute, navController)` | `apps/cmp/composeApp/.../AppNavHost.kt` |
| Test Main dispatcher | `MainDispatcherTest` | `core:testing` |
| Repository fake | `FakeSettingsRepository` | `service/settings/fake` |
| Font scale and screen size in tests | `DeviceConfigurationOverride.FontScale` / `ForcedSize` | `org.jetbrains.compose.ui:ui-test` (already in `jvmTest`) |
| Recreation and process death in tests | `StateRestorationTester` | `org.jetbrains.compose.ui:ui-test` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| `apps/cmp/feature/onboarding/ui` | `component/GetStartedButton.kt`, `OnboardingScreen.kt` (modify); `OnboardingContentTest.kt` (modify) | Touch target, wrapping, scrolling, insets, max width, crop, semantics |
| `apps/cmp/composeApp` | `AppNavHostTest.kt` (modify) | Recreation and process death tests |
| `apps/cmp/iosApp` | `iosApp/Info.plist` (modify) | iPad orientations |
| docs | `design/onboarding.md`, `subtasks/05-…` (modify) | Close the headline gap; record the 48dp button; status |

## Steps
### Step 1: Button meets the touch target and grows with its text  (AC-7, EC-5) ✅
- Files: `apps/cmp/feature/onboarding/ui/src/commonMain/.../component/GetStartedButton.kt`
- What:
  - `ButtonHeight = 43.dp` becomes `ButtonMinHeight = 48.dp`, used through `heightIn(min = ButtonMinHeight)`.
  - Remove `maxLines = 1` from the label.
  - Role, click handling, gradient, pill shape and 12dp padding are unchanged.
- Unit tests (`OnboardingContentTest`):
  - `buttonIsAnnouncedAsButtonWithLabel`: role `Button`, has a click action, text "Get started".
  - `buttonMeetsMinimumTouchTarget`: `assertHeightIsAtLeast(48.dp)`.
- Compile: `./gradlew :apps:cmp:feature:onboarding:ui:compileKotlinJvm :apps:cmp:feature:onboarding:ui:compileTestKotlinJvm`

### Step 2: Adaptive, scrollable, accessible content  (AC-7, AC-8, EC-5) ✅
- Files: `apps/cmp/feature/onboarding/ui/src/commonMain/.../OnboardingScreen.kt`
- What:
  - Artwork `Image`: `contentScale = ContentScale.Crop`, `alignment = Alignment.TopCenter`. Scrim unchanged
    (`FillBounds`).
  - Replace the bottom-aligned `Column` with this structure:
    ```
    BoxWithConstraints(fillMaxSize) {
      Column(                                   // scroll container, full height
        Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .heightIn(min = maxHeight)            // after verticalScroll: applies to the scrolled content
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(start = 29.dp, end = 31.dp, bottom = 31.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Column(                                 // content block
          Modifier.widthIn(max = 315.dp).fillMaxWidth().testTag(CONTENT_TAG),
          verticalArrangement = Arrangement.spacedBy(19.dp),
        ) { logo, headline (semantics { heading() }), body, GetStartedButton }
      }
    }
    ```
  - `internal const val CONTENT_TAG = "onboarding_content"`.
- Unit tests (`OnboardingContentTest`):
  - `decorativeImagesAreNotAnnounced`: no node has a `ContentDescription`, and the logo node has none.
  - `headlineIsAHeading`.
  - `largestFontKeepsTextAndButtonReachable`:
    - Setup: `DeviceConfigurationOverride.FontScale(2f) then ForcedSize(DpSize(360.dp, 640.dp))`.
    - Checks: after `performScrollTo()`, the headline, body and button are each displayed.
  - `landscapePhoneKeepsButtonReachable`: `ForcedSize(DpSize(800.dp, 360.dp))`; the button is displayed after
    `performScrollTo()`.
  - `tabletCentresContentAtMaxWidth`:
    - Setup: `ForcedSize(DpSize(1280.dp, 800.dp))`.
    - Checks: the content node is 315dp wide (±1dp), and its centre is the screen's centre shifted by the
      asymmetric padding, (29 − 31) / 2 = −1dp (±1dp).
  - `portraitPhoneKeepsDesignWidth`: `ForcedSize(DpSize(375.dp, 812.dp))`; the content is 315dp wide.
  - Existing tests are unchanged.
- Compile: `./gradlew :apps:cmp:feature:onboarding:ui:compileKotlinJvm :apps:cmp:feature:onboarding:ui:compileTestKotlinJvm`

### Step 3: Recreation and process death are covered  (EC-4) ✅
- Files: `apps/cmp/composeApp/src/jvmTest/.../AppNavHostTest.kt`
- What: no production change (see Decisions). Add tests with
  `StateRestorationTester(this).setContent { TmdbTheme { AppNavHost(...) } }`:
  - `onboardingIsRestoredAfterRecreation`: start on onboarding, call `emulateSavedInstanceStateRestore()`;
    onboarding is shown and the button is enabled.
  - `homeIsRestoredAfterRecreationWithoutOnboardingBehind`: tap the button, then emulate a restore. Home is
    shown, `previousBackStackEntry == null`, and onboarding is absent.
- Compile: `./gradlew :apps:cmp:composeApp:compileTestKotlinJvm`

### Step 4: iPad orientations  (AC-8) ✅
- Files: `apps/cmp/iosApp/iosApp/Info.plist`
- What: add `UISupportedInterfaceOrientations~ipad` with Portrait, PortraitUpsideDown, LandscapeLeft and
  LandscapeRight. The iPhone list is unchanged.
- Unit tests: none (platform config); manual check 3 on an iPad simulator.
- Compile: `xcodebuild -project apps/cmp/iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`

### Step 5: Docs ✅
- Files: `docs/features/onboarding/design/onboarding.md`, `docs/features/onboarding/subtasks/05-cmp-onboarding-robustness.md`
- What:
  - Design "Gaps": tick the headline screen-reader gap. The headline is one plain string marked as a heading.
  - Design "Components": the primary button note becomes "min height 48dp for the touch target (from user)".
  - Design change log entry.
  - Subtask status as set by forge.
- Unit tests: none.
- Compile: none (docs only).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-7 screen reader: decorative images skipped | 2 | `OnboardingContentTest.decorativeImagesAreNotAnnounced`; manual 1 (TalkBack, VoiceOver) |
| AC-7 headline and body read | 2 | `OnboardingContentTest.headlineIsAHeading`, existing text tests; manual 1 |
| AC-7 button announced with label | 1 | `OnboardingContentTest.buttonIsAnnouncedAsButtonWithLabel`; manual 1 |
| AC-7 largest font, no clipping | 1, 2 | `OnboardingContentTest.largestFontKeepsTextAndButtonReachable`; manual 2 |
| AC-7 touch target and contrast | 1 | `OnboardingContentTest.buttonMeetsMinimumTouchTarget`; contrast computed in Decisions |
| AC-8 artwork cover crop | 2 | Manual 3 (portrait, landscape, tablet); crop is set in code |
| AC-8 text and button reachable on any size | 2, 4 | `landscapePhoneKeepsButtonReachable`, `tabletCentresContentAtMaxWidth`, `portraitPhoneKeepsDesignWidth`; manual 3 |
| EC-4 rotation and process death | 3 | `AppNavHostTest.onboardingIsRestoredAfterRecreation`, `homeIsRestoredAfterRecreationWithoutOnboardingBehind`; manual 4 |
| EC-5 long copy or large font scrolls | 1, 2 | `largestFontKeepsTextAndButtonReachable`; manual 2 |

## Out of scope
- Licensed artwork and its backdrop blur → when the artwork is supplied.
- Inter font and typography tokens → when the design system grows.
- Native apps → subtasks 06 and 07.

## Risks
- **`DeviceConfigurationOverride` / `StateRestorationTester` on desktop**: both ship in `ui-test` 1.12.2 for
  desktop, but their behaviour there is less proven than on Android. Mitigation: if a test cannot express the
  check, keep the manual step and record a deviation; never weaken the assertion.
- **`safeDrawing` on iOS**: it includes the home indicator and, in landscape, the notch side. Mitigation: manual
  check 3 on an iPhone with a notch in landscape.
- **Crop of the placeholder in landscape**: a top-aligned crop of the portrait gradient shows mostly its top
  colour. That is expected for a placeholder; the licensed artwork is chosen with this crop in mind.
- **iOS state restoration**: Compose on iOS does not restore `rememberSaveable` after the system kills the app, so
  a relaunch starts fresh. Onboarding then shows again unless it was completed, which is what EC-4 needs for this
  stateless screen.

## Deviations
- The risk "`DeviceConfigurationOverride` / `StateRestorationTester` on desktop" happened. In `ui-test` 1.12.2
  on JVM, `DeviceConfigurationOverride.FontScale` and `ForcedSize` throw "isn't supported", and
  `StateRestorationTester` throws "Not yet implemented". The tests use equivalents that run on JVM, with the same
  assertions:
  - **Font scale**: `CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f))`.
  - **Screen size**: the content is wrapped in `Box(Modifier.requiredSize(...))`. The tablet centre is measured
    relative to that box, because a 1280dp box overflows the 1024dp test window.
  - **Recreation**: a private `setRecreatableContent` in `AppNavHostTest` saves through a `SaveableStateRegistry`,
    removes the content, and recomposes it at the same position with the saved state and a new `ViewModelStore`.
    This emulates process death, the stricter case; rotation keeps the `ViewModelStore`. When the content was
    recreated under a new `key`, the Home test failed because nothing was restored, so the test does catch a
    broken restore.
- After review: the Compose UI tests use `androidx.compose.ui.test.v2.runComposeUiTest` instead of the deprecated
  v1 (from user). v2 queues coroutines on a `StandardTestDispatcher`; all tests pass unchanged.
  `docs/architecture.md` 8 and `.claude/rules/build-logic.md` name the v2 API.
