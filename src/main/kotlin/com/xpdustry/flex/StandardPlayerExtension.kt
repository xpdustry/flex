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
import com.xpdustry.distributor.api.audience.PlayerAudience
import com.xpdustry.distributor.api.component.Component
import com.xpdustry.distributor.api.component.NumberComponent
import com.xpdustry.distributor.api.component.TextComponent
import com.xpdustry.distributor.api.plugin.MindustryPlugin

internal class StandardPlayerExtension(private val plugin: MindustryPlugin) : FlexExtension {
    override val identifier = "player"

    override fun getPlugin() = plugin

    override fun onPlaceholderRequest(
        context: FlexContext,
        query: String,
    ): Component? {
        if (context.sender !is PlayerAudience) return null
        val player = context.sender.player
        return when (query.lowercase()) {
            "name_raw" -> TextComponent.text(player.plainName())
            "name_colored" -> DistributorProvider.get().mindustryComponentDecoder.decode(player.coloredName())
            "tile_x" -> NumberComponent.number(player.tileX())
            "tile_y" -> NumberComponent.number(player.tileY())
            "world_x" -> NumberComponent.number(player.getX())
            "world_y" -> NumberComponent.number(player.getY())
            else -> null
        }
    }
}
