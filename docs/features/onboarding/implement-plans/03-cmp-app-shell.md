# Plan: App shell (cmp)

Requirement: [03 - App shell (cmp)](../subtasks/03-cmp-app-shell.md)
Status: Done

## Summary
This plan creates the first app, `apps/cmp`.
- `tmdb.module` learns two app-shell module types from their paths (`:apps:<app>:composeApp`, a KMP library that
  also builds the iOS framework, and `:apps:<app>:androidApp`, an Android application) and adds Compose
  Multiplatform to every `apps/cmp` UI module. Each module still applies only `tmdb.module`.
- `apps/cmp/designsystem` holds the dark theme.
- `composeApp` holds `App()` with a type-safe navigation graph whose start destination is a placeholder Home.
- `androidApp` and a hand-written Xcode project (`iosApp`) start the shared core (`initKoin`) and host `App()`.
- There are no feature modules yet; onboarding arrives in 04.

## Decisions
- App shells are new module types recognized by path and configured by `tmdb.module` (from user; rejected:
  separate convention plugins applied per module):
  - `:apps:<app>:composeApp` → `ModuleKind.APP`
  - `:apps:<app>:androidApp` → `ModuleKind.ANDROID_APP`

  Both are "root" modules (architecture 5.2): they may depend on anything in their own app and in the shared
  core; `fake` / `core:testing` only in test source sets. Architecture 5.2 / 9 and `.claude/rules/build-logic.md`
  are updated first, as the rule file requires for architecture changes.
- Compose Multiplatform (`org.jetbrains.compose` 1.12.2 + the Kotlin Compose compiler plugin) is applied by
  `tmdb.module` to `apps/cmp` modules of kind `DESIGNSYSTEM`, `UI` and `APP`. The convention adds runtime,
  foundation, ui and material3 to their `commonMain`. `apps/native` modules stay unchanged until that app is
  built.
- The theme lives in `apps/cmp/designsystem` now: `TmdbTheme` with a Material 3 dark color scheme from the
  design. — Architecture 5.2; both the shell's placeholder Home and onboarding (04) use it. (from user; rejected:
  theme in `composeApp` first.)
- Typography uses the platform default font; sizes and weights come from the design when a screen needs them
  (04). (from user; rejected: bundling Inter now.)
- iOS host: a hand-written minimal `apps/cmp/iosApp/iosApp.xcodeproj` with a SwiftUI entry. A Run Script phase
  calls `./gradlew :apps:cmp:composeApp:embedAndSignAppleFrameworkForXcode`; the static framework is
  `ComposeApp`. Verified with `xcodebuild`. (from user; rejected: XcodeGen, or a project created by hand in
  Xcode.)
- Placeholder Home is a composable in `composeApp` (`PlaceholderHomeScreen`, route `HomeRoute`). — It is
  temporary until the Home feature exists, and architecture 5.5 says not to create a feature module before it
  is needed. Its "Home" label is not localized because it will be replaced.
- Start-up:
  - Android: `TmdbApplication.onCreate` calls `initKoin { androidContext(this) }`; `MainActivity` calls
    `enableEdgeToEdge()` and `setContent { App() }`.
  - iOS: `iOSApp.init` calls `SharedCoreKt.setUpSharedCore()` (iosMain wrapper around `initKoin()`);
    `ContentView` hosts `MainViewController()`.
- Identifiers:
  - Android `applicationId` is derived from the path: `com.banhmilabs.tmdb.<app>`, here `com.banhmilabs.tmdb.cmp`.
  - The iOS bundle id is `com.banhmilabs.tmdb.cmp`.
  - `versionCode 1` / `versionName "0.1.0"` are set in the convention as constants.
  - `targetSdk` comes from a new catalog entry `android-targetSdk = "36"`.
- Detekt: `FunctionNaming` ignores `@Composable` functions (`config/detekt/detekt.yml`), the standard Compose
  setting.
- No Compose UI tests in this subtask. — There is no feature behavior yet; Compose UI test setup
  (`runComposeUiTest` on JVM) comes with the first screen that has behavior (04). The shell is checked by the
  build-logic unit tests, Konsist, `assemble`, `xcodebuild` and a manual launch.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Module type from path, dependency rules | `moduleTypeOf`, `DependencyRules`, `ModuleConventionPlugin` | `build-logic/convention/src/main/kotlin/…` |
