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
package com.xpdustry.flex

import com.xpdustry.distributor.api.DistributorProvider
import com.xpdustry.distributor.api.annotation.EventHandler
import com.xpdustry.distributor.api.key.KeyContainer
import com.xpdustry.distributor.api.plugin.PluginListener
import mindustry.game.EventType
import mindustry.gen.Player
import mindustry.net.Administration

internal class FlexConnectListener : PluginListener {
    override fun onPluginInit() {
        onFlexConnectMessagesChange()
    }

    @EventHandler
    internal fun onPlayerJoin(event: EventType.PlayerJoin) {
        sendConnect(event.player, "mindustry-join")
    }

    @EventHandler
    internal fun onPlayerQuit(event: EventType.PlayerLeave) {
        sendConnect(event.player, "mindustry-quit")
    }

    private fun sendConnect(
        player: Player,
        pipeline: String,
    ) {
        if (!FLEX_CONNECT_MESSAGES.bool()) return
        val audiences = DistributorProvider.get().audienceProvider
        val context = FlexContext(audiences.getPlayer(player), KeyContainer.empty())
        audiences.players.sendMessage(
            FlexAPI.get().interpolatePipeline(context, pipeline) ?: return,
        )
    }

    companion object {
        private val FLEX_CONNECT_MESSAGES =
            Administration.Config(
                "flexConnectMessages",
                "Whether flex should handle join messages",
                true,
                ::onFlexConnectMessagesChange,
            )

        private fun onFlexConnectMessagesChange() {
            Administration.Config.showConnectMessages.set(!FLEX_CONNECT_MESSAGES.bool())
        }
    }
}
