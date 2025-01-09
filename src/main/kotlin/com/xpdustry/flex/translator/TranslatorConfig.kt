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

import com.sksamuel.hoplite.Secret
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal data class TranslatorConfig(
    val backend: Backend = Backend.None,
    val registerMessageProcessor: Boolean = true,
) {
    sealed interface Backend {
        data object None : Backend

        data class LibreTranslate(val ltEndpoint: URI, val ltApiKey: Secret? = null) : Backend

        data class DeepL(val deeplApiKey: Secret) : Backend

        data class GoogleBasic(val googleBasicApiKey: Secret) : Backend

        data class Rolling(
            val translators: List<Backend>,
            val fallback: Backend = None,
        ) : Backend

        data class Caching(
            val successRetention: Duration = 10.minutes,
            val failureRetention: Duration = 10.seconds,
            val maximumSize: Int = 1000,
            val cachingTranslator: Backend,
        ) : Backend
    }
}
