package com.banhmilabs.tmdb.core.database

import kotlin.test.Test
import kotlin.test.assertFailsWith

class RequireNoPassphraseTest {
    @Test
    fun nullPassphraseIsAccepted() {
        requireNoPassphrase(null)
    }

    @Test
    fun nonNullPassphraseIsRejected() {
        assertFailsWith<IllegalArgumentException> { requireNoPassphrase(byteArrayOf(1)) }
    }
}
