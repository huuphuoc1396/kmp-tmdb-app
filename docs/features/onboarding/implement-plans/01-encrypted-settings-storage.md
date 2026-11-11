# Plan: Encrypted settings storage

Requirement: [01 - Encrypted settings storage](../subtasks/01-encrypted-settings-storage.md)
Status: Done

## Summary
`core:security` gets a `MasterKeyCipher`: AES-256-GCM with a per-device master key. On Android it uses an
Android Keystore key through `javax.crypto`, so the key never leaves the Keystore. On iOS the key is kept in
the Keychain, and on JVM (tests) it is held in memory; both of these use `cryptography-kotlin` for AES-GCM.
`core:datastore` provides a generic encrypted DataStore: any `@Serializable` type is stored as JSON encrypted
by `MasterKeyCipher`, and a file that can't be decrypted or parsed resets to the default value. The settings
schema itself (`onboardingCompleted`) belongs to the `settings` context and is created in 02. One shared
cipher test in `commonTest` runs on JVM, on an Android device (real Keystore) and on the iOS simulator (real
Keychain).

## Decisions
- Android encrypts with the Keystore key directly (`javax.crypto.Cipher`, `AES/GCM/NoPadding`); iOS and JVM
  use `cryptography-kotlin`. Encryption lives in `core:security`, and `core:datastore` calls it. — A
  non-exportable Keystore key can't be handed to `cryptography-kotlin`. (from user, 2026-11-10; rejected: a
  random data key wrapped by the Keystore key.) `docs/architecture.md` 10 and 12 are updated to match.
- `core:datastore` is generic: `encryptedDataStore(serializer, defaultValue, ...)`. The
  `AppSettings(onboardingCompleted)` schema moves to `service/settings/data` in 02. — Architecture 5.1/5.3:
  `core/` holds no business features, `onboarding` belongs to the `settings` context, and infra may not import
  `service`. (from user, 2026-11-11; replaces the earlier choice of `AppSettings` in `core:datastore`.)
- Settings that can't be decrypted or parsed are replaced by the default value (DataStore
  `ReplaceFileCorruptionHandler`). — Covers a lost key (backup restored on a new device) and corrupt files
  without a crash; matches architecture 10. (from user)
- Cipher behavior is tested once in `commonTest` and runs in `jvmTest`, `androidDeviceTest` and
  `iosSimulatorArm64Test`. `iosTest` adds only the Keychain accessibility check. — `commonTest` is allowed
  for multiplatform-only tests (no MockK), and here it exercises each platform's real `actual` code without
  duplicated test classes. (from user, 2026-11-11; rejected: one test class per platform.)
- Test cleanup goes through a test-only `internal expect fun deleteMasterKeyForTest(alias: String)` in
  `commonTest`, with actuals in `jvmTest` / `androidDeviceTest` / `iosTest`. — Keeps the production API free
  of a delete function that nothing else needs.
- Android device tests use `sourceSetTreeName = "test"`, so `androidDeviceTest` also compiles `commonTest`.
  `commonTest` gets `kotlin-test` in `configureKotlinMultiplatform()`. — This is how the shared test reaches
  the device. It applies to every shared module, which is harmless for modules without `commonTest` code.
- `tmdb.module` applies the kotlinx.serialization compiler plugin to every shared module. — Module build files
  may only apply `tmdb.module` (`.claude/rules/build-logic.md`). The plugin does nothing without
  `@Serializable`, and `data` modules (DTOs) and `service/settings/data` will need it.
- The `MasterKeyCipher` API is synchronous (`encrypt` / `decrypt` are plain functions). — Payloads are a few
  bytes, and DataStore already runs `readFrom` / `writeTo` on its own scope.
- `MasterKeyCipher` is created by `expect fun masterKeyCipher(alias: String): MasterKeyCipher`. — The Android
  Keystore needs no `Context`, so one common signature works on every platform. The caller (02) chooses the
  alias.
