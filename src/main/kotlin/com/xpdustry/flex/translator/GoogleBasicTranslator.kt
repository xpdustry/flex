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

import com.xpdustry.distributor.api.plugin.PluginListener
import com.xpdustry.flex.FlexScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Locale
import java.util.concurrent.CompletableFuture

internal class GoogleBasicTranslator(
    private val apiKey: String,
) : Translator, PluginListener {
    private val http = HttpClient.newHttpClient()
    internal lateinit var supported: Set<Locale>

    override fun onPluginInit() {
        supported = fetchSupportedLanguages()
    }

    override fun translate(
        text: String,
        source: Locale,
        target: Locale,
    ): CompletableFuture<String> =
        FlexScope.future {
            if (source.language == "router" || target.language == "router") {
                return@future "router"
            } else if (text.isBlank() || source.language == target.language) {
                return@future text
            }

            val parameters =
                mutableMapOf(
                    "key" to apiKey,
                    "q" to text,
                    "target" to target.toLanguageTag(),
                    "format" to "text",
                )

            if (source != Translator.AUTO_DETECT) {
                parameters["source"] = source.toLanguageTag()
            }

            val response =
                withContext(Dispatchers.IO) {
                    http.send(
                        HttpRequest.newBuilder(createApiUri(TRANSLATION_V2_ENDPOINT, parameters))
                            .GET()
                            .build(),
                        HttpResponse.BodyHandlers.ofString(),
                    )
                }

            if (response.statusCode() != 200) {
                error("Failed to translate text: ${response.statusCode()}")
            }

            Json.parseToJsonElement(response.body())
                .jsonObject["data"]!!
                .jsonObject["translations"]!!
                .jsonArray
                .first()
                .jsonObject["translatedText"]!!
                .jsonPrimitive
                .content
        }

    override fun isSupportedLanguage(locale: Locale) = locale in supported

    private fun fetchSupportedLanguages(): Set<Locale> {
        val response =
            http.send(
                HttpRequest.newBuilder(createApiUri(TRANSLATION_V2_ENDPOINT, "languages", mapOf("key" to apiKey)))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString(),
            )
        if (response.statusCode() != 200) {
            error("Failed to fetch supported languages: ${response.statusCode()}")
        }
        return Json.parseToJsonElement(response.body())
            .jsonObject["data"]!!
            .jsonObject["languages"]!!
            .jsonArray
            .map { Locale.forLanguageTag(it.jsonObject["language"]!!.jsonPrimitive.content) }
            .toSet()
    }

    companion object {
        private val TRANSLATION_V2_ENDPOINT = URI("https://translation.googleapis.com/language/translate/v2")
    }
}