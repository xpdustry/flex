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

internal class TestTranslator : Translator {
    val results = mutableMapOf<TranslationKey, Result<TranslatedText>>()

    var successCount = 0
        private set

    var failureCount = 0
        private set

    @Deprecated("Deprecated", ReplaceWith("translateDetecting(text, source, target)"))
    override fun translate(text: String, source: Locale, target: Locale): CompletableFuture<String> =
        translateDetecting(text, source, target).thenApply(TranslatedText::text)

    override fun translateDetecting(text: String, source: Locale, target: Locale): CompletableFuture<TranslatedText> =
        results[TranslationKey(text, source, target)]?.fold(
            {
                successCount++
                CompletableFuture.completedFuture(it)
            },
            {
                failureCount++
                CompletableFuture.failedFuture(it)
            },
        ) ?: CompletableFuture.failedFuture(UnsupportedOperationException("No result for $text"))
}
