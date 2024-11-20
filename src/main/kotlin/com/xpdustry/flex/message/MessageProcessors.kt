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

import arc.Core
import arc.util.Strings
import com.xpdustry.distributor.api.audience.PlayerAudience
import com.xpdustry.distributor.api.key.MutableKeyContainer
import com.xpdustry.distributor.api.key.StandardKeys
import com.xpdustry.flex.FlexScope
import com.xpdustry.flex.placeholder.PlaceholderContext
import com.xpdustry.flex.placeholder.PlaceholderMode
import com.xpdustry.flex.placeholder.PlaceholderPipeline
import com.xpdustry.flex.processor.Processor
import com.xpdustry.flex.translator.Translator
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.withTimeout
import mindustry.Vars
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

internal object AdminFilterProcessor : Processor<MessageContext, CompletableFuture<String>> {
    private val logger = LoggerFactory.getLogger(AdminFilterProcessor::class.java)
    private val filtering = AtomicBoolean(false)

    override fun process(context: MessageContext): CompletableFuture<String> {
        return if (context.sender is PlayerAudience && context.kind == MessageContext.Kind.CHAT) {
            if (filtering.get()) {
                logger.debug("Possible deadlock detected, skipping admin filter", IllegalStateException())
                return CompletableFuture.completedFuture(context.message)
            }
            val future = CompletableFuture<String>()
            Core.app.post {
                try {
                    filtering.set(true)
                    future.complete(Vars.netServer.admins.filterMessage(context.sender.player, context.message) ?: "")
                } finally {
                    filtering.set(false)
                }
            }
            future.orTimeout(5, TimeUnit.SECONDS)
        } else {
            CompletableFuture.completedFuture(context.message)
        }
    }
}

internal class TranslationProcessor(
    private val placeholders: PlaceholderPipeline,
    private val translator: Translator,
) : Processor<MessageContext, CompletableFuture<String>> {
    override fun process(context: MessageContext) =
        FlexScope.future {
            val sourceLocale = context.sender.metadata[StandardKeys.LOCALE] ?: Locale.getDefault()
            val targetLocale = context.target.metadata[StandardKeys.LOCALE] ?: Locale.getDefault()
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
                            "translation-format",
                            MutableKeyContainer.create().apply {
                                set(PlaceholderPipeline.MESSAGE, context.message)
                                set(PlaceholderPipeline.TRANSLATED_MESSAGE, result)
                            },
                        ),
                        PlaceholderMode.PRESET,
                    )
                if (raw == result.lowercase()) {
                    context.message
                } else if (formatted.isBlank()) {
                    "${context.message} [lightgray]($result)"
                } else {
                    formatted
                }
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

    companion object {
        private val logger = LoggerFactory.getLogger(TranslationProcessor::class.java)
    }
}
