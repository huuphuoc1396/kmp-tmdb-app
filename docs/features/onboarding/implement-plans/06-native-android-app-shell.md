# Plan: App shell (native Android)

Requirement: [06 - App shell (native Android)](../subtasks/06-native-android-app-shell.md)
Status: Done

## Summary
`tmdb.module` learns the Android-only side of `apps/native`. `:apps:native:androidApp` becomes an Android
application, and `:apps:native:designsystem` (and later native `feature/*` modules) becomes an Android library, both
with Jetpack Compose through the androidx Compose BOM instead of Compose Multiplatform. Because `native` is a Java
keyword that AGP rejects in a namespace, the package segment for `apps/native` is `nativeapp`; build-logic and
Konsist apply the same mapping. The shell mirrors `apps/cmp`'s:
- `TmdbApplication` starts Koin.
- `MainActivity` is edge-to-edge with light bars.
- A dark `TmdbTheme`.
- A Navigation Compose graph with a placeholder Home.

## Decisions
- **Package segment `nativeapp` for `apps/native`** (from user).
  - The directory and Gradle path stay `apps/native` / `:apps:native:*`.
  - The namespace, Kotlin packages and applicationId use `nativeapp`, for example
    `com.banhmilabs.tmdb.apps.nativeapp.androidApp` and applicationId `com.banhmilabs.tmdb.nativeapp`.
  - Reason: AGP 9.5.1's `validateNamespaceValue` uses `SourceVersion.isName`, which rejects the Java keyword
    `native`.
  - Rejected: renaming the directory.
- **Android application module is `androidApp`**, as in `apps/cmp` (from user). `moduleTypeOf` already maps it to
  `ANDROID_APP`. `docs/architecture.md` 11 changes `app/` → `androidApp/`. Rejected: `app`.
- **Native Android modules are Android libraries, not KMP**:
  - Kinds `DESIGNSYSTEM`, `NAV`, `API` and `UI` under `apps/native` get `configureAndroidLibrary()`
    (`com.android.library`, AGP built-in Kotlin, Jetpack Compose, serialization, Detekt).
  - Architecture 11: Gradle modules in `apps/native` are the Android app; iOS reaches Kotlin only through `shared`
    (subtask 07).
  - The dependency rules stay the same table, checked on the Android configurations like `androidApp` today.
- **Jetpack Compose through the androidx BOM `2026.09.00`** (Compose 1.12.1, the same Compose runtime that Compose
  Multiplatform 1.12.2 maps to on Android, material3 1.4.0).
  - Navigation is `androidx.navigation:navigation-compose:2.10.2`.
  - The catalog gets new `jetpack-*` aliases so they don't collide with the JetBrains `compose-*` /
    `androidx-navigation-compose` artifacts used by `apps/cmp`.
  - The two apps never share a module, so the two Compose distributions never meet in one classpath.
- **Shared Jetpack Compose setup in build-logic**: `configureJetpackCompose()` adds the compose compiler plugin,
  `buildFeatures.compose`, the BOM and ui / foundation / material3. It is applied to native Android libraries and
  to `:apps:native:androidApp`, but not to `:apps:cmp:androidApp`, which gets Compose from `composeApp`.
- **`TmdbTheme` is written again in `apps/native/designsystem`** with the same colour values as
  `apps/cmp/designsystem`: the subtask requires the apps to share no code. System font, as in plan 03.
- **App label "TMDB Native"**, so the native app is told apart from the cmp app ("TMDB") when both are installed.
- **Gradle `jvmTest` for Android-only modules is not wired yet**: the shell has no unit tests. Subtask 08 adds the
  first native ViewModel tests and the task alias, so CI's `jvmTest` runs them.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| Module type from path | `moduleTypeOf` (`:apps:<app>:designsystem`, `androidApp` already mapped) | `build-logic/.../ModuleType.kt` |
| Android application setup | `configureAndroidApplication(app)` | `build-logic/.../AndroidApplication.kt` |
| Dependency check on Android configurations | `enforceAndroidDependencyRules` | `build-logic/.../ModuleConventionPlugin.kt` |
| Namespace from path | `derivedNamespace()` | `build-logic/.../ProjectExtensions.kt` |
| Koin start-up | `initKoin(appDeclaration)` | `core:di` |
| Shell shape (Application, Activity, NavHost, placeholder Home) | `apps/cmp/androidApp`, `apps/cmp/composeApp` | `apps/cmp/...` |
| Theme values | `apps/cmp/designsystem/Theme.kt` (values copied, not depended on) | `apps/cmp/designsystem` |
| Package rule | `packageOf`, `appOf` | `architecture-test/.../ModulePaths.kt` |

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| catalog | `gradle/libs.versions.toml` | Jetpack Compose BOM, ui / foundation / material3, navigation-compose (androidx) |
| build-logic | `ProjectExtensions.kt`, `AndroidApplication.kt`, `ModuleConventionPlugin.kt` (modify); `AndroidLibrary.kt`, `JetpackCompose.kt` (new); `NamespaceTest.kt` (new), `ModuleTypeTest.kt` (modify) | `nativeapp` mapping, Android library and Jetpack Compose setup |
| architecture-test | `ModulePaths.kt` (modify) | Same `nativeapp` mapping for package and own-app rules |
| `apps/native/designsystem` | `build.gradle.kts`, `Theme.kt` (new) | Dark theme |
| `apps/native/androidApp` | `build.gradle.kts`, `AndroidManifest.xml`, `TmdbApplication.kt`, `MainActivity.kt`, `AppNavHost.kt`, `HomeRoute.kt`, `PlaceholderHomeScreen.kt` (new) | The shell |
| root | `settings.gradle.kts` | Include the two modules |
| docs | `docs/architecture.md` (5.2, 9, 11), `CLAUDE.md`, `.claude/rules/build-logic.md`, `.claude/rules/architecture-test.md`, subtask 06 | Native module types, `nativeapp`, run command |

