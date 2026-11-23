package com.banhmilabs.tmdb.service.settings.data

import androidx.datastore.core.DataStore
import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.datastore.encryptedDataStore
import com.banhmilabs.tmdb.core.security.CipherException
import com.banhmilabs.tmdb.core.security.MasterKeyCipher
import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import com.banhmilabs.tmdb.service.settings.fake.SettingsRepositoryContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.IOException
import okio.Path
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsRepositoryImplTest : SettingsRepositoryContract() {
    private val fileSystem = FileSystem.SYSTEM
    private val directory: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "settings-repository-${Random.nextLong()}"
    private val path = directory / "settings.json"
    private val cipher = PassThroughCipher()

    @AfterTest
    fun deleteFiles() {
        fileSystem.deleteRecursively(directory)
    }

    override fun TestScope.createRepository(): SettingsRepository = SettingsRepositoryImpl(dataStore(backgroundScope))

    @Test
    fun completedSurvivesRestart() = runTest {
        withRepository { assertEquals(AppResult.Success(Unit), it.completeOnboarding()) }

        withRepository { assertTrue(it.observeOnboardingCompleted().first()) }
    }

    @Test
    fun writeFailureReturnsUnknownAndStaysNotCompleted() = runTest {
        val failingCipher = PassThroughCipher(encryptFailure = CipherException("Keystore unavailable"))

        withRepository(failingCipher) { repository ->
            assertEquals(AppResult.Failure(AppError.Unknown), repository.completeOnboarding())
            assertFalse(repository.observeOnboardingCompleted().first())
        }
    }

    @Test
    fun readIoErrorEmitsNotCompleted() = runTest {
        assertFalse(SettingsRepositoryImpl(UnreadableDataStore()).observeOnboardingCompleted().first())
    }

    @Test
    fun corruptFileReadsNotCompleted() = runTest {
        fileSystem.createDirectories(directory)
        fileSystem.write(path) { write(Random.nextBytes(64)) }

        withRepository { assertFalse(it.observeOnboardingCompleted().first()) }
    }

    private fun dataStore(scope: CoroutineScope, storeCipher: MasterKeyCipher = cipher) = encryptedDataStore(
        serializer = AppSettings.serializer(),
        defaultValue = AppSettings(),
        path = path,
        fileSystem = fileSystem,
        cipher = storeCipher,
        scope = scope,
    )

    private suspend fun TestScope.withRepository(
        storeCipher: MasterKeyCipher = cipher,
        block: suspend (SettingsRepository) -> Unit,
    ) {
        val job = Job()
        try {
            block(SettingsRepositoryImpl(dataStore(CoroutineScope(coroutineContext + job), storeCipher)))
        } finally {
            job.cancelAndJoin()
        }
    }

    private class PassThroughCipher(
        private val encryptFailure: CipherException? = null,
        private val decryptFailure: CipherException? = null,
    ) : MasterKeyCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray {
            encryptFailure?.let { throw it }
            return plaintext
        }

        override fun decrypt(ciphertext: ByteArray): ByteArray {
            decryptFailure?.let { throw it }
            return ciphertext
        }
    }

    private class UnreadableDataStore : DataStore<AppSettings> {
        override val data: Flow<AppSettings> = flow { throw IOException("Disk unavailable") }

        override suspend fun updateData(transform: suspend (t: AppSettings) -> AppSettings): AppSettings =
            throw IOException("Disk unavailable")
    }
}
