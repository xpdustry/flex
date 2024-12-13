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
import com.xpdustry.flex.translator.Translator.Companion.AUTO_DETECT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.Charset
import java.util.Locale

internal class LibreTranslateTranslator(
    private val endpoint: URI,
    private val apiKey: String,
) : Translator, PluginListener {
    private val http = HttpClient.newHttpClient()
    private lateinit var languages: Map<String, Set<String>>

    override fun onPluginInit() {
        languages = fetchSupportedLanguages()
    }

    override fun translate(
        text: String,
        source: Locale,
        target: Locale,
    ) = FlexScope.future {
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

        val response =
            withContext(Dispatchers.IO) {
                http.send(
                    HttpRequest.newBuilder(
                        createLibreUri(
                            "translate",
                            "q" to text,
                            "source" to source.language,
                            "target" to target.language,
                            "api_key" to apiKey,
                            "format" to "text",
                        ),
                    )
                        .header("Accept", "application/json")
                        .POST(BodyPublishers.noBody())
                        .build(),
                    BodyHandlers.ofString(),
                )
            }

        val json = Json.parseToJsonElement(response.body()).jsonObject
        if (response.statusCode() != 200) {
            throw Exception("Failed to translate: ${json["error"]?.jsonPrimitive?.content} (code=${response.statusCode()})")
        } else {
            json["translatedText"]!!.jsonPrimitive.content
        }
    }

    override fun isSupportedLanguage(locale: Locale) = locale.language in languages

    private fun fetchSupportedLanguages(): Map<String, Set<String>> {
        val response =
            http.send(
                HttpRequest.newBuilder(createLibreUri("languages"))
                    .header("Accept", "application/json")
                    .GET()
                    .build(),
                BodyHandlers.ofString(),
            )
        return if (response.statusCode() != 200) {
            throw Exception("Failed to fetch supported languages (code=${response.statusCode()})")
        } else {
            val result =
                Json.parseToJsonElement(response.body()).jsonArray.associate { entry ->
                    val obj = entry.jsonObject
                    obj["code"]!!.jsonPrimitive.content to
                        obj["targets"]!!.jsonArray.mapTo(mutableSetOf()) { it.jsonPrimitive.content }
                }
            result + (AUTO_DETECT.language to result.keys.toSet())
        }
    }

    private fun createLibreUri(
        path: String,
        vararg params: Pair<String, String>,
    ): URI {
        var result = endpoint.toString()
        if (!result.endsWith('/')) result += '/'
        result += path
        val query = params.joinToString("&") { (key, value) -> "$key=${URLEncoder.encode(value, Charset.defaultCharset())}" }
        if (query.isNotEmpty()) result += "?$query"
        return URI.create(result)
    }
}
