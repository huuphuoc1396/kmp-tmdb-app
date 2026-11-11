package com.banhmilabs.tmdb.core.datastore

import androidx.datastore.core.DataStore
import com.banhmilabs.tmdb.core.security.CipherException
import com.banhmilabs.tmdb.core.security.MasterKeyCipher
import com.banhmilabs.tmdb.core.security.masterKeyCipher
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.Path
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class EncryptedDataStoreTest {
    private val fileSystem = FileSystem.SYSTEM
    private val directory: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "encrypted-datastore-${Random.nextLong()}"
    private val path = directory / "stored.json.enc"
    private val cipher = masterKeyCipher("EncryptedDataStoreTest")

    @AfterTest
    fun deleteFiles() {
        fileSystem.deleteRecursively(directory)
    }

    @Test
    fun missingFileReadsDefault() = runTest {
        withStore { store -> assertEquals(Stored(), store.data.first()) }
    }

    @Test
    fun updatePersistsAcrossRestart() = runTest {
        withStore { store -> store.updateData { it.copy(flag = true) } }

        withStore { store -> assertEquals(Stored(flag = true), store.data.first()) }
    }

    @Test
    fun fileOnDiskDoesNotContainPlaintext() = runTest {
        withStore { store -> store.updateData { it.copy(name = "secret") } }

        assertFalse(fileSystem.read(path) { readByteArray() }.decodeToString().contains("secret"))
    }

    @Test
    fun corruptFileResetsToDefault() = runTest {
        fileSystem.createDirectories(directory)
        fileSystem.write(path) { write(Random.nextBytes(64)) }

        withStore { store -> assertEquals(Stored(), store.data.first()) }
    }

    @Test
    fun keyMismatchResetsToDefault() = runTest {
        withStore { store -> store.updateData { it.copy(flag = true) } }

        withStore(masterKeyCipher("EncryptedDataStoreTest-other")) { store ->
            assertEquals(Stored(), store.data.first())
        }
    }

    @Test
    fun writeFailurePropagatesAndKeepsPreviousValue() = runTest {
        withStore { store -> store.updateData { it.copy(flag = true) } }
        val failingCipher = mockk<MasterKeyCipher> {
            every { decrypt(any()) } answers { cipher.decrypt(firstArg()) }
            every { encrypt(any()) } throws CipherException("Keystore unavailable")
        }

        withStore(failingCipher) { store ->
            assertFailsWith<CipherException> { store.updateData { it.copy(name = "lost") } }
            assertEquals(Stored(flag = true), store.data.first())
        }
    }

    /** Creates a store, runs [block], then cancels the store's scope so the next store may open the same file. */
    private suspend fun TestScope.withStore(
        storeCipher: MasterKeyCipher = cipher,
        block: suspend (DataStore<Stored>) -> Unit,
    ) {
        val job = Job()
        val store = encryptedDataStore(
            serializer = Stored.serializer(),
            defaultValue = Stored(),
            path = path,
            fileSystem = fileSystem,
            cipher = storeCipher,
            scope = CoroutineScope(coroutineContext + job),
        )
        try {
            block(store)
        } finally {
            job.cancelAndJoin()
        }
    }

    @Serializable
    private data class Stored(val flag: Boolean = false, val name: String = "")
}
