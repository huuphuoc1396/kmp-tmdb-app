package com.banhmilabs.tmdb.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import com.banhmilabs.tmdb.core.security.MasterKeyCipher
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import okio.FileSystem
import okio.Path

/**
 * DataStore of [T] stored at [path] as JSON encrypted with [cipher]. A file that can't be decrypted or parsed is
 * replaced by [defaultValue]. Create one instance per [path] per process.
 */
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
