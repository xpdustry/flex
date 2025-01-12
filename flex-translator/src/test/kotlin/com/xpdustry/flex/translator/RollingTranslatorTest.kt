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

import java.util.Locale
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class RollingTranslatorTest {
    @Test
    fun `test rolling`() {
        val translator1 = TestTranslator()
        val val1 = translationSuccess("Salut")
        translator1.results[TranslationKey("Hello", Locale.ENGLISH, Locale.FRENCH)] = val1
        val translator2 = TestTranslator()
        val val2 = translationSuccess("Bonjour")
        translator2.results[TranslationKey("Hello", Locale.ENGLISH, Locale.FRENCH)] = val2
        val rolling = RollingTranslator(listOf(translator1, translator2), Translator.noop())

        Assertions.assertEquals(
            val1.getOrThrow(),
            rolling.translateDetecting("Hello", Locale.ENGLISH, Locale.FRENCH).join(),
        )
        Assertions.assertEquals(
            val2.getOrThrow(),
            rolling.translateDetecting("Hello", Locale.ENGLISH, Locale.FRENCH).join(),
        )
        Assertions.assertEquals(
            val1.getOrThrow(),
            rolling.translateDetecting("Hello", Locale.ENGLISH, Locale.FRENCH).join(),
        )
        Assertions.assertEquals(
            val2.getOrThrow(),
            rolling.translateDetecting("Hello", Locale.ENGLISH, Locale.FRENCH).join(),
        )

        Assertions.assertEquals(2, translator1.successCount)
        Assertions.assertEquals(0, translator1.failureCount)
        Assertions.assertEquals(2, translator2.successCount)
        Assertions.assertEquals(0, translator2.failureCount)
    }

    @Test
    fun `test failure`() {
        val translator1 = TestTranslator()
        val val1 = translationFailure(RateLimitedException())
        translator1.results[TranslationKey("Hello", Locale.ENGLISH, Locale.FRENCH)] = val1
        val translator2 = TestTranslator()
        val val2 = translationSuccess("Bonjour")
        translator2.results[TranslationKey("Hello", Locale.ENGLISH, Locale.FRENCH)] = val2
        val rolling = RollingTranslator(listOf(translator1, translator2), Translator.noop())

        Assertions.assertEquals(
            val2.getOrThrow(),
            rolling.translateDetecting("Hello", Locale.ENGLISH, Locale.FRENCH).join(),
        )
        Assertions.assertEquals(
            val2.getOrThrow(),
            rolling.translateDetecting("Hello", Locale.ENGLISH, Locale.FRENCH).join(),
        )

        Assertions.assertEquals(0, translator1.successCount)
        Assertions.assertEquals(1, translator1.failureCount)

        Assertions.assertEquals(2, translator2.successCount)
        Assertions.assertEquals(0, translator2.failureCount)
    }

    @Test
    fun `test fallback`() {
        val translator1 = TestTranslator()
        translator1.results[TranslationKey("Hello", Locale.ENGLISH, Locale.FRENCH)] =
            translationFailure(RateLimitedException())
        val translator2 = TestTranslator()
        translator2.results[TranslationKey("Hello", Locale.ENGLISH, Locale.FRENCH)] =
            translationFailure(RateLimitedException())
        val fallback = TestTranslator()
        val result = translationSuccess("Bonjour")
        fallback.results[TranslationKey("Hello", Locale.ENGLISH, Locale.FRENCH)] = result
        val rolling = RollingTranslator(listOf(translator1, translator2), fallback)

        Assertions.assertEquals(
            result.getOrThrow(),
            rolling.translateDetecting("Hello", Locale.ENGLISH, Locale.FRENCH).join(),
        )
        Assertions.assertEquals(
            result.getOrThrow(),
            rolling.translateDetecting("Hello", Locale.ENGLISH, Locale.FRENCH).join(),
        )

        Assertions.assertEquals(0, translator1.successCount)
        Assertions.assertEquals(2, translator1.failureCount)

        Assertions.assertEquals(0, translator2.successCount)
        Assertions.assertEquals(2, translator2.failureCount)

        Assertions.assertEquals(2, fallback.successCount)
        Assertions.assertEquals(0, fallback.failureCount)
    }
}
