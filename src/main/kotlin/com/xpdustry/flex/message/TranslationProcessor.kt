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
package com.xpdustry.flex.message

import arc.util.Strings
import com.xpdustry.distributor.api.key.MutableKeyContainer
import com.xpdustry.distributor.api.key.StandardKeys
import com.xpdustry.flex.FlexAPI
import com.xpdustry.flex.FlexKeys
import com.xpdustry.flex.FlexScope
import com.xpdustry.flex.placeholder.PlaceholderContext
import com.xpdustry.flex.placeholder.PlaceholderPipeline
import com.xpdustry.flex.processor.Processor
import com.xpdustry.flex.translator.RateLimitedException
import com.xpdustry.flex.translator.Translator
import com.xpdustry.flex.translator.UnsupportedLanguageException
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.seconds

public open class TranslationProcessor(
    private val translator: Translator,
    private val placeholders: PlaceholderPipeline,
) : Processor<MessageContext, CompletableFuture<String>> {
    public constructor() : this(FlexAPI.get().translator, FlexAPI.get().placeholders)

    override fun process(context: MessageContext): CompletableFuture<String> {
        val sourceLocale = context.sender.metadata[StandardKeys.LOCALE] ?: Locale.getDefault()
        val targetLocale = context.target.metadata[StandardKeys.LOCALE] ?: Locale.getDefault()
        return process(context, sourceLocale, targetLocale)
    }

    protected fun process(
        context: MessageContext,
        sourceLocale: Locale,
        targetLocale: Locale,
    ): CompletableFuture<String> =
        FlexScope.future {
            if (context.kind != MessageContext.Kind.CHAT) {
                return@future context.message
            }

            val raw = Strings.stripColors(context.message).lowercase()
            try {
                val result =
                    withTimeout(3.seconds) {
                        translator.translate(raw, sourceLocale, targetLocale).await()
                    }
                val formatted =
                    placeholders.pump(
                        PlaceholderContext(
                            context.sender,
                            TRANSLATOR_PLACEHOLDER,
                            MutableKeyContainer.create().apply {
                                set(FlexKeys.MESSAGE, context.message)
                                set(FlexKeys.TRANSLATED_MESSAGE, result)
                            },
                        ),
                    )
                if (raw == result.lowercase()) {
                    context.message
                } else if (formatted.isBlank() || formatted == TRANSLATOR_PLACEHOLDER) {
                    "${context.message} [lightgray]($result)"
                } else {
                    formatted
                }
            } catch (e: RateLimitedException) {
                logger.debug("The {} translator is rate limited", translator.javaClass.simpleName)
                context.message
            } catch (e: UnsupportedLanguageException) {
                context.message
            } catch (e: Exception) {
                logger.error(
                    "Failed to translate the message '{}' from {} to {}",
                    raw,
                    sourceLocale,
                    targetLocale,
                    e,
                )
                context.message
            }
        }

    public companion object {
        public const val TRANSLATOR_PLACEHOLDER: String = "%template:translator_format%"
        private val logger = LoggerFactory.getLogger(TranslationProcessor::class.java)
    }
}