- The DataStore factory takes the file `Path`, the `FileSystem` and the `CoroutineScope` as parameters. — The
  Android file path needs a `Context`, and `DispatcherProvider` doesn't exist yet. The composition root
  (`core:di`, in 02) passes the platform path, `FileSystem.SYSTEM`, and a scope built from
  `DispatcherProvider.io`.
- Errors are exceptions at this level (`CipherException`, `IOException`). — The repository in
  `service/settings/data` (02) maps them to `AppResult` / `AppError`. `core:common` is allowed for infra and
  security (5.3) but isn't needed here.
- Wire format: `iv (12 bytes) || ciphertext || tag (16 bytes)` on every platform. — It is what both the
  Keystore cipher and `cryptography-kotlin` AES-GCM produce by default. It is never shared across devices.
- JVM key: random AES-256 key, generated once per alias per process and kept in memory. — The JVM target is
  for tests only (architecture 10). It is a real cipher, not a fake.

## Reuse
| Need | Existing component | Location |
|---|---|---|
| KMP targets, test deps, plugin application for every shared module | `configureKotlinMultiplatform()` (called by `tmdb.module`) | `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/KotlinMultiplatform.kt` |
| Catalog access in build logic | `Project.libs`, `libs.version(...)`, `libs.findLibrary(...)` | `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/ProjectExtensions.kt` |
| Dependency check (`datastore` → `security` allowed: infra → security) | `DependencyRules.violation` | `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/DependencyRules.kt` |
| Module shells, `datastore` → `security` dependency | `core:security`, `core:datastore` | `core/security/build.gradle.kts`, `core/datastore/build.gradle.kts` |
| Package and import rules | Konsist | `architecture-test/src/test/kotlin/com/banhmilabs/tmdb/architecture/` |
| Test style | `CommonSmokeTest` | `core/common/src/jvmTest/kotlin/com/banhmilabs/tmdb/core/common/CommonSmokeTest.kt` |

Nothing exists yet for encryption, key storage, DataStore or serialization; all of it is new.

## Changes overview
| Module | Files (new / modified) | Why |
|---|---|---|
| root | `gradle/libs.versions.toml`, `build.gradle.kts` (modify) | New libraries and the serialization plugin |
| build-logic | `buildlogic/KotlinMultiplatform.kt` (modify) | Serialization plugin, `commonTest` deps, Android device tests |
| `core:security` | `build.gradle.kts` (modify); `MasterKeyCipher.kt`, `CipherException.kt`, `MasterKeyCipher.android.kt`, `MasterKeyCipher.ios.kt`, `MasterKeyCipher.jvm.kt` (new); common / jvm / device / iOS tests (new); `SecuritySmokeTest.kt` (delete) | Master key + AES-GCM |
| `core:datastore` | `build.gradle.kts` (modify); `EncryptedJsonSerializer.kt`, `EncryptedDataStore.kt` (new); jvm tests (new); `DatastoreSmokeTest.kt` (delete) | Generic encrypted store |
| docs | `docs/architecture.md`, `CLAUDE.md`, `.claude/rules/testing.md`, `.claude/rules/build-logic.md` (modify) | Record the Android cipher decision and the new build/test setup |

Affected modules: every shared module gets the serialization plugin, `commonTest` deps and the device-test
setup (step 2), with no code added to them.

## Steps

