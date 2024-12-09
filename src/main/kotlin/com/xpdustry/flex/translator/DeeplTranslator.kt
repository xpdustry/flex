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

import com.deepl.api.Formality
import com.deepl.api.LanguageType
import com.deepl.api.TextTranslationOptions
import com.deepl.api.TranslatorOptions
import com.xpdustry.distributor.api.plugin.PluginListener
import com.xpdustry.flex.FlexScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import kotlinx.coroutines.withContext
import java.time.Duration
import java.util.Locale

internal class DeeplTranslator(config: TranslatorConfig.DeepL, version: String) :
    Translator, PluginListener {
    private val translator =
        com.deepl.api.Translator(
            config.deeplToken.value,
            TranslatorOptions()
                .setTimeout(Duration.ofSeconds(3L))
                .setAppInfo("Flex", version),
        )

    private lateinit var sourceLanguages: List<Locale>
    private lateinit var targetLanguages: List<Locale>

    override fun onPluginInit() {
        sourceLanguages = fetchLanguages(LanguageType.Source)
        targetLanguages = fetchLanguages(LanguageType.Target)
    }

    override fun translate(
        text: String,
        source: Locale?,
        target: Locale,
    ) = FlexScope.future {
        if (text.isBlank()) {
            return@future text
        } else if (source?.language == "router" || target.language == "router") {
            return@future "router"
        }

        val sourceLocale =
            if (source == null) {
                null
            } else {
                findClosestLanguage(LanguageType.Source, source)
                    ?: throw UnsupportedLanguageException(source)
            }

        val targetLocale =
            findClosestLanguage(LanguageType.Target, target)
                ?: throw UnsupportedLanguageException(target)

        if (sourceLocale?.language == targetLocale.language) {
            return@future text
        }

        if (fetchRateLimited()) {
            throw RateLimitedException()
        }

        withContext(Dispatchers.IO) {
            translator.translateText(text, sourceLocale?.language, targetLocale.toLanguageTag(), DEFAULT_OPTIONS).text
        }
    }

    override fun isSupportedLanguage(locale: Locale): Boolean = findClosestLanguage(LanguageType.Source, locale) != null

    private fun findClosestLanguage(
        type: LanguageType,
        locale: Locale,
    ): Locale? {
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

    private fun fetchLanguages(type: LanguageType) = translator.getLanguages(type).map { Locale.forLanguageTag(it.code) }

    private suspend fun fetchRateLimited() =
        withContext(Dispatchers.IO) {
            translator.usage.character!!.limitReached()
        }

    companion object {
        private val DEFAULT_OPTIONS = TextTranslationOptions().setFormality(Formality.PreferLess)
    }
}
