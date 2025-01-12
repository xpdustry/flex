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
import java.time.Duration
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

public interface Translator {
    @Deprecated("Deprecated", ReplaceWith("translateDetecting(text, source, target)"))
    public fun translate(text: String, source: Locale, target: Locale): CompletableFuture<String>

    @Suppress("DEPRECATION")
    public fun translateDetecting(text: String, source: Locale, target: Locale): CompletableFuture<TranslatedText> =
        translate(text, source, target).thenApply { TranslatedText(it, target) }

    @Deprecated("Deprecated", ReplaceWith("Translator.noop()"))
    public object None : Translator {
        @Deprecated("Deprecated", ReplaceWith("translateDetecting(text, source, target)"))
        override fun translate(text: String, source: Locale, target: Locale): CompletableFuture<String> =
            CompletableFuture.failedFuture(UnsupportedLanguageException(target))
    }

    public companion object {
        @JvmStatic public val ROUTER: Locale = Locale.forLanguageTag("router")
        @JvmStatic public val AUTO_DETECT: Locale = Locale.forLanguageTag("auto")

        @[JvmStatic Suppress("DEPRECATION")]
        public fun noop(): Translator = None

        @[JvmStatic JvmOverloads]
        public fun caching(
            translator: Translator,
            executor: Executor,
            maximumSize: Int = 1000,
            successRetention: Duration = Duration.ofMinutes(10),
            failureRetention: Duration = Duration.ofSeconds(10),
        ): Translator = CachingTranslator(translator, executor, maximumSize, successRetention, failureRetention)

        @[JvmStatic JvmOverloads]
        public fun libreTranslate(endpoint: URI, executor: Executor, apiKey: String? = null): Translator =
            LibreTranslateTranslator(endpoint, executor, apiKey)

        @[JvmStatic]
        public fun deepl(apiKey: String, executor: Executor): Translator = DeepLTranslator(apiKey, executor)

        @[JvmStatic]
        public fun googleBasic(apiKey: String, executor: Executor): Translator = GoogleBasicTranslator(apiKey, executor)

        @[JvmStatic]
        public fun rolling(translators: List<Translator>, fallback: Translator): Translator =
            RollingTranslator(java.util.List.copyOf(translators), fallback)
    }
}
