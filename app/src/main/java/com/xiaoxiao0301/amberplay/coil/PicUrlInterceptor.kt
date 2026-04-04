package com.xiaoxiao0301.amberplay.coil

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject

/**
 * OkHttp application interceptor used exclusively by Coil's ImageLoader.
 *
 * The music-api pic endpoint returns JSON instead of an image:
 *   GET /api.php?types=pic&source=netease&id=XXX&size=300
 *   → {"url":"https://p2.music.126.net/.../XXX.jpg?param=300y300","from":"..."}
 *
 * This interceptor detects that case, parses the json, and performs a second
 * request to the real CDN URL so Coil receives raw image bytes it can decode.
 *
 * Being an APPLICATION interceptor (addInterceptor, not addNetworkInterceptor)
 * it is allowed to call chain.proceed() more than once.
 */
class PicUrlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()

        // Only handle API pic calls — pass everything else straight through
        if (req.url.queryParameter("types") != "pic") {
            return chain.proceed(req)
        }

        val metaResp = chain.proceed(req)
        if (!metaResp.isSuccessful) return metaResp

        val jsonString = metaResp.peekBody(8192).string()
        metaResp.close()

        return try {
            val realUrl = JSONObject(jsonString).getString("url")
            val imageReq = req.newBuilder().url(realUrl).build()
            chain.proceed(imageReq)
        } catch (_: Exception) {
            // Return a valid but empty response so Coil shows the placeholder
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(204)
                .message("Pic URL resolve failed")
                .body(ByteArray(0).toResponseBody("image/jpeg".toMediaType()))
                .build()
        }
    }
}