### Step 1: Catalog entries  (setup) ✅
- Files: `gradle/libs.versions.toml` (modify), `build.gradle.kts` (modify)
- What:
  - `[versions]`: `datastore = "1.3.0"`, `cryptography = "0.6.0"`, `kotlinx-serialization = "1.12.0"`,
    `kotlinx-coroutines = "1.11.0"`, `okio = "3.18.2"`, `androidx-test-runner = "1.7.0"`.
  - `[libraries]`:
    - `androidx-datastore-core-okio = { module = "androidx.datastore:datastore-core-okio", version.ref = "datastore" }`
    - `cryptography-core`, `cryptography-provider-jdk`, `cryptography-provider-optimal`
      (`dev.whyoleg.cryptography:*`, `version.ref = "cryptography"`)
    - `kotlinx-serialization-json` (`org.jetbrains.kotlinx:kotlinx-serialization-json`)
    - `kotlinx-coroutines-core`, `kotlinx-coroutines-test` (`org.jetbrains.kotlinx:*`)
    - `okio` (`com.squareup.okio:okio`)
    - `androidx-test-runner` (`androidx.test:runner`)
  - `[plugins]`: `kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }`.
  - Root `build.gradle.kts`: `alias(libs.plugins.kotlin.serialization) apply false`, so the plugin is on the
    build classpath for `tmdb.module` to apply by id.
- Unit tests: none.
- Compile: `./gradlew help`

### Step 2: Shared module setup in `configureKotlinMultiplatform()`  (setup for AC-3 platform tests) ✅
- Files: `build-logic/convention/src/main/kotlin/com/banhmilabs/tmdb/buildlogic/KotlinMultiplatform.kt` (modify)
- What:
  - `pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")`.
  - In the `KotlinMultiplatformAndroidLibraryExtension` block, enable the device test compilation with
    `sourceSetTreeName = "test"` and `instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`
    (AGP 9 KMP library DSL: `withDeviceTestBuilder { sourceSetTreeName = "test" }.configure { instrumentationRunner = ... }`,
    or `withDeviceTest { ... }` if that is the form this AGP exposes). The runner class name is a
    `private const val`.
  - `sourceSets.getByName("commonTest").dependencies`: `kotlin-test`.
  - `sourceSets.getByName("androidDeviceTest").dependencies`: `androidx-test-runner`.
  - KDoc: unit tests default to `jvmTest` (`kotlin-test`, MockK); `commonTest` (`kotlin-test`) also runs on the
    Android device and the iOS simulator; no MockK outside `jvmTest`.
- Unit tests: none (the existing `build-logic` tests must still pass).
- Compile: `./gradlew compileTestKotlinJvm`, then
  `./gradlew :core:common:tasks --all | grep -i -E "deviceTest|iosSimulatorArm64Test"` to confirm the tasks exist.

