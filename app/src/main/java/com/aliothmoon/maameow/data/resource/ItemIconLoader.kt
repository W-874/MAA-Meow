package com.aliothmoon.maameow.data.resource

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.aliothmoon.maameow.data.config.MaaPathConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ItemIconLoader(
    private val pathConfig: MaaPathConfig,
) {
    private val maxCacheBytes = minOf(
        24L * 1024 * 1024,
        Runtime.getRuntime().maxMemory() / 16,
    ).coerceAtLeast(1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    private val cache = object : LruCache<String, Bitmap>(maxCacheBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private val loadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()
    private val missing = ConcurrentHashMap.newKeySet<String>()
    private val versionFile = File(pathConfig.resourceDir, "version.json")
    private var resourceVersionToken = Long.MIN_VALUE
    private var lastVersionCheckMs = 0L

    suspend fun load(itemId: String, targetSizePx: Int = 0): ImageBitmap? {
        invalidateIfResourceChanged()
        val cacheKey = "$itemId@$targetSizePx"
        getCached(cacheKey)?.let { return it }
        if (itemId in missing) return null

        val deferred = inFlight.computeIfAbsent(cacheKey) {
            loadScope.async { loadInternal(itemId, cacheKey, targetSizePx) }
        }
        return try {
            deferred.await()
        } finally {
            inFlight.remove(cacheKey, deferred)
        }
    }

    private suspend fun loadInternal(
        itemId: String,
        cacheKey: String,
        targetSizePx: Int,
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        getCached(cacheKey)?.let { return@withContext it }
        if (itemId in missing) return@withContext null

        val file = File(pathConfig.resourceDir, "template/items/$itemId.png")
        if (!file.isFile) {
            missing.add(itemId)
            return@withContext null
        }

        runCatching { decodeAndProcess(file, targetSizePx) }
            .onFailure { Timber.w(it, "加载物品图标失败: $itemId") }
            .getOrNull()
            ?.also { bitmap -> synchronized(cache) { cache.put(cacheKey, bitmap) } }
            ?.asImageBitmap()
            ?: run {
                missing.add(itemId)
                null
            }
    }

    private fun getCached(cacheKey: String): ImageBitmap? = synchronized(cache) {
        cache.get(cacheKey)?.asImageBitmap()
    }

    private fun decodeAndProcess(file: File, targetSizePx: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
        }
        if (targetSizePx > 0) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val ratio = (maxOf(bounds.outWidth, bounds.outHeight) / targetSizePx).coerceAtLeast(1)
            options.inSampleSize = Integer.highestOneBit(ratio)
        }

        val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        val bitmap = if (targetSizePx > 0 && maxOf(decoded.width, decoded.height) > targetSizePx) {
            val scale = targetSizePx.toFloat() / maxOf(decoded.width, decoded.height)
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true,
            ).also { if (it !== decoded) decoded.recycle() }
        } else {
            decoded
        }

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (index in pixels.indices) {
            if (pixels[index] and 0x00FFFFFF == 0) pixels[index] = 0
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun invalidateIfResourceChanged() {
        val now = SystemClock.elapsedRealtime()
        synchronized(this) {
            if (now - lastVersionCheckMs < 1_000L) return
            lastVersionCheckMs = now
            val token = versionFile.lastModified() xor versionFile.length()
            if (resourceVersionToken == Long.MIN_VALUE) {
                resourceVersionToken = token
            } else if (resourceVersionToken != token) {
                resourceVersionToken = token
                synchronized(cache) { cache.evictAll() }
                missing.clear()
            }
        }
    }
}
