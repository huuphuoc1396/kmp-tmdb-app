package com.banhmilabs.tmdb.service.title.data.list.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.banhmilabs.tmdb.core.common.DispatcherProvider
import com.banhmilabs.tmdb.service.title.data.generated.TitleDatabase
import com.banhmilabs.tmdb.service.title.data.list.TitleListKey
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class TitleListLocalDataSourceTest {
    private val io = newSingleThreadContext(IO_THREAD)
    private val dispatchers = mockk<DispatcherProvider> { every { io } returns this@TitleListLocalDataSourceTest.io }
    private var openedOn: String? = null
    private val database = lazy {
        openedOn = Thread.currentThread().name.substringBefore(" @")
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TitleDatabase.Schema.create(driver)
        TitleDatabase(driver)
    }

    @AfterTest
    fun closeIo() {
        io.close()
    }

    @Test
    fun databaseOpensOnFirstQueryOnIo() = runBlocking {
        val local = TitleListLocalDataSource(database, dispatchers)

        val cached = local.observe(KEY)
        assertNull(openedOn)

        cached.first()
        assertEquals(IO_THREAD, openedOn)
    }

    private companion object {
        const val IO_THREAD = "io"
        val KEY = TitleListKey(TitleCategory.MOVIES, TitleListType.POPULAR, "en-US")
    }
}