### Step 3: Master key cipher in `core:security`  (AC-3 partial, EC-1 partial) ✅
- Files (under `core/security/`, package `com.banhmilabs.tmdb.core.security`):
  - `build.gradle.kts` (modify): `iosMain` and `jvmMain` dependencies `libs.cryptography.core`;
    `iosMain` `libs.cryptography.provider.optimal`; `jvmMain` `libs.cryptography.provider.jdk`.
  - `src/commonMain/.../MasterKeyCipher.kt` (new):
    ```kotlin
    /** AES-256-GCM with a per-device master key. Output: iv || ciphertext || tag. */
    interface MasterKeyCipher {
        fun encrypt(plaintext: ByteArray): ByteArray
        /** @throws CipherException if the data was tampered with or the key doesn't match. */
        fun decrypt(ciphertext: ByteArray): ByteArray
    }

    /** Returns the cipher for [alias], creating and storing the key on first use. */
    expect fun masterKeyCipher(alias: String): MasterKeyCipher
    ```
  - `src/commonMain/.../CipherException.kt` (new):
    `class CipherException(message: String, cause: Throwable? = null) : Exception(message, cause)`.
  - `src/androidMain/.../MasterKeyCipher.android.kt` (new): `internal class AndroidKeystoreCipher(alias)`.
    - Key: `KeyStore.getInstance("AndroidKeyStore").load(null)`. If `alias` is missing, generate it with
      `KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")` and
      `KeyGenParameterSpec.Builder(alias, PURPOSE_ENCRYPT or PURPOSE_DECRYPT).setBlockModes(BLOCK_MODE_GCM).setEncryptionPaddings(ENCRYPTION_PADDING_NONE).setKeySize(256)`.
    - `encrypt`: `Cipher.getInstance("AES/GCM/NoPadding")`, `init(ENCRYPT_MODE, key)` (the Keystore picks a
      random IV), return `cipher.iv + cipher.doFinal(plaintext)`.
    - `decrypt`: split the 12-byte IV, `init(DECRYPT_MODE, key, GCMParameterSpec(128, iv))`, `doFinal`.
    - `actual fun masterKeyCipher(alias): MasterKeyCipher = AndroidKeystoreCipher(alias)`.
  - `src/iosMain/.../MasterKeyCipher.ios.kt` (new): `internal class KeychainCipher(alias)`.
    - Key bytes are read with `SecItemCopyMatching` (`kSecClassGenericPassword`,
      `kSecAttrService = KEYCHAIN_SERVICE` = `"com.banhmilabs.tmdb.core.security"`, `kSecAttrAccount = alias`,
      `kSecReturnData = true`). If missing (`errSecItemNotFound`), generate a key
      (`AES.GCM` `keyGenerator(AES.Key.Size.B256).generateKeyBlocking()` →
      `encodeToByteArrayBlocking(AES.Key.Format.RAW)`) and `SecItemAdd` it with
      `kSecAttrAccessible = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`. Any other `OSStatus` →
      `CipherException("Keychain error $status")`.
    - Cipher: `CryptographyProvider.Default.get(AES.GCM).keyDecoder().decodeFromByteArrayBlocking(RAW, bytes).cipher()`,
      `encryptBlocking` / `decryptBlocking`. The key is loaded lazily, once per instance.
    - `KEYCHAIN_SERVICE` is `internal` so `iosTest` can query and delete the item.
  - `src/jvmMain/.../MasterKeyCipher.jvm.kt` (new): `internal class InMemoryKeyCipher(key: AES.GCM.Key)` using
    `cryptography-kotlin` (JDK provider). `actual fun masterKeyCipher(alias)` looks up or generates the key in
    `internal val inMemoryKeys = ConcurrentHashMap<String, AES.GCM.Key>()`.
  - Byte sizes (12, 16, 128, 256) are named constants.
- Pseudo-code (every `decrypt`):
  ```
  if (input.size < IV_SIZE + TAG_SIZE) throw CipherException("Ciphertext too short")
  try { platformDecrypt(input) } catch (platform crypto exception) { throw CipherException("Decryption failed", it) }
  ```
  (Android: `GeneralSecurityException`; `cryptography-kotlin`: its exception type, checked when writing.)
- Unit tests:
  - `src/commonTest/.../MasterKeyCipherTest.kt` (new, `kotlin.test`, no mocks; runs in `jvmTest`,
    `androidDeviceTest`, `iosSimulatorArm64Test`). Each test uses a unique alias
    (`"test-" + Random.nextLong()`) and calls `deleteMasterKeyForTest(alias)` in `@AfterTest`:
    `encryptThenDecryptReturnsPlaintext`, `ciphertextDoesNotContainPlaintext`,
    `encryptingTwiceGivesDifferentOutput`, `tamperedCiphertextThrowsCipherException`,
    `tooShortInputThrowsCipherException`, `newInstanceWithSameAliasDecrypts` (key persists),
    `differentAliasCannotDecrypt`.
  - `src/commonTest/.../MasterKeyTestSupport.kt` (new): `internal expect fun deleteMasterKeyForTest(alias: String)`.
  - `src/jvmTest/.../MasterKeyTestSupport.jvm.kt` (new): `inMemoryKeys.remove(alias)`.
  - Delete `src/jvmTest/.../SecuritySmokeTest.kt`.
- Compile: `./gradlew :core:security:compileTestKotlinJvm :core:security:compileAndroidMain`
  (iOS in step 4)

