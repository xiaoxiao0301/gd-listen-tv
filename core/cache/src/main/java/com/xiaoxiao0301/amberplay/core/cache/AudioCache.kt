package com.xiaoxiao0301.amberplay.core.cache

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 音频文件磁盘缓存（LRU，基于文件 lastModified 时间戳）。
 *
 * Key："{source}_{trackId}_{br}"  → 文件名 "{key}.cache"
 * 缓存目录：{filesDir}/audio_cache/
 * 容量由 [updateLimit] 动态调整，默认 1 GB。
 */
@Singleton
class AudioCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    private val cacheDir = File(context.filesDir, "audio_cache").also { it.mkdirs() }

    @Volatile
    private var limitBytes: Long = DEFAULT_LIMIT_BYTES

    companion object {
        private const val TAG = "AudioCache"
        const val DEFAULT_LIMIT_BYTES = 1L * 1024 * 1024 * 1024  // 1 GB
    }

    // ─── 公开 API ─────────────────────────────────────────────────────────────

    /** 检查缓存是否命中 */
    fun has(source: String, trackId: String, br: Int): Boolean =
        fileFor(source, trackId, br).exists()

    /**
     * 返回缓存文件（供 ExoPlayer 直接以 file:// URI 播放）。
     * 不存在则返回 null。同时 touch lastModified 以维持 LRU 顺序。
     */
    fun getFile(source: String, trackId: String, br: Int): File? {
        val f = fileFor(source, trackId, br)
        if (!f.exists()) return null
        f.setLastModified(System.currentTimeMillis())
        return f
    }

    /**
     * 下载 [url] 并存入缓存，返回本地缓存文件。
     * 失败返回 null（不抛出异常）。
     * 此方法会阻塞调用线程，应在 Dispatchers.IO 上调用。
     */
    fun downloadAndCache(source: String, trackId: String, br: Int, url: String): File? =
        runCatching {
            getFile(source, trackId, br)?.let { return it }

            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body ?: return null
                val bytes = body.bytes()
                evictIfNeeded(bytes.size.toLong())
                val tmp = File(cacheDir, "${key(source, trackId, br)}.tmp")
                tmp.writeBytes(bytes)
                val dest = fileFor(source, trackId, br)
                tmp.renameTo(dest)
                dest.takeIf { it.exists() }
            }
        }.onFailure { Log.w(TAG, "Cache download error for $source/$trackId/$br", it) }
         .getOrNull()

    /** 当前缓存占用字节数 */
    fun usedBytes(): Long = cacheDir.listFiles()
        ?.filter { it.extension == "cache" }
        ?.sumOf { it.length() }
        ?: 0L

    /** 清除全部缓存文件 */
    fun clear() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    /** 更新最大缓存容量（MB），超限时立即触发 LRU 驱逐 */
    fun updateLimit(limitMb: Int) {
        limitBytes = limitMb.toLong() * 1024 * 1024
        evictIfNeeded(0L)
    }

    // ─── 内部 ─────────────────────────────────────────────────────────────────

    private fun fileFor(source: String, trackId: String, br: Int) =
        File(cacheDir, "${key(source, trackId, br)}.cache")

    private fun key(source: String, trackId: String, br: Int) =
        "${source}_${trackId}_${br}".lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")
            .take(120)

    private fun evictIfNeeded(incoming: Long) {
        if (limitBytes <= 0L) return
        val files = cacheDir.listFiles()
            ?.filter { it.extension == "cache" }
            ?.sortedBy { it.lastModified() }
            ?: return

        var used = files.sumOf { it.length() }
        for (f in files) {
            if (used + incoming <= limitBytes) break
            used -= f.length()
            if (!f.delete()) Log.w(TAG, "Failed to evict ${f.name}")
        }
    }
}
