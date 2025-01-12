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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

public class RollingTranslator(public val translators: List<Translator>, public val fallback: Translator) : Translator {
    private val cursor = AtomicInteger(0)

    @Deprecated("Deprecated", ReplaceWith("translateDetecting(text, source, target)"))
    override fun translate(text: String, source: Locale, target: Locale): CompletableFuture<String> =
        translateDetecting(text, source, target).thenApply(TranslatedText::text)

    override fun translateDetecting(text: String, source: Locale, target: Locale): CompletableFuture<TranslatedText> {
        val cursor = cursor.getAndUpdate { if (it + 1 < translators.size) it + 1 else 0 }
        return translate0(text, source, target, cursor, 0)
    }

    private fun translate0(
        text: String,
        source: Locale,
        target: Locale,
        cursor: Int,
        index: Int,
    ): CompletableFuture<TranslatedText> {
        if (index >= translators.size) return fallback.translateDetecting(text, source, target)
        val translator = translators[(cursor + index) % translators.size]
        return translator.translateDetecting(text, source, target).exceptionallyCompose { throwable ->
            logger.log(System.Logger.Level.DEBUG, "Translator {0} failed", translator.javaClass.simpleName, throwable)
            translate0(text, source, target, cursor, index + 1)
        }
    }

    private companion object {
        @JvmStatic private val logger = System.getLogger(RollingTranslator::class.java.name)
    }
}
