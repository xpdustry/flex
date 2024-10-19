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

import com.xpdustry.distributor.api.audience.Audience
import com.xpdustry.distributor.api.key.StandardKeys
import com.xpdustry.distributor.api.plugin.MindustryPlugin
import com.xpdustry.distributor.api.plugin.PluginListener
import com.xpdustry.flex.FlexScope
import com.xpdustry.flex.processor.ProcessorPipeline
import com.xpdustry.imperium.mindustry.processing.AbstractProcessorPipeline
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future

public data class MessageContext
    @JvmOverloads
    constructor(
        val sender: Audience,
        val target: Audience,
        val message: String,
        val kind: Kind = Kind.CHAT,
    ) {
        public enum class Kind {
            CHAT,
            COMMAND,
        }
    }

public interface MessagePipeline : ProcessorPipeline<MessageContext, String>

internal class MessagePipelineImpl(plugin: MindustryPlugin) :
    MessagePipeline,
    PluginListener,
    AbstractProcessorPipeline<MessageContext, String>(plugin, "chat-message") {
    override fun onPluginInit() {
        register("admin_filter", AdminFilterProcessor)
    }

    override fun pump(context: MessageContext) =
        FlexScope.future {
            var result = context.message
            for (processor in processors) {
                result =
                    try {
                        processor.process(context.copy(message = result)).await()
                    } catch (error: Throwable) {
                        plugin.logger.error(
                            "Error while processing message of {} to {}",
                            context.sender.metadata[StandardKeys.NAME] ?: "Unknown",
                            context.target.metadata[StandardKeys.NAME] ?: "Unknown",
                            error,
                        )
                        result
                    }
                if (result.isEmpty()) break
            }
            result
        }
}