| KMP target setup | `configureKotlinMultiplatform()` | `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/KotlinMultiplatform.kt` |
| Rule tests to extend | `ModuleTypeTest`, `DependencyRulesTest` | `build-logic/convention/src/test/kotlin/com/banhmilabs/tmdb/buildlogic/` |
| Package-path rule | `ModulePaths.kt` (`MODULE_DIR`, `packageOf`) | `architecture-test/src/test/kotlin/com/banhmilabs/tmdb/architecture/ModulePaths.kt` |
| Start the shared core | `initKoin(appDeclaration)` | `core/di/src/commonMain/kotlin/com/banhmilabs/tmdb/core/di/InitKoin.kt` |
| Design colors | `design/onboarding.md` (scrim `#15151D`, purple `#8000FF` → `#540BA1`) | `docs/features/onboarding/design/onboarding.md` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| docs | `docs/architecture.md` 5.2 / 9, `.claude/rules/build-logic.md`, `CLAUDE.md` | Record the app-shell types and how to run the app |
| root | `gradle/libs.versions.toml`, `build.gradle.kts`, `settings.gradle.kts`, `config/detekt/detekt.yml`, `.gitignore` | Compose / Android app plugins and libraries, new modules, Compose naming |
| build-logic | `build.gradle.kts`, `ModuleType.kt`, `DependencyRules.kt`, `ModuleConventionPlugin.kt`, `Compose.kt` (new), `AndroidApplication.kt` (new), tests | App-shell types and setup |
| architecture-test | `ModulePaths.kt` | Package rule covers the shell modules |
| `apps:cmp:designsystem` (new) | `build.gradle.kts`, `Theme.kt` | Dark theme |
| `apps:cmp:composeApp` (new) | `build.gradle.kts`, `App.kt`, `HomeRoute.kt`, `PlaceholderHomeScreen.kt`, iOS `MainViewController.kt`, `SharedCore.kt` | Shared app entry + navigation |
| `apps:cmp:androidApp` (new) | `build.gradle.kts`, `AndroidManifest.xml`, `TmdbApplication.kt`, `MainActivity.kt` | Android app |
| `apps/cmp/iosApp` (new, Xcode) | `iosApp.xcodeproj/project.pbxproj`, `iosApp/iOSApp.swift`, `iosApp/ContentView.swift`, `iosApp/Info.plist` | iOS host |

## Steps

### Step 1: Docs first: app-shell module types  (setup) ✅
- Files: `docs/architecture.md`, `.claude/rules/build-logic.md` (modify)
- What:
  - Architecture 5.2 table: add rows `apps/<app>/composeApp` → `app` (KMP library: `App()`, navigation graph,
    iOS framework; root) and `apps/<app>/androidApp` → `android-app` (Android application; root).
  - Architecture 9: `moduleTypeOf` also recognizes `:apps:<app>:composeApp` and `:apps:<app>:androidApp`.
    Replace the "Status" note: `apps/cmp` shells and Compose setup exist; `apps/native` (`app`, `shared`) is added
    with that app.
  - `build-logic.md` "Module types and dependency rules": the new kinds, Compose applied to `apps/cmp` `designsystem`
    / `ui` / `app`, `androidApp` configured as an Android application.
- Unit tests: none.
- Compile: none.

### Step 2: Catalog, plugins and Detekt config  (setup) ✅
- Files: `gradle/libs.versions.toml`, `build.gradle.kts`, `build-logic/convention/build.gradle.kts`,
  `config/detekt/detekt.yml` (modify)