### Step 4: Platform test support for the master key  (AC-3 partial) ✅
- Files:
  - `core/security/src/androidDeviceTest/kotlin/com/banhmilabs/tmdb/core/security/MasterKeyTestSupport.android.kt`
    (new): `KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)`.
  - `core/security/src/iosTest/kotlin/com/banhmilabs/tmdb/core/security/MasterKeyTestSupport.ios.kt` (new):
    `SecItemDelete` for (`KEYCHAIN_SERVICE`, `alias`).
  - `core/security/src/iosTest/kotlin/com/banhmilabs/tmdb/core/security/KeychainCipherTest.kt` (new):
    `keyIsStoredThisDeviceOnly` (create a cipher, encrypt once, read the item back with
    `kSecReturnAttributes` and check `kSecAttrAccessible == kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`).
- Unit tests: as listed; the `commonTest` cases from step 3 now also run on the device and the simulator.
- Compile: `./gradlew :core:security:compileKotlinIosSimulatorArm64 :core:security:compileTestKotlinIosSimulatorArm64`
  (macOS + Xcode) and the Android device-test compile task confirmed in step 2 (for example
  `:core:security:compileAndroidDeviceTest`).
- Run locally: `./gradlew :core:security:iosSimulatorArm64Test` (macOS + Xcode) and
  `./gradlew :core:security:connectedAndroidDeviceTest` (emulator/device).

### Step 5: Generic encrypted store in `core:datastore`  (AC-3 partial, EC-1 partial) ✅
- Files (under `core/datastore/`, package `com.banhmilabs.tmdb.core.datastore`):
  - `build.gradle.kts` (modify): `commonMain`: `api(projects.core.security)` (replaces `implementation`;
    `MasterKeyCipher` is in the public signature), `api(libs.androidx.datastore.core.okio)` (callers receive
    `DataStore<T>`), `api(libs.okio)` (`Path`, `FileSystem`), `api(libs.kotlinx.serialization.json)`
    (`KSerializer`), `implementation(libs.kotlinx.coroutines.core)`; `jvmTest`:
    `implementation(libs.kotlinx.coroutines.test)`.
  - `src/commonMain/.../EncryptedJsonSerializer.kt` (new):
    `internal class EncryptedJsonSerializer<T>(private val serializer: KSerializer<T>, override val defaultValue: T, private val cipher: MasterKeyCipher) : OkioSerializer<T>`,
    with a private `Json { ignoreUnknownKeys = true }` (a file written by a newer build still loads).
  - `src/commonMain/.../EncryptedDataStore.kt` (new):
    ```kotlin
    /** DataStore of [T] stored as AES-GCM-encrypted JSON; unreadable files reset to [defaultValue].
     *  One instance per [path] per process. */
    fun <T> encryptedDataStore(
        serializer: KSerializer<T>,
        defaultValue: T,
        path: Path,
        fileSystem: FileSystem,
        cipher: MasterKeyCipher,
        scope: CoroutineScope,
    ): DataStore<T> = DataStoreFactory.create(
        storage = OkioStorage(fileSystem, EncryptedJsonSerializer(serializer, defaultValue, cipher)) { path },
        corruptionHandler = ReplaceFileCorruptionHandler { defaultValue },
        scope = scope,
    )
    ```
- Pseudo-code (`EncryptedJsonSerializer`):
  ```
  readFrom(source):
    bytes = source.readByteArray()
    try { json.decodeFromString(serializer, cipher.decrypt(bytes).decodeToString()) }
    catch (e: CipherException | SerializationException | IllegalArgumentException) {
      throw CorruptionException("Stored data unreadable", e)   // -> ReplaceFileCorruptionHandler -> defaultValue
    }
  writeTo(t, sink):
    sink.write(cipher.encrypt(json.encodeToString(serializer, t).encodeToByteArray()))
    // a CipherException here propagates to updateData's caller; the file keeps its previous content
  ```
