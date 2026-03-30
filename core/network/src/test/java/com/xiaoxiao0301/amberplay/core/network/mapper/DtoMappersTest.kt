package com.xiaoxiao0301.amberplay.core.network.mapper

import com.xiaoxiao0301.amberplay.core.network.dto.LyricDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DtoMappersTest {

    @Test
    fun `parseLrc parses standard two-digit milliseconds`() {
        val dto = LyricDto(lyric = "[00:01.23] Hello world\n[00:05.67] Goodbye", tlyric = null)
        val lyric = dto.toDomain()

        assertEquals(2, lyric.lines.size)
        assertEquals(1_230L, lyric.lines[0].timestampMs)
        assertEquals("Hello world", lyric.lines[0].text)
        assertEquals(5_670L, lyric.lines[1].timestampMs)
        assertEquals("Goodbye", lyric.lines[1].text)
    }

    @Test
    fun `parseLrc parses three-digit milliseconds`() {
        val dto = LyricDto(lyric = "[01:02.345] Song line", tlyric = null)
        val lyric = dto.toDomain()

        assertEquals(1, lyric.lines.size)
        assertEquals(62_345L, lyric.lines[0].timestampMs)
        assertEquals("Song line", lyric.lines[0].text)
    }

    @Test
    fun `parseLrc pads two-digit ms to three digits`() {
        // [00:01.23] should become 1_230ms (23 padded to 230)
        val dto = LyricDto(lyric = "[00:01.23] Padded", tlyric = null)
        val lyric = dto.toDomain()

        assertEquals(1_230L, lyric.lines[0].timestampMs)
    }

    @Test
    fun `parseLrc ignores non-timestamp header lines`() {
        val lrc = "[ti:Song Title]\n[ar:Artist Name]\n[00:01.00] Real line"
        val dto = LyricDto(lyric = lrc, tlyric = null)
        val lyric = dto.toDomain()

        // Only the real timed line should be included
        assertEquals(1, lyric.lines.size)
        assertEquals("Real line", lyric.lines[0].text)
    }

    @Test
    fun `parseLrc sorts lines by timestamp`() {
        val lrc = "[00:05.00] Second\n[00:01.00] First"
        val dto = LyricDto(lyric = lrc, tlyric = null)
        val lyric = dto.toDomain()

        assertEquals("First", lyric.lines[0].text)
        assertEquals("Second", lyric.lines[1].text)
    }

    @Test
    fun `translation is merged at matching timestamp`() {
        val orig  = "[00:01.00] Original text"
        val trans = "[00:01.00] 翻译文本"
        val dto = LyricDto(lyric = orig, tlyric = trans)
        val lyric = dto.toDomain()

        assertEquals("翻译文本", lyric.lines[0].translation)
    }

    @Test
    fun `missing translation yields null`() {
        val dto = LyricDto(lyric = "[00:01.00] Only original", tlyric = null)
        val lyric = dto.toDomain()

        assertNull(lyric.lines[0].translation)
    }

    @Test
    fun `no-match translation yields null for that line`() {
        val orig  = "[00:01.00] Line"
        val trans = "[00:02.00] Different timestamp"
        val dto = LyricDto(lyric = orig, tlyric = trans)
        val lyric = dto.toDomain()

        assertNull(lyric.lines[0].translation)
    }

    @Test
    fun `empty lrc results in empty lines`() {
        val dto = LyricDto(lyric = "", tlyric = null)
        val lyric = dto.toDomain()

        assertEquals(0, lyric.lines.size)
    }

    @Test
    fun `minutes are converted to milliseconds correctly`() {
        val dto = LyricDto(lyric = "[02:00.00] Two minutes", tlyric = null)
        val lyric = dto.toDomain()

        assertEquals(120_000L, lyric.lines[0].timestampMs)
    }
}
