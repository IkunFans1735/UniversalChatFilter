package io.wdsj.asw.bukkit.util.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.setting.PluginSettings
import java.util.concurrent.TimeUnit

object BookCache {
    private lateinit var cache: Cache<String, BookCacheEntry>
    fun isBookCached(content: String): Boolean {
        return cache.getIfPresent(content) != null
    }

    fun addToBookCache(content: String, processedContent: String, sensitiveWordList: List<String>) {
        cache.put(content, BookCacheEntry(processedContent, sensitiveWordList))
    }

    /**
     * Retrieves the processed book content from the cache.
     * Warning: This method is only safely called after checking if the book is cached.
     * @param content The content of the book.
     * @return The processed book content.
     */
    @Throws(NoSuchElementException::class)
    fun getCachedProcessedBookContent(content: String): String {
        val entry = cache.getIfPresent(content) ?: throw NoSuchElementException("Book not found in cache")
        return entry.processedContent
    }

    /**
     * Retrieves the list of sensitive words from the cache.
     * Warning: This method is only safely called after checking if the book is cached.
     * @param content The content of the book.
     * @return The list of sensitive words.
     */
    @Throws(NoSuchElementException::class)
    fun getCachedBookSensitiveWordList(content: String): List<String> {
        val entry = cache.getIfPresent(content) ?: throw NoSuchElementException("Book not found in cache")
        return entry.sensitiveWordList
    }

    @JvmStatic
    fun invalidateAll() {
        cache.invalidateAll()
    }

    @JvmStatic
    fun initialize() {
        cache = Caffeine.newBuilder()
            .maximumSize(
                settingsManager.getProperty(PluginSettings.BOOK_MAXIMUM_CACHE_SIZE).toLong()
            )
            .expireAfterWrite(
                settingsManager.getProperty(PluginSettings.BOOK_CACHE_EXPIRE_TIME).toLong(),
                TimeUnit.MINUTES
            )
            .build()
    }


    /**
     * Inner class to encapsulate the processed book content and its list of sensitive words.
     */
    private class BookCacheEntry(val processedContent: String, val sensitiveWordList: List<String>)
}