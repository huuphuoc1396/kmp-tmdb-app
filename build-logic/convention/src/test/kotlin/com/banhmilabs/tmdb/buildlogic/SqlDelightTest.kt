package com.banhmilabs.tmdb.buildlogic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SqlDelightTest {
    @Test
    fun databaseNameIsContextPlusDatabase() {
        assertEquals("TitleDatabase", sqlDelightDatabaseName(":service:title:data"))
    }

    @Test
    fun databaseNameOfFavorites() {
        assertEquals("FavoritesDatabase", sqlDelightDatabaseName(":service:favorites:data"))
    }
}