## Steps
### Step 1: `nativeapp` package segment in build-logic and Konsist ✅
- Files:
  - `build-logic/.../ProjectExtensions.kt`
  - `build-logic/.../AndroidApplication.kt`
  - `build-logic/convention/src/test/.../NamespaceTest.kt` (new)
  - `architecture-test/.../ModulePaths.kt`
- What:
  - `internal fun namespaceOf(path: String): String`: joins the segments, strips `-`, and maps the segment
    `native` → `nativeapp` (`private val PACKAGE_SEGMENT_ALIASES = mapOf("native" to "nativeapp")`).
    `Project.derivedNamespace()` calls it.
  - `internal fun packageSegment(app: String): String`, the same alias. `configureAndroidApplication` uses it for
    `applicationId = "com.banhmilabs.tmdb.${packageSegment(app)}"`.
  - `ModulePaths.kt`: `packageOf` applies the same segment alias. The own-app prefix in "apps import no other app"
    uses `packageOf("apps/<app>")` instead of the raw `appOf`.
- Unit tests:
  - `NamespaceTest` (JUnit Jupiter): `coreModule`, `strippedDashes`, `nativeAppSegmentIsAliased`
    (`:apps:native:designsystem` → `com.banhmilabs.tmdb.apps.nativeapp.designsystem`), `cmpUnchanged`.
  - `ModuleTypeTest`: add `:apps:native:androidApp` → `ANDROID_APP`, app `native`.
- Compile: `./gradlew -p build-logic :convention:compileTestKotlin` and `./gradlew :architecture-test:compileTestKotlin`

### Step 2: Android library and Jetpack Compose conventions ✅
- Files:
  - `gradle/libs.versions.toml`
  - `build-logic/.../JetpackCompose.kt` (new)
  - `build-logic/.../AndroidLibrary.kt` (new)
  - `build-logic/.../AndroidApplication.kt`
  - `ModuleConventionPlugin.kt`
- What:
  - Catalog:
    - Versions: `jetpack-compose-bom = "2026.09.00"`, `jetpack-navigation = "2.10.2"`.
    - Libraries: `jetpack-compose-bom` (`androidx.compose:compose-bom`), `jetpack-compose-ui`,
      `jetpack-compose-foundation`, `jetpack-compose-material3` (no version, from the BOM), and
      `jetpack-navigation-compose`.
  - `internal fun Project.configureJetpackCompose()`:
    - Apply `org.jetbrains.kotlin.plugin.compose`.
    - Set `buildFeatures.compose = true` on the `CommonExtension`.
    - Add dependencies `implementation(platform(bom))` plus ui, foundation and material3.
  - `internal fun Project.configureAndroidLibrary()`:
    - Apply `com.android.library`, kotlinx.serialization and `tmdb.detekt`.
    - `LibraryExtension`: `namespace = derivedNamespace()`, compileSdk and minSdk from the catalog.
    - `KotlinAndroidProjectExtension.jvmToolchain`.
    - Call `configureJetpackCompose()`.
  - `configureAndroidApplication(app)`: call `configureJetpackCompose()` when `app == "native"`.
  - `ModuleConventionPlugin`, ordered dispatch:
    - `ANDROID_APP` → the application, as today.
    - `type.app == "native" && kind in NATIVE_ANDROID_KINDS` (`DESIGNSYSTEM`, `NAV`, `API`, `UI`) →
      `configureAndroidLibrary()` + `enforceAndroidDependencyRules`.
    - Otherwise KMP, as today.
    - The "Unknown module type" message is unchanged.
- Unit tests: none new (configuration); `ModuleTypeTest` / `DependencyRulesTest` still pass.
- Compile: `./gradlew :apps:cmp:androidApp:assembleDebug` (unchanged cmp still configures and builds)

