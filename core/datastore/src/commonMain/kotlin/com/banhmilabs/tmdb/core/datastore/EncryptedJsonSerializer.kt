package com.banhmilabs.tmdb.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import com.banhmilabs.tmdb.core.security.CipherException
import com.banhmilabs.tmdb.core.security.MasterKeyCipher
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource

/** Unknown keys are ignored so a file written by a newer build still loads. */
private val json = Json { ignoreUnknownKeys = true }

/** Stores [T] as JSON encrypted with [cipher]; unreadable data is reported as a [CorruptionException]. */
internal class EncryptedJsonSerializer<T>(
    private val serializer: KSerializer<T>,
    override val defaultValue: T,
    private val cipher: MasterKeyCipher,
) : OkioSerializer<T> {
    override suspend fun readFrom(source: BufferedSource): T {
        val bytes = source.readByteArray()
        val failure = try {
            return json.decodeFromString(serializer, cipher.decrypt(bytes).decodeToString())
        } catch (e: CipherException) {
            e
        } catch (e: SerializationException) {
            e
        } catch (e: IllegalArgumentException) {
            e
        }
        throw CorruptionException("Stored data can't be decrypted or parsed", failure)
    }

    override suspend fun writeTo(t: T, sink: BufferedSink) {
        sink.write(cipher.encrypt(json.encodeToString(serializer, t).encodeToByteArray()))
    }
}
