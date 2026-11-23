package com.banhmilabs.tmdb.core.datastore

import androidx.datastore.core.DataStore
import com.banhmilabs.tmdb.core.security.CipherException
import com.banhmilabs.tmdb.core.security.MasterKeyCipher
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

class EncryptedDataStoreTest {
    private val fileSystem = FileSystem.SYSTEM
    private val directory: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "encrypted-datastore-${Random.nextLong()}"
    private val path = directory / "stored.json.enc"
    private val cipher = PassThroughCipher()

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
    fun corruptFileResetsToDefault() = runTest {
        fileSystem.createDirectories(directory)
        fileSystem.write(path) { write(Random.nextBytes(64)) }

        withStore { store -> assertEquals(Stored(), store.data.first()) }
    }

    @Test
    fun writeFailurePropagatesAndKeepsPreviousValue() = runTest {
        withStore { store -> store.updateData { it.copy(flag = true) } }
        val failingCipher = PassThroughCipher(encryptFailure = CipherException("Keystore unavailable"))

        withStore(failingCipher) { store ->
            assertFailsWith<CipherException> { store.updateData { it.copy(name = "lost") } }
            assertEquals(Stored(flag = true), store.data.first())
        }
    }

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

    @Serializable
    private data class Stored(val flag: Boolean = false, val name: String = "")
}
