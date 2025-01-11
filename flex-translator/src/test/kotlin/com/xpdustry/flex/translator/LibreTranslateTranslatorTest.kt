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

import java.net.URI
import java.util.Locale
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

class LibreTranslateTranslatorTest {
    @EnabledIfEnvironmentVariable(named = ENDPOINT_ENV, matches = ".+")
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    @Test
    fun test() {
        val translator = assertDoesNotThrowsAndReturns {
            LibreTranslateTranslator(URI(System.getenv(ENDPOINT_ENV)), Runnable::run, System.getenv(API_KEY_ENV))
        }
        Assertions.assertTrue(translator.languages.isNotEmpty())
        Assertions.assertTrue(translator.languages.values.flatten().isNotEmpty())
        Assertions.assertDoesNotThrow { translator.translate("Bonjour", Locale.FRENCH, Locale.ENGLISH).join() }
    }

    companion object {
        private const val ENDPOINT_ENV = "FLEX_TEST_TRANSLATOR_LT_ENDPOINT"
        private const val API_KEY_ENV = "FLEX_TEST_TRANSLATOR_LT_API_KEY"
    }
}
