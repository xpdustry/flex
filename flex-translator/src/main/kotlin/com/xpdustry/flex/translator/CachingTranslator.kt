/*
 * This file is part of Flex. An advanced text processing library for Mindustry plugins.
 *
 * MIT License
 *
 * Copyright (c) 2024 Xpdustry
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xpdustry.flex.translator

import com.github.benmanes.caffeine.cache.AsyncCacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.github.benmanes.caffeine.cache.Ticker
import java.time.Duration
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

internal class CachingTranslator
internal constructor(
    val translator: Translator,
    executor: Executor,
    maximumSize: Int,
    private val successRetention: Duration,
    private val failureRetention: Duration,
    ticker: Ticker = Ticker.systemTicker(),
) : BaseTranslator() {
    init {
        require(maximumSize > 0) { "maximumSize must be positive" }
        require(successRetention >= Duration.ZERO) { "successRetention must be non-negative" }
        require(failureRetention >= Duration.ZERO) { "failureRetention must be non-negative" }
    }

    private val cache =
        Caffeine.newBuilder()
            .expireAfter(TranslationExpiry())
            .maximumSize(maximumSize.toLong())
            .ticker(ticker)
            .executor(executor)
            .buildAsync(TranslationLoader())

    override fun translateDetecting(text: String, source: Locale, target: Locale): CompletableFuture<TranslatedText> =
        cache
            .get(TranslationKey(text, Locale.forLanguageTag(source.language), Locale.forLanguageTag(target.language)))
            .thenCompose { result ->
                result.fold({ CompletableFuture.completedFuture(it) }, { CompletableFuture.failedFuture(it) })
            }

    override fun translateDetecting(
        texts: List<String>,
        source: Locale,
        target: Locale,
    ): CompletableFuture<List<TranslatedText>> {
        val sourceLanguage = Locale.forLanguageTag(source.language)
        val targetLanguage = Locale.forLanguageTag(target.language)
        val keys = texts.map { TranslationKey(it, sourceLanguage, targetLanguage) }
        return cache.getAll(keys).thenCompose { map ->
            val list = ArrayList<TranslatedText>(keys.size)
            for (i in keys.indices) {
                val result =
                    map[keys[i]]
                        ?: return@thenCompose CompletableFuture.failedFuture(
                            NullPointerException(
                                "Missing key ${keys[i]} in results for text $texts translating from $source to $target"
                            )
                        )
                list.add(
                    result.getOrNull() ?: return@thenCompose CompletableFuture.failedFuture(result.exceptionOrNull()!!)
                )
            }
            CompletableFuture.completedFuture(list)
        }
    }

    private inner class TranslationLoader : AsyncCacheLoader<TranslationKey, Result<TranslatedText>> {
        override fun asyncLoad(key: TranslationKey, executor: Executor): CompletableFuture<Result<TranslatedText>> =
            translator
                .translateDetecting(key.text, key.source, key.target)
                .thenApply<Result<TranslatedText>> { Result.success(it) }
                .exceptionally { Result.failure(it) }

        override fun asyncLoadAll(
            keys: Set<TranslationKey>,
            executor: Executor,
        ): CompletableFuture<out Map<TranslationKey, Result<TranslatedText>>> {
            val groups =
                keys
                    .groupBy { it.source to it.target }
                    .map { (pair, keys) ->
                        val (source, target) = pair
                        translator.translateDetecting(keys.map(TranslationKey::text), source, target).thenApply {
                            it.mapIndexed { i, t -> keys[i] to Result.success(t) }
                        }
                    }
            return CompletableFuture.allOf(*groups.toTypedArray()).thenApply { _ ->
                groups.map { it.join() }.flatten().toMap()
            }
        }
    }

    private inner class TranslationExpiry : Expiry<TranslationKey, Result<TranslatedText>> {
        override fun expireAfterCreate(key: TranslationKey, value: Result<TranslatedText>, currentTime: Long) =
            when {
                value.isSuccess -> successRetention.toNanos()
                value.isFailure -> failureRetention.toNanos()
                else -> error("Invalid result: $value")
            }

        override fun expireAfterUpdate(
            key: TranslationKey,
            value: Result<TranslatedText>,
            currentTime: Long,
            currentDuration: Long,
        ) =
            when {
                value.isSuccess -> successRetention.toNanos()
                value.isFailure -> failureRetention.toNanos()
                else -> error("Invalid result: $value")
            }

        override fun expireAfterRead(
            key: TranslationKey,
            value: Result<TranslatedText>,
            currentTime: Long,
            currentDuration: Long,
        ) =
            when {
                value.isSuccess -> successRetention.toNanos()
                value.isFailure -> currentDuration
                else -> error("Invalid result: $value")
            }
    }
}
