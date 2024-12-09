/*
 * This file is part of FlexPlugin. A chat management plugin for Mindustry.
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
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class CachingTranslator(private val translator: Translator, ticker: Ticker = Ticker.systemTicker()) : Translator {
    private val cache =
        Caffeine.newBuilder()
            .expireAfter(TranslationExpiry)
            .maximumSize(1000)
            .ticker(ticker)
            .buildAsync(TranslationLoader(translator))

    override fun translate(
        text: String,
        source: Locale?,
        target: Locale,
    ): CompletableFuture<String> =
        cache.get(TranslationKey(text, source, target))
            .thenCompose {
                when (it) {
                    is TranslationResult.Success -> CompletableFuture.completedFuture(it.translation)
                    is TranslationResult.Failure -> CompletableFuture.failedFuture(it.throwable)
                }
            }

    override fun isSupportedLanguage(locale: Locale): Boolean = translator.isSupportedLanguage(locale)

    private class TranslationLoader(private val translator: Translator) : AsyncCacheLoader<TranslationKey, TranslationResult> {
        override fun asyncLoad(
            key: TranslationKey,
            executor: Executor,
        ): CompletableFuture<TranslationResult> =
            translator.translate(key.text, key.source, key.target)
                .thenApply<TranslationResult> { TranslationResult.Success(it) }
                .exceptionally { TranslationResult.Failure(it) }
    }

    private data object TranslationExpiry : Expiry<TranslationKey, TranslationResult> {
        private val FIVE_MINUTES_AS_NANOS = 5.minutes.inWholeNanoseconds
        private val FIVE_SECONDS_AS_NANOS = 5.seconds.inWholeNanoseconds

        override fun expireAfterCreate(
            key: TranslationKey,
            value: TranslationResult,
            currentTime: Long,
        ) = when (value) {
            is TranslationResult.Success -> FIVE_MINUTES_AS_NANOS
            is TranslationResult.Failure -> FIVE_SECONDS_AS_NANOS
        }

        override fun expireAfterUpdate(
            key: TranslationKey,
            value: TranslationResult,
            currentTime: Long,
            currentDuration: Long,
        ) = when (value) {
            is TranslationResult.Success -> FIVE_MINUTES_AS_NANOS
            is TranslationResult.Failure -> FIVE_SECONDS_AS_NANOS
        }

        override fun expireAfterRead(
            key: TranslationKey,
            value: TranslationResult,
            currentTime: Long,
            currentDuration: Long,
        ) = when (value) {
            is TranslationResult.Success -> FIVE_MINUTES_AS_NANOS
            is TranslationResult.Failure -> currentDuration
        }
    }
}
