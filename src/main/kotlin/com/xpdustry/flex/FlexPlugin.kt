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

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.KebabCaseParamMapper
import com.sksamuel.hoplite.addPathSource
import com.xpdustry.distributor.api.DistributorProvider
import com.xpdustry.distributor.api.annotation.EventHandler
import com.xpdustry.distributor.api.component.Component
import com.xpdustry.distributor.api.component.ListComponent
import com.xpdustry.distributor.api.component.ListComponent.components
import com.xpdustry.distributor.api.component.TextComponent
import com.xpdustry.distributor.api.component.TextComponent.text
import com.xpdustry.distributor.api.key.KeyContainer
import com.xpdustry.distributor.api.plugin.AbstractMindustryPlugin
import mindustry.game.EventType
import mindustry.net.Administration

class FlexPlugin : AbstractMindustryPlugin() {
    private val pipelines = mutableMapOf<String, FlexPipeline>()
    private val extensions =
        listOf(
            StandardPlayerExtension(this),
        )

    @EventHandler
    internal fun onPlayerJoin(event: EventType.PlayerJoin) {
        if (!FLEX_CONNECT_MESSAGES.bool()) return
        val pipeline = pipelines["mindustry-join"] ?: return logger.warn("Pipeline 'mindustry-join' not found")
        val components = components()
        val subject = DistributorProvider.get().audienceProvider.getPlayer(event.player)
        DistributorProvider.get().audienceProvider.players.audiences.forEach { audience ->
            val context = FlexContext(subject, audience, KeyContainer.empty())
            pipeline.steps.forEach { step ->
                if (step.filter.accepts(context)) components.append(step.text)
            }
            audience.sendMessage(interpolate(context, components.build()) { query -> extensions.find { it.identifier == query } })
        }
    }

    @EventHandler
    internal fun onPlayerQuit(event: EventType.PlayerLeave) {
        if (!FLEX_CONNECT_MESSAGES.bool()) return
        val pipeline = pipelines["mindustry-quit"] ?: return logger.warn("Pipeline 'mindustry-quit' not found")
    }

    override fun onLoad() {
    }

    fun load(lookup: FlexExtensionFinder) {
        ConfigLoader {
            withClassLoader(FlexPlugin::class.java.classLoader)
            addDefaultDecoders()
            addDefaultPreprocessors()
            addDefaultParamMappers()
            addParameterMapper(KebabCaseParamMapper)
            addDefaultPropertySources()
            addDefaultParsers() // YamlParser is loaded via ServiceLoader here
            addPathSource(directory.resolve("config.yaml"))
            addDecoder(FlexStepDecoder(lookup))
            strict()
        }
    }

    fun interpolate(
        context: FlexContext,
        component: Component,
        finder: FlexExtensionFinder,
    ): Component =
        when (component) {
            is ListComponent ->
                component.toBuilder()
                    .setComponents(component.components.map { interpolate(context, it, finder) })
                    .build()
            is TextComponent -> {
                val output =
                    components()
                        .setTextStyle(component.textStyle)
                var cursor = 0
                while (cursor < component.content.length) {
                    val start = component.content.indexOf('%', cursor)
                    if (start == -1) {
                        output.append(text(component.content.substring(cursor)))
                        break
                    }
                    val close = component.content.indexOf('%', start + 1)
                    if (close == -1) {
                        output.append(text(component.content.substring(cursor)))
                        break
                    }
                    val placeholder = component.content.substring(start + 1, close)
                    if (placeholder.isEmpty()) {
                        output.append(text('%'))
                        cursor = close + 1
                        continue
                    }
                    val extension = finder.find(placeholder)
                    if (extension == null) {
                        output.append(text(component.content.substring(cursor, close + 1)))
                        cursor = close + 1
                        continue
                    }
                    val result =
                        try {
                            extension.onPlaceholderRequest(context, placeholder)
                        } catch (e: Exception) {
                            output.append(text(component.content.substring(cursor, close + 1)))
                            cursor = close + 1
                            // TODO log error
                            continue
                        }
                    if (result == null) {
                        output.append(text(component.content.substring(cursor, close + 1)))
                        cursor = close + 1
                        continue
                    }
                    output.append(result)
                }
                output.build()
            }
            else -> component
        }

    companion object {
        private val FLEX_CONNECT_MESSAGES: Administration.Config =
            Administration.Config(
                "flex-connect-messages",
                "Whether flex should handle join messages",
                false,
                ::onFlexConnectMessagesChange,
            )

        private fun onFlexConnectMessagesChange() {
            Administration.Config.showConnectMessages.set(!FLEX_CONNECT_MESSAGES.bool())
        }
    }
}