- What:
  - `[versions]`: `compose-multiplatform = "1.12.2"`, `compose-material3 = "1.9.0"`,
    `androidx-navigation = "2.10.0"` (JetBrains KMP navigation), `androidx-activity = "1.13.0"`,
    `android-targetSdk = "36"`.
  - `[libraries]`:
    - `compose-runtime` (`org.jetbrains.compose.runtime:runtime`)
    - `compose-foundation` (`org.jetbrains.compose.foundation:foundation`)
    - `compose-ui` (`org.jetbrains.compose.ui:ui`)
    - `compose-material3` (`org.jetbrains.compose.material3:material3`, `compose-material3`)
    - `androidx-navigation-compose` (`org.jetbrains.androidx.navigation:navigation-compose`)
    - `androidx-activity-compose` (`androidx.activity:activity-compose`)
    - `kotlinx-serialization-core`
    - `compose-gradlePlugin` (`org.jetbrains.compose:compose-gradle-plugin`)
    - `compose-compiler-gradlePlugin` (`org.jetbrains.kotlin:compose-compiler-gradle-plugin`, `kotlin`)
  - `[plugins]`: `compose-multiplatform` (`org.jetbrains.compose`), `kotlin-compose`
    (`org.jetbrains.kotlin.plugin.compose`, `kotlin`), `android-application` (`com.android.application`, `agp`).
  - Root `build.gradle.kts`: the three plugins `apply false`.
  - `build-logic/convention/build.gradle.kts`: `compileOnly(libs.compose.gradlePlugin)`,
    `compileOnly(libs.compose.compiler.gradlePlugin)`.
  - `detekt.yml`: `naming: FunctionNaming: ignoreAnnotated: ['Composable']`.
- Unit tests: none.
- Compile: `./gradlew help`

### Step 3: Module types and rules in build-logic  (setup) ✅
- Files: `buildlogic/ModuleType.kt`, `buildlogic/DependencyRules.kt` (modify); tests in
  `build-logic/convention/src/test/kotlin/com/banhmilabs/tmdb/buildlogic/` (modify)
- What:
  - `ModuleKind`: add `APP`, `ANDROID_APP`.
  - `moduleTypeOf`: `segments.size == 3 && segments[0] == "apps"`: `"composeApp"` → `ModuleType(APP, app = segments[1])`,
    `"androidApp"` → `ModuleType(ANDROID_APP, app = segments[1])` (next to `designsystem`).
  - `DependencyRules.violation`: `APP, ANDROID_APP -> true` (root of its app; `appViolation()` already blocks
    other apps). The test-only check treats them like every non-`DI` kind (fake / testing only in tests).
    `ruleFor`: `"an app shell may depend on its own app and the shared core"`.
- Unit tests:
  - `ModuleTypeTest`: `composeAppPathIsApp`, `androidAppPathIsAndroidApp`, `unknownAppModuleIsNull`
    (`:apps:cmp:desktopApp` → `null`).
  - `DependencyRulesTest`: `androidAppMayDependOnComposeApp`, `appShellMayDependOnSharedCoreAndOwnApp`,
    `appShellMayNotDependOnOtherApp`, `appShellUsesFakeOnlyInTests`.
- Compile: `./gradlew -p build-logic :convention:compileTestKotlin`

### Step 4: Configure the new types in `tmdb.module`  (setup) ✅
- Files: `ModuleConventionPlugin.kt` (modify), `buildlogic/Compose.kt` (new), `buildlogic/AndroidApplication.kt` (new),
  `buildlogic/KotlinMultiplatform.kt` (modify)
- What:
  - `ModuleConventionPlugin.apply`:
    ```
    when (type.kind) {
      ANDROID_APP -> configureAndroidApplication()            // not KMP
      else -> {
        configureKotlinMultiplatform(iosFramework = if (type.kind == APP) "ComposeApp" else null)
        if (type.app == "cmp" && type.kind in setOf(DESIGNSYSTEM, UI, APP)) configureComposeMultiplatform()
        if (type.kind == DOMAIN || type.kind == DATA) configureKover()
      }
    }
    enforceDependencyRules(path, type)
    ```
  - `configureKotlinMultiplatform(iosFramework: String? = null)`: when set, `iosArm64()` / `iosSimulatorArm64()`
    get `binaries.framework { baseName = iosFramework; isStatic = true }`.
  - `Compose.kt`: `internal fun Project.configureComposeMultiplatform()` applies `org.jetbrains.compose` and
    `org.jetbrains.kotlin.plugin.compose`, and adds `compose-runtime`, `compose-foundation`, `compose-ui` and
    `compose-material3` to `commonMain`.
  - `AndroidApplication.kt`: `internal fun Project.configureAndroidApplication()`.
    - Applies `com.android.application` (AGP 9 built-in Kotlin), `org.jetbrains.kotlin.plugin.compose`,
      `org.jetbrains.kotlin.plugin.serialization` and `tmdb.detekt`.
    - `ApplicationExtension`: `namespace = derivedNamespace()`,
      `applicationId = "com.banhmilabs.tmdb." + <app>`, `compileSdk`, `minSdk`, `targetSdk` from the catalog,
      `versionCode = VERSION_CODE`, `versionName = VERSION_NAME`, and `buildFeatures.compose = true` if AGP
      requires it.
    - Kotlin `jvmToolchain(libs.version("jvmToolchain"))`.
  - `enforceDependencyRules`: for `ANDROID_APP` (no `KotlinMultiplatformExtension`), check the project
    dependencies of `implementation`, `api`, `compileOnly`, `runtimeOnly` and every configuration whose name
    contains `Test` (test scope), with the same `DependencyRules.violation` call.
