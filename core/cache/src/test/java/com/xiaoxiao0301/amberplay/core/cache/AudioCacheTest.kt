package com.xiaoxiao0301.amberplay.core.cache

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class AudioCacheTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var context: Context
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var cache: AudioCache

    private val audioBytes = ByteArray(1024) { it.toByte() }

    @BeforeEach
    fun setUp() {
        context = mockk {
            every { filesDir } returns tempDir
        }
        okHttpClient = mockk()
        cache = AudioCache(context, okHttpClient)
    }

    private fun successResponse(bytes: ByteArray = audioBytes): Response {
        val request = okhttp3.Request.Builder().url("https://example.com/song.mp3").build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(bytes.toResponseBody("audio/mpeg".toMediaType()))
            .build()
    }

    private fun mockOkHttpCall(response: Response) {
        val call = mockk<okhttp3.Call> {
            every { execute() } returns response
        }
        every { okHttpClient.newCall(any()) } returns call
    }

    // ─── download and cache ─────────────────────────────────────────

    @Test
    fun `downloadAndCache writes file and returns it`() {
        mockOkHttpCall(successResponse())

        val result = cache.downloadAndCache("netease", "123", 320, "https://example.com/song.mp3")

        assertNotNull(result)
        assert(result!!.exists())
        assertEquals(audioBytes.size.toLong(), result.length())
    }

    @Test
    fun `downloadAndCache returns cached file without re-downloading on second call`() {
        mockOkHttpCall(successResponse())
        cache.downloadAndCache("netease", "123", 320, "https://example.com/song.mp3")

        // Second call should NOT hit network (call count should still be 1)
        val secondResult = cache.downloadAndCache("netease", "123", 320, "https://example.com/song.mp3")
        assertNotNull(secondResult)
        // OkHttpClient.newCall should only have been called once
        io.mockk.verify(exactly = 1) { okHttpClient.newCall(any()) }
    }

    @Test
    fun `downloadAndCache returns null on HTTP error`() {
        val request = okhttp3.Request.Builder().url("https://example.com/song.mp3").build()
        val errorResponse = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(404)
            .message("Not Found")
            .body(ByteArray(0).toResponseBody())
            .build()
        mockOkHttpCall(errorResponse)

        val result = cache.downloadAndCache("netease", "999", 320, "https://example.com/song.mp3")
        assertNull(result)
    }

    // ─── usedBytes (from in-memory index) ──────────────────────────

    @Test
    fun `usedBytes reflects downloaded file size`() {
        mockOkHttpCall(successResponse())
        cache.downloadAndCache("netease", "abc", 128, "https://example.com/song.mp3")

        assertEquals(audioBytes.size.toLong(), cache.usedBytes())
    }

    @Test
    fun `clear resets usedBytes to zero`() {
        mockOkHttpCall(successResponse())
        cache.downloadAndCache("netease", "abc", 128, "https://example.com/song.mp3")
        cache.clear()

        assertEquals(0L, cache.usedBytes())
    }

    // ─── LRU eviction ──────────────────────────────────────────────

    @Test
    fun `updateLimit triggers eviction when over limit`() {
        // Write 3 small files
        for (i in 1..3) {
            mockOkHttpCall(successResponse(ByteArray(400)))
            cache.downloadAndCache("netease", "song$i", 128, "https://example.com/s$i.mp3")
        }
        // 3 × 400 = 1200 bytes. Set limit to 600 bytes → should evict at least 1 file
        cache.updateLimit(1)   // 1 MB is fine; use direct byte limit via reflection or just check eviction
        // Instead test that updateLimit is called and cache size stays within limit
        // Here we set a tiny limit via updateLimit(0) which maps to 0 MB → 0 bytes
        cache.updateLimit(0)
        // After evicting everything, used bytes should be 0
        assertEquals(0L, cache.usedBytes())
    }

    // ─── Concurrent download safety ────────────────────────────────

    @Test
    fun `concurrent downloads of same key do not corrupt cache`() {
        val latch = CountDownLatch(1)
        val call = mockk<okhttp3.Call> {
            every { execute() } answers {
                latch.await()   // hold until all threads have started
                successResponse()
            }
        }
        every { okHttpClient.newCall(any()) } returns call

        val executor = Executors.newFixedThreadPool(4)
        val futures = (1..4).map {
            executor.submit<File?> {
                cache.downloadAndCache("netease", "concurrent", 320, "https://example.com/c.mp3")
            }
        }

        latch.countDown()  // release all threads
        val results = futures.map { it.get() }
        executor.shutdown()

        // At least one result should be a valid file
        assert(results.any { it != null && it.exists() })
        // Cache must not contain more than one entry for the same key
        val cacheFiles = tempDir.walkTopDown()
            .filter { it.name.contains("concurrent") && it.extension == "cache" }
            .toList()
        assert(cacheFiles.size <= 1) { "Expected at most 1 cache file for concurrent key, got ${cacheFiles.size}" }
    }
}
