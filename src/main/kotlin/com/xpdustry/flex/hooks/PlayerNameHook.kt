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
import com.xpdustry.distributor.api.collection.MindustryCollections
import com.xpdustry.distributor.api.plugin.MindustryPlugin
import com.xpdustry.distributor.api.plugin.PluginListener
import com.xpdustry.distributor.api.scheduler.MindustryTimeUnit
import com.xpdustry.flex.placeholder.PlaceholderContext
import com.xpdustry.flex.placeholder.PlaceholderPipeline
import mindustry.Vars
import mindustry.gen.Groups
import org.slf4j.LoggerFactory
import kotlin.time.toJavaDuration

internal class PlayerNameHook(
    private val placeholders: PlaceholderPipeline,
    private val plugin: MindustryPlugin,
    private val config: HooksConfig,
) : PluginListener {
    override fun onPluginInit() {
        if (config.name.enabled) {
            DistributorProvider.get().pluginScheduler
                .schedule(plugin)
                .async(false)
                .delay(1, MindustryTimeUnit.SECONDS)
                .repeat(config.name.interval.toJavaDuration())
                .execute(this::onNameUpdate)
        }
    }

    private fun onNameUpdate() {
        if (!Vars.state.isGame) return
        MindustryCollections.immutableList(Groups.player).forEach { player ->
            try {
                val result =
                    placeholders.pump(
                        PlaceholderContext(DistributorProvider.get().audienceProvider.getPlayer(player), "%template:mindustry_name%"),
                    )
                if (result.isNotBlank()) {
                    player.name(result)
                } else {
                    logger.warn("Processed name of player {} ({}) is blank.", player.name(), player.uuid())
                }
            } catch (e: Exception) {
                logger.error("Error while updating player name of {} ({})", player.name(), player.uuid(), e)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PlayerNameHook::class.java)
    }
}
