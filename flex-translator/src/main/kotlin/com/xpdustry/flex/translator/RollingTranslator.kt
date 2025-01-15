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

internal class RollingTranslator(private val translators: List<Translator>, private val fallback: Translator) :
    BaseTranslator() {
    private val cursor = AtomicInteger(0)

    override fun translateDetecting(text: String, source: Locale, target: Locale) = roll {
        it.translateDetecting(text, source, target)
    }

    override fun translateDetecting(texts: List<String>, source: Locale, target: Locale) = roll {
        it.translateDetecting(texts, source, target)
    }

    private fun <T : Any> roll(
        idx: Int = 0,
        cur: Int = cursor.getAndUpdate { if (it + 1 < translators.size) it + 1 else 0 },
        function: (Translator) -> CompletableFuture<T>,
    ): CompletableFuture<T> {
        if (idx >= translators.size) return function(fallback)
        val translator = translators[(cur + idx) % translators.size]
        return function(translator).exceptionallyCompose { roll(cur, idx + 1, function) }
    }
}
