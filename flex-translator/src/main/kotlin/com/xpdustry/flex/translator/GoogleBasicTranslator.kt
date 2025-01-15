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
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// TODO Add backoff and retries (in case of 429)
internal class GoogleBasicTranslator(private val apiKey: String, executor: Executor) : BaseTranslator() {
    private val http = HttpClient.newBuilder().executor(executor).build()
    internal val supported: Set<Locale> = fetchSupportedLanguages()

    override fun translateDetecting(
        texts: List<String>,
        source: Locale,
        target: Locale,
    ): CompletableFuture<List<TranslatedText>> {
        if (source == Translator.ROUTER || target == Translator.ROUTER) {
            val result = TranslatedText("router")
            return CompletableFuture.completedFuture(List(texts.size) { result })
        }

        var fixedSource = source
        var fixedTarget = target
        if (source != Translator.AUTO_DETECT && source !in supported) {
            fixedSource = Locale.forLanguageTag(source.language)
            if (fixedSource !in supported) {
                throw UnsupportedLanguageException(source)
            }
        } else if (target !in supported) {
            fixedTarget = Locale.forLanguageTag(target.language)
            if (fixedTarget !in supported) {
                throw UnsupportedLanguageException(target)
            }
        }

        if (fixedSource.language == fixedTarget.language) {
            return CompletableFuture.completedFuture(List(texts.size) { i -> TranslatedText(texts[i], fixedSource) })
        }

        val parameters =
            mutableMapOf(
                "key" to apiKey,
                "q" to JsonArray(texts.map(::JsonPrimitive)).toString(),
                "target" to fixedTarget.toLanguageTag(),
                "format" to "text",
            )

        if (fixedSource != Translator.AUTO_DETECT) {
            parameters["source"] = fixedSource.toLanguageTag()
        }

        return http
            .sendAsync(
                HttpRequest.newBuilder(createApiUri(TRANSLATION_V2_ENDPOINT, parameters)).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            .thenCompose { response ->
                if (response.statusCode() != 200) {
                    CompletableFuture.failedFuture(Exception("Failed to translate text: ${response.statusCode()}"))
                } else {
                    CompletableFuture.completedFuture(
                        Json.parseToJsonElement(response.body())
                            .jsonObject["data"]!!
                            .jsonObject["translations"]!!
                            .jsonArray
                            .map { element ->
                                val obj = element.jsonObject
                                TranslatedText(
                                    obj["translatedText"]!!.jsonPrimitive.content,
                                    if (fixedSource == Translator.AUTO_DETECT) {
                                        Locale.forLanguageTag(obj["detectedSourceLanguage"]!!.jsonPrimitive.content)
                                    } else {
                                        fixedSource
                                    },
                                )
                            }
                    )
                }
            }
    }

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

    private companion object {
        @JvmStatic private val TRANSLATION_V2_ENDPOINT = URI("https://translation.googleapis.com/language/translate/v2")
    }
}