- Unit tests (`src/jvmTest/...`; test type: a `private @Serializable data class Stored(val flag: Boolean = false, val name: String = "")`
  nested in each test class):
  - `EncryptedJsonSerializerTest` (real JVM `masterKeyCipher`, Okio `Buffer`):
    `writeThenReadRestoresValue`, `writtenBytesDoNotContainPlaintext`,
    `undecryptableBytesThrowCorruptionException`, `invalidJsonThrowsCorruptionException`,
    `unknownKeysAreIgnored`.
  - `EncryptedDataStoreTest` (real file in a fresh directory under `FileSystem.SYSTEM_TEMPORARY_DIRECTORY`,
    deleted after each test; `runTest`, store scope = a child `CoroutineScope` of the test that is cancelled
    at the end):
    `missingFileReadsDefault`, `updatePersistsAcrossRestart` (cancel the first store's scope, create a new
    store on the same path, read the updated value), `fileOnDiskDoesNotContainPlaintext`,
    `corruptFileResetsToDefault` (random bytes written to the path first), `keyMismatchResetsToDefault`
    (written with one alias, read with another), `writeFailurePropagatesAndKeepsPreviousValue` (MockK
    `MasterKeyCipher` whose `encrypt` throws `CipherException`: `updateData` throws, `data.first()` is still
    the previous value).
  - Delete `src/jvmTest/.../DatastoreSmokeTest.kt`.
- Compile: `./gradlew :core:datastore:compileTestKotlinJvm :core:datastore:compileAndroidMain :core:datastore:compileKotlinIosSimulatorArm64`
  (iOS needs macOS + Xcode)

### Step 6: Docs ✅
- Files: `docs/architecture.md`, `CLAUDE.md`, `.claude/rules/testing.md`, `.claude/rules/build-logic.md` (modify)
- What:
  - `docs/architecture.md` 10 ("Encrypted settings"): `core:security` encrypts with AES-GCM (Android:
    Keystore key via `javax.crypto`; iOS and JVM: `cryptography-kotlin`); `core:datastore` stores
    `@Serializable` values through it. 12 ("Settings" row) adjusted the same way.
  - `CLAUDE.md` Commands: Android device tests are configured:
    `./gradlew :<module>:connectedAndroidDeviceTest` (emulator/device); `commonTest` also runs there and on
    the iOS simulator. Remove "not configured yet".
  - `.claude/rules/testing.md`: replace "Platform tests (not configured yet)" with where they live
    (`commonTest` for shared behavior, `src/androidDeviceTest/` and `src/iosTest/` for platform-only checks
    and `actual`s of test helpers), their dependencies (`kotlin-test`, AndroidX test runner, no MockK) and
    the reference `MasterKeyCipherTest`. Update the `commonTest` bullet: `kotlin-test` is now configured.
  - `.claude/rules/build-logic.md` "Module build file": `tmdb.module` also applies the kotlinx.serialization
    plugin; don't apply it in module files.
- Unit tests: none.
- Compile: none (docs only).

After the last step, run: `./gradlew jvmTest` (includes the `build-logic` tests),
`./gradlew :core:security:detekt :core:datastore:detekt :architecture-test:test`, then `./gradlew assemble`
(expect/actual and build-logic changed; macOS + Xcode) and the platform tests from step 4 (local; say if
skipped).

## Traceability
| AC / EC | Step(s) | Test(s) |
|---|---|---|
| AC-3 (partial: stored data survives a restart, on-device) | 3, 4, 5 | `EncryptedDataStoreTest.updatePersistsAcrossRestart`; `MasterKeyCipherTest.newInstanceWithSameAliasDecrypts` on JVM, Android device and iOS simulator |
| EC-1 (partial: a failed write is an error to the caller, no crash) | 3, 5 | `EncryptedDataStoreTest.writeFailurePropagatesAndKeepsPreviousValue` |
| Subtask "value not in plain text" | 3, 5 | `MasterKeyCipherTest.ciphertextDoesNotContainPlaintext`, `EncryptedDataStoreTest.fileOnDiskDoesNotContainPlaintext` |
| Decision: unreadable file resets to default | 5 | `EncryptedDataStoreTest.corruptFileResetsToDefault`, `EncryptedDataStoreTest.keyMismatchResetsToDefault`, `EncryptedJsonSerializerTest.undecryptableBytesThrowCorruptionException` |
| Architecture 10: iOS key this-device-only | 4 | `KeychainCipherTest.keyIsStoredThisDeviceOnly` |