### Step 3: `apps/native/designsystem` ✅
- Files: `settings.gradle.kts` (include `:apps:native:designsystem`); `apps/native/designsystem/build.gradle.kts`;
  `apps/native/designsystem/src/main/kotlin/com/banhmilabs/tmdb/apps/nativeapp/designsystem/Theme.kt`
- What:
  - `build.gradle.kts`: only `alias(libs.plugins.tmdb.module)`.
  - `@Composable fun TmdbTheme(content: @Composable () -> Unit)` with the same `darkColorScheme` values as cmp:
    Purple `0xFF8000FF`, DeepPurple `0xFF540BA1`, Night `0xFF15151D`.
- Unit tests: none (theme values only).
- Compile: `./gradlew :apps:native:designsystem:compileDebugKotlin`

### Step 4: `apps/native/androidApp` shell ✅
- Files:
  - `settings.gradle.kts` (include `:apps:native:androidApp`)
  - `apps/native/androidApp/build.gradle.kts`
  - `src/main/AndroidManifest.xml`
  - `src/main/kotlin/com/banhmilabs/tmdb/apps/nativeapp/androidApp/`: `TmdbApplication.kt`, `MainActivity.kt`,
    `AppNavHost.kt`, `HomeRoute.kt`, `PlaceholderHomeScreen.kt`
- What:
  - Dependencies: `projects.apps.native.designsystem`, `projects.core.di`, `libs.koin.android`,
    `libs.androidx.activity.compose`, `libs.jetpack.navigation.compose`, `libs.kotlinx.serialization.core`.
  - Manifest: `.TmdbApplication`, launcher `.MainActivity`, `android:label="TMDB Native"`, theme
    `@android:style/Theme.Material.NoActionBar` (as cmp before 04).
  - `TmdbApplication`: `initKoin { androidContext(this@TmdbApplication) }`.
  - `MainActivity`: `enableEdgeToEdge(SystemBarStyle.dark(TRANSPARENT) ×2)` then
    `setContent { TmdbTheme { AppNavHost() } }`.
  - `@Serializable internal data object HomeRoute`.
  - `@Composable internal fun AppNavHost(navController = rememberNavController())`: `NavHost` starting at
    `HomeRoute` → `PlaceholderHomeScreen()`.
  - `PlaceholderHomeScreen`: the same as cmp's, "Home" on the background.
- Unit tests: none (shell; manual run as in the subtask).
- Compile: `./gradlew :apps:native:androidApp:assembleDebug`

### Step 5: Docs ✅
- Files: `docs/architecture.md`, `CLAUDE.md`, `.claude/rules/build-logic.md`, `.claude/rules/architecture-test.md`
- What:
  - **`docs/architecture.md`**:
    - 5.2: the `androidApp` row covers both apps. `designsystem` / `feature` modules of `apps/native` are Android
      libraries with Jetpack Compose.
    - 9: the status note says `apps/native` Android modules exist and are configured as above.
    - 11: the tree uses `androidApp/` and notes the `nativeapp` package segment.
  - **`CLAUDE.md`**:
    - Repository layout: `apps/native` now has its Android shell.
    - Commands: run the native app with `./gradlew :apps:native:androidApp:installDebug`.
    - Modules table: native `androidApp` / `designsystem` rows.
    - Conventions: the package of `apps/native` is `…apps.nativeapp…`.
  - **Rules**:
    - `build-logic.md`: `configureAndroidLibrary`, `configureJetpackCompose`, the `nativeapp` alias.
    - `architecture-test.md`: the `packageOf` alias.
- Unit tests: none.
- Compile: none (docs only).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| (prerequisite for AC-1–AC-8 on native Android) app launches with placeholder Home, dark theme, light bars | 3, 4 | Manual: subtask "How to test" 1 |
| Installs next to the cmp app | 1, 4 | Manual: subtask "How to test" 2 (different applicationId and label) |
| Module rules hold for `apps/native` (no dependency on `apps/cmp`) | 1, 2 | `NamespaceTest`, `ModuleTypeTest`, `DependencyRulesTest`, Konsist `PackageRulesTest` / `DependencyRulesTest`; subtask "How to test" 3 |

## Out of scope
- The iOS app, `shared` and SKIE → 07-native-ios-app-shell.
- The onboarding screen and the `jvmTest` wiring for Android-only modules → 08-native-android-onboarding.
- App icon; Inter font.

## Risks
- **Two Compose distributions in one build** (JetBrains for cmp, androidx for native). They live in separate
  modules and apps, so Gradle resolves each app on its own. Mitigation: step 4's `assembleDebug` plus the cmp
  build in step 2.
- **AGP built-in Kotlin with `com.android.library`**: the cmp shell uses it only for the application. Mitigation:
  step 3 compiles a library; if `KotlinAndroidProjectExtension` is not registered there, configure the toolchain
  through the Android extension (deviation).
- **Konsist and the `nativeapp` alias**: every rule that builds a package from a path must use `packageOf`.
  Mitigation: `:architecture-test:test` runs over the new files (`PackageRulesTest` would fail on a missed spot).
