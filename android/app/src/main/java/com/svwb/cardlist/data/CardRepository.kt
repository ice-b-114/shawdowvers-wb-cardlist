package com.svwb.cardlist.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class CardRepository(private val context: Context) {

    class RefreshResult(val data: CardData, val source: String)

    /** 先读本地缓存，没有则读随包内置的卡表 */
    suspend fun loadLocal(lang: Lang): CardData? = withContext(Dispatchers.IO) {
        val cache = cacheFile(lang)
        if (cache.exists()) {
            runCatching { return@withContext CardData.parse(lang, cache.readText()) }
        }
        runCatching {
            val text = context.assets.open(fileName(lang)).bufferedReader().use { it.readText() }
            CardData.parse(lang, text)
        }.getOrNull()
    }

    /** 依次尝试主地址与镜像地址，任一成功即写入缓存并返回；全部失败返回 null */
    suspend fun refresh(lang: Lang): RefreshResult? = withContext(Dispatchers.IO) {
        for (source in SOURCES) {
            val url = source.template.format(fileName(lang))
            val text = runCatching { download(url) }.getOrNull() ?: continue
            val data = runCatching { CardData.parse(lang, text) }.getOrNull() ?: continue
            if (data.cards.isEmpty()) continue
            runCatching { writeCache(lang, text) }
            return@withContext RefreshResult(data, source.name)
        }
        null
    }

    private fun download(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Accept-Encoding", "gzip")
            check(conn.responseCode == 200) { "HTTP ${conn.responseCode}" }
            val stream = if (conn.contentEncoding.equals("gzip", true)) {
                java.util.zip.GZIPInputStream(conn.inputStream)
            } else conn.inputStream
            return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun writeCache(lang: Lang, text: String) {
        val target = cacheFile(lang)
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
    }

    private fun cacheFile(lang: Lang) = File(context.filesDir, fileName(lang))

    private fun fileName(lang: Lang) = "cards_${lang.code}.json"

    private class Source(val name: String, val template: String)

    private companion object {
        const val REPO = "ice-b-114/shawdowvers-wb-cardlist"
        val SOURCES = listOf(
            Source("GitHub", "https://raw.githubusercontent.com/$REPO/main/data/%s"),
            Source("jsDelivr 镜像", "https://cdn.jsdelivr.net/gh/$REPO@main/data/%s"),
            Source("jsDelivr Fastly 镜像", "https://fastly.jsdelivr.net/gh/$REPO@main/data/%s"),
            Source("jsDelivr Gcore 镜像", "https://gcore.jsdelivr.net/gh/$REPO@main/data/%s"),
        )
    }
}
