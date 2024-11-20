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

import arc.util.serialization.Jval
import com.xpdustry.distributor.api.plugin.PluginListener
import com.xpdustry.flex.FlexScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.Charset
import java.util.Locale
import java.util.concurrent.CompletableFuture

internal class LibreTranslateTranslator(
    private val config: TranslatorConfig.LibreTranslate,
) : Translator, PluginListener {
    private val http = HttpClient.newHttpClient()
    private lateinit var languages: Map<String, Set<String>>

    override fun onPluginInit() {
        runBlocking { languages = fetchSupportedLanguages() }
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

            val targets = languages[source.language]
            if (targets == null) {
                throw UnsupportedLanguageException(source)
            } else if (target.language !in targets) {
                throw UnsupportedLanguageException(target)
            }

            val uri =
                URI(
                    "${config.endpoint}/translate" +
                        "?q=${URLEncoder.encode(text, Charset.defaultCharset())}" +
                        "&source=${source.language}" +
                        "&target=${target.language}" +
                        "&api_key=${config.token.value}" +
                        "&format=text",
                )

            val response =
                withContext(Dispatchers.IO) {
                    http.send(
                        HttpRequest.newBuilder(uri)
                            .header("Accept", "application/json")
                            .POST(BodyPublishers.noBody())
                            .build(),
                        BodyHandlers.ofString(),
                    )
                }

            val json = Jval.read(response.body())
            if (response.statusCode() != 200) {
                throw Exception("Failed to translate: ${json["error"]} (code=${response.statusCode()})")
            } else {
                json["translatedText"]!!.asString()
            }
        }

    override fun isSupportedLanguage(locale: Locale) = locale.language in languages

    private suspend fun fetchSupportedLanguages(): Map<String, Set<String>> {
        val uri = URI("${config.endpoint}/languages")

        val response =
            withContext(Dispatchers.IO) {
                http.send(
                    HttpRequest.newBuilder(uri)
                        .header("Accept", "application/json")
                        .GET()
                        .build(),
                    BodyHandlers.ofString(),
                )
            }

        val json = Jval.read(response.body())
        if (response.statusCode() != 200) {
            throw Exception("Failed to fetch supported languages (code=${response.statusCode()})")
        } else {
            return json.asArray().associate { entry ->
                val obj = entry.asObject()
                obj["code"]!!.asString() to obj["targets"]!!.asArray().map(Jval::asString).toSet()
            }
        }
    }
}
