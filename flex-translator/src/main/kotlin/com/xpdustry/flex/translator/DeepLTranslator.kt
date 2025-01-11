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

import com.deepl.api.Formality
import com.deepl.api.LanguageType
import com.deepl.api.TextTranslationOptions
import com.deepl.api.TranslatorOptions
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

// TODO When implementing custom retries and backoff, use raw deepl API
public class DeepLTranslator(public val apiKey: String, private val executor: Executor) : Translator {
    private val translator = com.deepl.api.Translator(apiKey, TranslatorOptions().setAppInfo("Flex", "v1"))

    internal val sourceLanguages: List<Locale> = fetchLanguages(LanguageType.Source)
    internal val targetLanguages: List<Locale> = fetchLanguages(LanguageType.Target)

    override fun translate(text: String, source: Locale, target: Locale): CompletableFuture<String> {
        if (text.isBlank()) {
            return CompletableFuture.completedFuture(text)
        } else if (source == Translator.ROUTER || target == Translator.ROUTER) {
            return CompletableFuture.completedFuture("router")
        }

        val sourceLocale =
            if (source == Translator.AUTO_DETECT) {
                null
            } else {
                findClosestLanguage(LanguageType.Source, source)
                    ?: return CompletableFuture.failedFuture(UnsupportedLanguageException(source))
            }

        val targetLocale =
            findClosestLanguage(LanguageType.Target, target)
                ?: return CompletableFuture.failedFuture(UnsupportedLanguageException(target))

        if (sourceLocale?.language == targetLocale.language) {
            return CompletableFuture.completedFuture(text)
        }

        return CompletableFuture.runAsync(
                {
                    if (translator.usage.character!!.limitReached()) {
                        throw RateLimitedException()
                    }
                },
                executor,
            )
            .thenApply {
                translator
                    .translateText(text, sourceLocale?.language, targetLocale.toLanguageTag(), DEFAULT_OPTIONS)
                    .text
            }
    }

    private fun findClosestLanguage(type: LanguageType, locale: Locale): Locale? {
        val languages =
            when (type) {
                LanguageType.Source -> sourceLanguages
                LanguageType.Target -> targetLanguages
            }
        val candidates = languages.filter { locale.language == it.language }
        return if (candidates.isEmpty()) {
            null
        } else if (candidates.size == 1) {
            candidates[0]
        } else {
            candidates.find { locale.country == it.country } ?: candidates[0]
        }
    }

    private fun fetchLanguages(type: LanguageType) =
        translator.getLanguages(type).map { Locale.forLanguageTag(it.code) }

    private companion object {
        private val DEFAULT_OPTIONS = TextTranslationOptions().setFormality(Formality.PreferLess)
    }
}
