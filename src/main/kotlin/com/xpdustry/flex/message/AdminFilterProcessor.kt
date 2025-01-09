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
package com.xpdustry.flex.message

import arc.Core
import com.xpdustry.distributor.api.audience.PlayerAudience
import com.xpdustry.flex.processor.Processor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import mindustry.Vars
import mindustry.gen.Player
import org.slf4j.LoggerFactory

internal object AdminFilterProcessor : Processor<MessageContext, CompletableFuture<String>> {
    private val logger = LoggerFactory.getLogger(AdminFilterProcessor::class.java)
    private val filtering = AtomicBoolean(false)

    override fun process(context: MessageContext): CompletableFuture<String> =
        if (context.sender is PlayerAudience && context.kind == MessageContext.Kind.CHAT && context.filter) {
            CompletableFuture.supplyAsync({ process(context.sender.player, context.message) }, Core.app::post)
                .orTimeout(5, TimeUnit.SECONDS)
        } else {
            CompletableFuture.completedFuture(context.message)
        }

    private fun process(player: Player, message: String): String {
        if (filtering.get()) {
            logger.debug(
                "Stack overflow detected for message {} from player {} ({}), skipping admin filter",
                message,
                player.plainName(),
                player.uuid(),
                IllegalStateException(),
            )
            return message
        }
        try {
            filtering.set(true)
            return Vars.netServer.admins.filterMessage(player, message) ?: ""
        } finally {
            filtering.set(false)
        }
    }
}