## Out of scope
- `service/settings/{domain,data}`: the `AppSettings(onboardingCompleted)` schema, the repository, `AppResult`
  mapping, and the Koin module -> 02-onboarding-completed-state.
- `core:di` wiring (platform file path, `FileSystem.SYSTEM`, scope from `DispatcherProvider`) and
  `DispatcherProvider` itself -> 02.
- Excluding the settings file from Android Auto Backup -> the app shell (the reset-to-default handler already
  covers a restored file).
- Database encryption (SQLCipher) -> planned (architecture 10).

## Risks
- AGP 9 KMP Android library device-test DSL (`withDeviceTestBuilder {}` vs `withDeviceTest {}`) and whether
  `sourceSetTreeName = "test"` pulls `commonTest` into `androidDeviceTest` — step 2 compiles and lists tasks
  before any test is written. If `commonTest` isn't included, add `androidDeviceTest.dependsOn(commonTest)` in
  the same function.
- Applying the serialization plugin by id needs it on the build classpath (root `apply false`); if Gradle
  can't resolve it from `build-logic`, add `compileOnly(libs.kotlin.serialization.gradlePlugin)`
  (`org.jetbrains.kotlin:kotlin-serialization`) to `build-logic/convention/build.gradle.kts`, as the rule for
  third-party plugins says.
- iOS provider support for AES-GCM in `cryptography-kotlin` 0.6.0 (`optimal` should select CryptoKit) —
  `MasterKeyCipherTest` exercises it on the simulator; if unsupported, switch `iosMain` to
  `cryptography-provider-cryptokit` explicitly.
- Keychain access from the Kotlin/Native simulator test runner can fail with `errSecMissingEntitlement`
  (-34018) — if it does, record it in `.claude/rules/testing.md` and run the iOS cipher tests from a host app
  later; the JVM run still covers the logic.
- One DataStore per file per process: tests that recreate the store cancel the first scope before creating the
  second.
- The Android Keystore can be reset on some devices (e.g. after a lock-screen change on old API levels).
  Decryption then fails, settings reset to defaults, and onboarding shows again. This is accepted as
  EC-1-like behavior.

## Deviations
- `cryptography-kotlin` 0.6.0 deprecates `CryptographyException` (it throws `IllegalStateException`), so the JVM and
  iOS ciphers catch `IllegalStateException` (JVM also `GeneralSecurityException`) and wrap it in `CipherException`.
- The size constants and the too-short check (`requireCiphertextSize`) live once in `commonMain` as `internal`
  declarations instead of being repeated per platform.
- iOS: the Keychain query builder `withKeychainQuery` is `internal` (not private) so `iosTest` can delete the key
  and read its attributes; the `NSData` conversion needs `@OptIn(BetaInteropApi::class)`.
- `KeychainCipherTest` compares the bridged `kSecAttrAccessible` value (`CFEqual` can't be imported in Kotlin/Native).
- Device-test DSL on AGP 9.5.1: `withDeviceTestBuilder { sourceSetTreeName = "test" }.configure { ... }`; the compile
  task is `compileAndroidDeviceTestSources`.
- `EncryptedJsonSerializer.readFrom` collects the caught exception and throws one `CorruptionException` (Detekt
  `ThrowsCount` allows two throws per function).
- The iOS cipher also wraps encryption failures in `CipherException`.
