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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.util.Locale

class DeepLTranslatorTest {
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    @Test
    fun test() {
        val translator = DeepLTranslator(System.getenv(API_KEY_ENV))
        Assertions.assertDoesNotThrow { translator.onPluginInit() }
        Assertions.assertTrue(translator.sourceLanguages.isNotEmpty())
        Assertions.assertTrue(translator.targetLanguages.isNotEmpty())
        Assertions.assertEquals("Hello", translator.translate("Bonjour", Locale.FRENCH, Locale.ENGLISH).join())
    }

    companion object {
        private const val API_KEY_ENV = "FLEX_TEST_TRANSLATOR_DEEPL_API_KEY"
    }
}
