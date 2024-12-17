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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class CachingTranslatorTest {
    @Test
    fun `test simple`() {
        val translator = TestTranslator()

        val key1 = TranslationKey("hello", Locale.ENGLISH, Locale.FRENCH)
        val val1 = TranslationResult.Success("bonjour")
        val key2 = TranslationKey("hello", Locale.ENGLISH, Locale.CHINESE)
        val val2 = TranslationResult.Failure(RateLimitedException())

        translator.results[key1] = val1
        translator.results[key2] = val2

        val ticker = NavigableTicker()
        val caching = CachingTranslator(translator, 1000, 5.minutes, 5.seconds, ticker)

        Assertions.assertEquals(val1.translation, caching.translate(key1.text, key1.source, key1.target).join())
        Assertions.assertEquals(1, translator.successCount)

        assertThrowsCompletable(RateLimitedException::class, caching.translate(key2.text, key2.source, key2.target))
        Assertions.assertEquals(1, translator.failureCount)

        ticker += 1.seconds

        Assertions.assertEquals(val1.translation, caching.translate(key1.text, key1.source, key1.target).join())
        Assertions.assertEquals(1, translator.successCount)

        assertThrowsCompletable(RateLimitedException::class, caching.translate(key2.text, key2.source, key2.target))
        Assertions.assertEquals(1, translator.failureCount)

        ticker += 5.seconds

        Assertions.assertEquals(val1.translation, caching.translate(key1.text, key1.source, key1.target).join())
        Assertions.assertEquals(1, translator.successCount)

        assertThrowsCompletable(RateLimitedException::class, caching.translate(key2.text, key2.source, key2.target))
        Assertions.assertEquals(2, translator.failureCount)

        ticker += 4.minutes

        Assertions.assertEquals(val1.translation, caching.translate(key1.text, key1.source, key1.target).join())
        Assertions.assertEquals(1, translator.successCount)

        ticker += 2.minutes

        Assertions.assertEquals(val1.translation, caching.translate(key1.text, key1.source, key1.target).join())
        Assertions.assertEquals(1, translator.successCount)

        ticker += 6.minutes

        Assertions.assertEquals(val1.translation, caching.translate(key1.text, key1.source, key1.target).join())
        Assertions.assertEquals(2, translator.successCount)
    }

    private fun assertThrowsCompletable(
        exception: KClass<out Throwable>,
        future: CompletableFuture<*>,
    ) {
        try {
            future.join()
        } catch (e: CompletionException) {
            Assertions.assertEquals(exception, e.cause!!::class)
            return
        }
        Assertions.fail<Any>("Expected exception of type $exception")
    }
}
