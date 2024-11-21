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
package com.xpdustry.flex.hooks

import com.xpdustry.distributor.api.DistributorProvider
import com.xpdustry.distributor.api.annotation.EventHandler
import com.xpdustry.distributor.api.plugin.PluginListener
import com.xpdustry.flex.placeholder.PlaceholderContext
import com.xpdustry.flex.placeholder.PlaceholderPipeline
import mindustry.game.EventType
import mindustry.gen.Player

internal class ConnectionNotificationHook(
    private val placeholders: PlaceholderPipeline,
    private val hooks: HooksConfig,
) : PluginListener {
    @EventHandler
    internal fun onPlayerJoin(event: EventType.PlayerJoin) {
        if (hooks.join) sendConnect(event.player, "mindustry_join")
    }

    @EventHandler
    internal fun onPlayerQuit(event: EventType.PlayerLeave) {
        if (hooks.quit) sendConnect(event.player, "mindustry_quit")
    }

    private fun sendConnect(
        player: Player,
        template: String,
    ) = DistributorProvider.get().audienceProvider.players.sendMessage(
        DistributorProvider.get()
            .mindustryComponentDecoder.decode(
                placeholders.pump(
                    PlaceholderContext(DistributorProvider.get().audienceProvider.getPlayer(player), "%template:$template%"),
                ),
            ),
    )
}
