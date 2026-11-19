package com.banhmilabs.tmdb.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageTest {
    @Test
    fun ptBrMapsToPtBr() {
        assertEquals(Language.PT_BR, Language.fromLocaleTag("pt-BR"))
    }

    @Test
    fun ptBrIgnoresCaseAndUnderscore() {
        assertEquals(Language.PT_BR, Language.fromLocaleTag("pt_br"))
    }

    @Test
    fun englishMapsToEnUs() {
        assertEquals(Language.EN_US, Language.fromLocaleTag("en-GB"))
    }

    @Test
    fun otherLanguagesMapToEnUs() {
        listOf("es-MX", "pt-PT", "pt", "").forEach { assertEquals(Language.EN_US, Language.fromLocaleTag(it), it) }
    }

    @Test
    fun tagsAreTmdbLanguages() {
        assertEquals(listOf("pt-BR", "en-US"), Language.entries.map { it.tag })
    }
}