- Unit tests: none beyond step 3 (configuration is exercised by steps 5–7 and `assemble`).
- Compile: `./gradlew compileTestKotlinJvm`, then `./gradlew :core:common:tasks --all | grep -i compose` (expect
  no output: shared modules don't get Compose).

### Step 5: `apps/cmp/designsystem` theme  (setup) ✅
- Files (package `com.banhmilabs.tmdb.apps.cmp.designsystem`):
  - `settings.gradle.kts`: include `":apps:cmp:designsystem"`.
  - `apps/cmp/designsystem/build.gradle.kts` (new): `plugins { alias(libs.plugins.tmdb.module) }` only.
  - `src/commonMain/.../Theme.kt` (new):
    ```kotlin
    private val Purple = Color(0xFF8000FF)          // design: button gradient start
    private val DeepPurple = Color(0xFF540BA1)      // design: button gradient end
    private val Night = Color(0xFF15151D)           // design: scrim rgba(21, 21, 29)
    private val TmdbDarkColors = darkColorScheme(
        primary = Purple, onPrimary = Color.White, primaryContainer = DeepPurple, onPrimaryContainer = Color.White,
        background = Night, onBackground = Color.White, surface = Night, onSurface = Color.White,
    )

    /** The app's dark theme (architecture: dark theme, purple accents). */
    @Composable
    fun TmdbTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = TmdbDarkColors, content = content)
    ```
- Unit tests: none (no behavior).
- Compile: `./gradlew :apps:cmp:designsystem:compileKotlinJvm :apps:cmp:designsystem:compileAndroidMain :apps:cmp:designsystem:compileKotlinIosSimulatorArm64`

### Step 6: `apps/cmp/composeApp`  (setup) ✅
- Files (package `com.banhmilabs.tmdb.apps.cmp.composeApp`):
  - `settings.gradle.kts`: include `":apps:cmp:composeApp"`.
  - `apps/cmp/composeApp/build.gradle.kts` (new): `commonMain`: `implementation(projects.apps.cmp.designsystem)`,
    `implementation(projects.core.di)`, `implementation(libs.androidx.navigation.compose)`,
    `implementation(libs.kotlinx.serialization.core)`.
  - `src/commonMain/.../HomeRoute.kt` (new): `@Serializable data object HomeRoute`.
  - `src/commonMain/.../PlaceholderHomeScreen.kt` (new): `@Composable internal fun PlaceholderHomeScreen()`, a
    `Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Center) { Text("Home", color = onBackground) }`.
  - `src/commonMain/.../App.kt` (new):
    ```kotlin
    /** The cmp app: theme + navigation graph. Hosted by androidApp and iosApp. */
    @Composable
    fun App() {
        TmdbTheme {
            val navController = rememberNavController()
            NavHost(navController, startDestination = HomeRoute) {
                composable<HomeRoute> { PlaceholderHomeScreen() }
            }
        }
    }
    ```
  - `src/iosMain/.../MainViewController.kt` (new): `fun MainViewController(): UIViewController = ComposeUIViewController { App() }`.
  - `src/iosMain/.../SharedCore.kt` (new): `/** Called once from iosApp at launch. */ fun setUpSharedCore() { initKoin() }`.
- Unit tests: none (see Decisions).
- Compile: `./gradlew :apps:cmp:composeApp:compileKotlinJvm :apps:cmp:composeApp:compileAndroidMain :apps:cmp:composeApp:linkDebugFrameworkIosSimulatorArm64`

### Step 7: `apps/cmp/androidApp`  (setup) ✅
- Files (package `com.banhmilabs.tmdb.apps.cmp.androidApp`):
  - `settings.gradle.kts`: include `":apps:cmp:androidApp"`.
  - `apps/cmp/androidApp/build.gradle.kts` (new): `plugins { alias(libs.plugins.tmdb.module) }`;
    `dependencies { implementation(projects.apps.cmp.composeApp); implementation(projects.core.di); implementation(libs.koin.android); implementation(libs.androidx.activity.compose) }`.
  - `src/main/AndroidManifest.xml` (new): `<application android:name=".TmdbApplication" android:label="TMDB" android:theme="@android:style/Theme.Material.NoActionBar">`
    with `MainActivity` (`exported="true"`, MAIN / LAUNCHER intent filter).
  - `src/main/kotlin/.../TmdbApplication.kt` (new): `class TmdbApplication : Application() { override fun onCreate() { super.onCreate(); initKoin { androidContext(this@TmdbApplication) } } }`.
  - `src/main/kotlin/.../MainActivity.kt` (new): `class MainActivity : ComponentActivity()`; `onCreate`:
    `enableEdgeToEdge()`, `setContent { App() }`.
- Unit tests: none.
- Compile: `./gradlew :apps:cmp:androidApp:assembleDebug`

### Step 8: `apps/cmp/iosApp` Xcode project  (setup) ✅
- Files (new):
  - `apps/cmp/iosApp/iosApp/iOSApp.swift`:
    ```swift
    import SwiftUI
    import ComposeApp

    @main
    struct iOSApp: App {
        init() { SharedCoreKt.setUpSharedCore() }
        var body: some Scene { WindowGroup { ContentView() } }
    }
    ```
  - `apps/cmp/iosApp/iosApp/ContentView.swift`: a `UIViewControllerRepresentable` returning
    `MainViewControllerKt.MainViewController()`, and `ContentView` showing it with `.ignoresSafeArea()`.
  - `apps/cmp/iosApp/iosApp/Info.plist`: `CADisableMinimumFrameDurationOnPhone = true`, launch screen dictionary,
    standard bundle keys.
  - `apps/cmp/iosApp/iosApp.xcodeproj/project.pbxproj`: one application target `iosApp`.
    - Swift sources: `iOSApp.swift`, `ContentView.swift`.
    - A Run Script phase before Compile Sources:
      `cd "$SRCROOT/../../.." && ./gradlew :apps:cmp:composeApp:embedAndSignAppleFrameworkForXcode`.
    - Build settings:
      - `FRAMEWORK_SEARCH_PATHS = $(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`
      - `OTHER_LDFLAGS = -framework ComposeApp`
      - `PRODUCT_BUNDLE_IDENTIFIER = com.banhmilabs.tmdb.cmp`, `IPHONEOS_DEPLOYMENT_TARGET = 16.0`
      - `ENABLE_USER_SCRIPT_SANDBOXING = NO` (the script runs Gradle), `SWIFT_VERSION = 6.0`,
        `GENERATE_INFOPLIST_FILE = NO`, `INFOPLIST_FILE = iosApp/Info.plist`
      - Debug and Release configurations.
  - `apps/cmp/iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`: shared scheme.
  - `.gitignore`: `xcuserdata/`, `*.xcworkspace/xcuserdata`, `apps/cmp/iosApp/build/` if not covered.
- Unit tests: none.
- Compile: `xcodebuild -project apps/cmp/iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' -configuration Debug CODE_SIGNING_ALLOWED=NO build`

### Step 9: Konsist path rule and CLAUDE.md  (setup) ✅
- Files: `architecture-test/src/test/kotlin/com/banhmilabs/tmdb/architecture/ModulePaths.kt`, `CLAUDE.md` (modify)
- What:
  - `MODULE_DIR`: add `apps/[^/]+/(composeApp|androidApp)` so shell files must use
    `com.banhmilabs.tmdb.apps.cmp.composeApp` / `…androidApp` (Android sources sit under `src/main/kotlin`,
    still matched by `/src/`).
  - `CLAUDE.md`:
    - Repository layout: `apps/cmp` exists (designsystem, composeApp, androidApp, iosApp).
    - Commands: run Android with `./gradlew :apps:cmp:androidApp:installDebug`; run iOS by opening
      `apps/cmp/iosApp/iosApp.xcodeproj`; build iOS with the `xcodebuild` command above.
    - Modules table: add the `apps/<app>/composeApp` / `androidApp` rows.
- Unit tests: existing `PackageRulesTest` now also covers the shell files.
- Compile: `./gradlew :architecture-test:compileTestKotlin`

After the last step, run: `./gradlew detekt jvmTest koverVerify :architecture-test:test --continue` (CI set,
includes the `build-logic` tests), `./gradlew assemble`, the `xcodebuild` command from step 8, then launch the app
manually: Android on the `kmp_tmdb_test` emulator (`installDebug` + `adb shell am start`), iOS on a simulator. In
both, check that the placeholder Home shows in the dark theme (subtask "How to test").

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| (prerequisite for AC-1–AC-8 on cmp) App launches on Android with dark theme and placeholder Home | 4–7 | Manual: subtask How to test 1; `:apps:cmp:androidApp:assembleDebug` |
| (prerequisite) App launches on iOS | 4–6, 8 | Manual: How to test 2; `xcodebuild` build |
| (prerequisite) JVM checks incl. module rules for `apps/cmp` | 3, 4, 9 | `ModuleTypeTest`, `DependencyRulesTest` new cases; `PackageRulesTest`; CI set (How to test 3) |
| AC-2 (partial: placeholder Home destination exists) | 6 | Manual launch shows "Home" |

## Out of scope
- Onboarding screen, start-screen choice, Compose resources (strings en / pt-BR), onboarding typography and
  gradients, Compose UI test setup -> 04-cmp-onboarding-happy-path.
- Inter font -> later, if wanted (system font for now).
- App icons and launch screen artwork -> later.
- `apps/native` shells -> 06-native-android-app-shell, 07-native-ios-app-shell.
- Desktop app (optional in architecture 11) -> not planned.

## Risks
- AGP 9 built-in Kotlin with `com.android.application` + the Compose compiler plugin. The exact DSL for
  `jvmToolchain` and `buildFeatures.compose` may differ — step 7's `assembleDebug` verifies; adjust in
  `AndroidApplication.kt`.
- `compose-material3` 1.9.0 must match Compose Multiplatform 1.12.2 — if Gradle resolves a conflict or fails, use
  the material3 version that the 1.12.2 release notes pair with.
- Navigation 2.10.0 type-safe routes need the serialization plugin on `composeApp` (already applied by
  `tmdb.module`).
- Hand-written `project.pbxproj`: object IDs and build phases must be consistent. `xcodebuild` in step 8 is the
  check; Xcode may rewrite parts of the file on first open (commit those changes if any).
- The Xcode Run Script needs a JDK on Xcode's `PATH` — document in `CLAUDE.md` if `xcodebuild` can't find Java
  (set `JAVA_HOME` in the script from `/usr/libexec/java_home -v 21`).
- The iOS deployment target `16.0` is duplicated in the Xcode project (the catalog's `ios-deploymentTarget` can't
  be read by Xcode) — noted in `CLAUDE.md` next to the iOS command.

## Deviations
- `android-compileSdk` 36 → 37 for every module (from user): Compose Multiplatform 1.12.2 pulls AndroidX Compose
  1.12.1, Navigation 2.10 and Lifecycle 2.11, which require compileSdk 37. `CLAUDE.md` updated.
- `android-targetSdk` 36 → 37 (from user, after the plan was done), matching compileSdk.
- `org.gradle.jvmargs` `-Xmx4g` → `-Xmx6g`: `assemble` ran out of heap linking the release `ComposeApp` frameworks
  for both iOS targets.
- `MainActivity` pins light system bar icons (`SystemBarStyle.dark(TRANSPARENT)`): with plain `enableEdgeToEdge()`
  a light-mode device drew dark icons on the always-dark app.
- `MainViewController()` keeps its type-like name (architecture 11 / Compose Multiplatform convention, called from
  Swift) with `@Suppress("FunctionNaming")`.
- `configureAndroidApplication(app)` takes the app name to derive `applicationId`; the Android shell's dependency check
  is a separate `enforceAndroidDependencyRules` sharing `checkDependency` with the KMP check.
- The Konsist `MODULE_DIR` regex is split over two string literals (line length).
- `.gitignore`: the Xcode `xcuserdata` patterns now match any depth (`xcuserdata/`); they only matched a root `iosApp/`.
- Known warning: the iOS linker reports that Compose's bundled ICU data was built for iOS-simulator 18.5 while the app
  targets 16.0. The app builds and runs (checked on an iOS 26 simulator); running on iOS 16–18.4 is not verified.
