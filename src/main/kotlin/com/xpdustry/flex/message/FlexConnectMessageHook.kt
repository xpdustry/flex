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

import com.xpdustry.distributor.api.DistributorProvider
import com.xpdustry.distributor.api.annotation.EventHandler
import com.xpdustry.distributor.api.plugin.PluginListener
import com.xpdustry.flex.FlexScope
import com.xpdustry.flex.placeholder.PlaceholderContext
import com.xpdustry.flex.placeholder.PlaceholderPipeline
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import mindustry.game.EventType
import mindustry.gen.Player
import java.util.function.Supplier

internal class FlexConnectMessageHook(
    private val placeholders: PlaceholderPipeline,
    private val config: Supplier<MessageConfig>,
) : PluginListener {
    @EventHandler
    internal fun onPlayerJoin(event: EventType.PlayerJoin) =
        FlexScope.launch {
            sendConnect(event.player, "mindustry-join")
        }

    @EventHandler
    internal fun onPlayerQuit(event: EventType.PlayerLeave) =
        FlexScope.launch {
            sendConnect(event.player, "mindustry-quit")
        }

    private suspend fun sendConnect(
        player: Player,
        pipeline: String,
    ) {
        if (!config.get().connect) return
        DistributorProvider.get().audienceProvider.players.sendMessage(
            DistributorProvider.get()
                .mindustryComponentDecoder.decode(
                    placeholders.pump(
                        PlaceholderContext(DistributorProvider.get().audienceProvider.getPlayer(player), pipeline),
                        PlaceholderPipeline.Mode.PRESET,
                    ).await(),
                ),
        )
    }
}
