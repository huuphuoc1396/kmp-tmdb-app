package com.banhmilabs.tmdb.nativeapp.titledetails

import com.banhmilabs.tmdb.core.model.Language
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TitleDetailsFormatTest {
    @Test
    fun portugueseDateIsDayMonthYear() {
        assertEquals("10/12/2021", formatReleaseDate(LocalDate(2021, 12, 10), Language.PT_BR))
    }

    @Test
    fun englishDateIsMonthDayYear() {
        assertEquals("12/10/2021", formatReleaseDate(LocalDate(2021, 12, 10), Language.EN_US))
    }

    @Test
    fun datePadsDayAndMonth() {
        assertEquals("05/03/1999", formatReleaseDate(LocalDate(1999, 3, 5), Language.PT_BR))
    }

    @Test
    fun vietnameseDateIsDayThgMonthYear() {
        assertEquals("26 thg 11, 2026", formatReleaseDate(LocalDate(2026, 11, 26), Language.VI_VN))
        assertEquals("5 thg 3, 1999", formatReleaseDate(LocalDate(1999, 3, 5), Language.VI_VN))
    }

    @Test
    fun vietnameseRuntime() {
        assertEquals("2 giờ 10 phút", formatRuntime(130, Language.VI_VN))
        assertEquals("2 giờ", formatRuntime(120, Language.VI_VN))
        assertEquals("45 phút", formatRuntime(45, Language.VI_VN))
    }

    @Test
    fun portugueseRuntimeIsUnchanged() {
        assertEquals("1h 53m", formatRuntime(113, Language.PT_BR))
    }

    @Test
    fun runtimeWithHoursAndMinutes() {
        assertEquals("1h 53m", formatRuntime(113, Language.EN_US))
    }

    @Test
    fun runtimeWholeHours() {
        assertEquals("2h", formatRuntime(120, Language.EN_US))
    }

    @Test
    fun runtimeUnderAnHour() {
        assertEquals("45m", formatRuntime(45, Language.EN_US))
    }

    @Test
    fun nameWithYearAddsYear() {
        assertEquals("Imperdoável\u00A0(2021)", nameWithYear("Imperdoável", 2021))
    }

    @Test
    fun nameWithYearKeepsYearAttached() {
        assertEquals('\u00A0', nameWithYear("A very long name", 1999)[16])
    }

    @Test
    fun nameWithoutYearIsName() {
        assertEquals("Imperdoável", nameWithYear("Imperdoável", null))
    }
}
