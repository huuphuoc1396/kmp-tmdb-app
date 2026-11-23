package com.banhmilabs.tmdb.core.datastore

import androidx.datastore.core.DataStore
import com.banhmilabs.tmdb.core.security.MasterKeyCipher
import com.banhmilabs.tmdb.core.security.masterKeyCipher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okio.Buffer
import okio.FileSystem
import okio.Path
import java.security.KeyStore
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class EncryptedDataStoreDeviceTest {
    private val fileSystem = FileSystem.SYSTEM
    private val directory: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "encrypted-datastore-${Random.nextLong()}"
    private val path = directory / "stored.json.enc"
    private val aliases = mutableListOf<String>()
    private val cipher = newCipher()

    @AfterTest
    fun cleanUp() {
        fileSystem.deleteRecursively(directory)
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        aliases.forEach(keyStore::deleteEntry)
    }

    @Test
    fun fileOnDiskDoesNotContainPlaintext() = runTest {
        withStore { store -> store.updateData { it.copy(name = "secret") } }

        assertFalse(fileSystem.read(path) { readByteArray() }.decodeToString().contains("secret"))
    }

    @Test
    fun keyMismatchResetsToDefault() = runTest {
        withStore { store -> store.updateData { it.copy(flag = true) } }

        withStore(newCipher()) { store -> assertEquals(Stored(), store.data.first()) }
    }

    @Test
    fun writtenBytesDoNotContainPlaintext() = runTest {
        val buffer = Buffer()
        EncryptedJsonSerializer(Stored.serializer(), Stored(), cipher).writeTo(Stored(name = "secret"), buffer)

        assertFalse(buffer.readByteArray().decodeToString().contains("secret"))
    }

    private fun newCipher(): MasterKeyCipher =
        masterKeyCipher("EncryptedDataStoreDeviceTest-${Random.nextLong()}".also(aliases::add))

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
