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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Locale

class RollingTranslatorTest {
    @Test
    fun `test rolling`() {
        val translator1 = TestTranslator()
        translator1.results[TranslationKey("Hello", Locale.ENGLISH, Locale.FRENCH)] = TranslationResult.Success("Salut")
        val translator2 = TestTranslator()
        translator2.results[TranslationKey("Hello", Locale.ENGLISH, Locale.FRENCH)] = TranslationResult.Success("Bonjour")
        val rolling = RollingTranslator(listOf(translator1, translator2), Translator.None)

        Assertions.assertEquals("Salut", rolling.translate("Hello", Locale.ENGLISH, Locale.FRENCH).join())
        Assertions.assertEquals("Bonjour", rolling.translate("Hello", Locale.ENGLISH, Locale.FRENCH).join())
        Assertions.assertEquals("Salut", rolling.translate("Hello", Locale.ENGLISH, Locale.FRENCH).join())
        Assertions.assertEquals("Bonjour", rolling.translate("Hello", Locale.ENGLISH, Locale.FRENCH).join())

        Assertions.assertEquals(2, translator1.successCount)
        Assertions.assertEquals(0, translator1.failureCount)
        Assertions.assertEquals(2, translator2.successCount)
        Assertions.assertEquals(0, translator2.failureCount)
    }

    @Test
    fun `test failure`() {
        val translator1 = TestTranslator()
        translator1.results[TranslationKey("Hello", Locale.ENGLISH, Locale.FRENCH)] = TranslationResult.Failure(RateLimitedException())
        val translator2 = TestTranslator()
        translator2.results[TranslationKey("Hello", Locale.ENGLISH, Locale.FRENCH)] = TranslationResult.Success("Bonjour")
        val rolling = RollingTranslator(listOf(translator1, translator2), Translator.None)

        Assertions.assertEquals("Bonjour", rolling.translate("Hello", Locale.ENGLISH, Locale.FRENCH).join())
        Assertions.assertEquals("Bonjour", rolling.translate("Hello", Locale.ENGLISH, Locale.FRENCH).join())

        Assertions.assertEquals(0, translator1.successCount)
        Assertions.assertEquals(1, translator1.failureCount)

        Assertions.assertEquals(2, translator2.successCount)
        Assertions.assertEquals(0, translator2.failureCount)
    }

    @Test
    fun `test fallback`() {
        val translator1 = TestTranslator()
        translator1.results[TranslationKey("Hello", Locale.ENGLISH, Locale.FRENCH)] = TranslationResult.Failure(RateLimitedException())
        val translator2 = TestTranslator()
        translator2.results[TranslationKey("Hello", Locale.ENGLISH, Locale.FRENCH)] = TranslationResult.Failure(RateLimitedException())
        val fallback = TestTranslator()
        fallback.results[TranslationKey("Hello", Locale.ENGLISH, Locale.FRENCH)] = TranslationResult.Success("Bonjour")
        val rolling = RollingTranslator(listOf(translator1, translator2), fallback)

        Assertions.assertEquals("Bonjour", rolling.translate("Hello", Locale.ENGLISH, Locale.FRENCH).join())
        Assertions.assertEquals("Bonjour", rolling.translate("Hello", Locale.ENGLISH, Locale.FRENCH).join())

        Assertions.assertEquals(0, translator1.successCount)
        Assertions.assertEquals(2, translator1.failureCount)

        Assertions.assertEquals(0, translator2.successCount)
        Assertions.assertEquals(2, translator2.failureCount)

        Assertions.assertEquals(2, fallback.successCount)
        Assertions.assertEquals(0, fallback.failureCount)
    }
}
