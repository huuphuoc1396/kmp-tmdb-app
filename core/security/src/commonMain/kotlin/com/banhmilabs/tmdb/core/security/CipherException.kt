package com.banhmilabs.tmdb.core.security

/** Encryption, decryption or key storage failed. */
class CipherException(message: String, cause: Throwable? = null) : Exception(message, cause)
