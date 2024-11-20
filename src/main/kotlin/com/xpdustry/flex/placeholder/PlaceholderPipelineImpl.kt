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
package com.xpdustry.flex.placeholder

import com.xpdustry.distributor.api.plugin.MindustryPlugin
import com.xpdustry.distributor.api.plugin.PluginListener
import com.xpdustry.flex.processor.AbstractProcessorPipeline

internal class PlaceholderPipelineImpl(
    plugin: MindustryPlugin,
    private val config: PlaceholderConfig,
) : PlaceholderPipeline, PluginListener,
    AbstractProcessorPipeline<PlaceholderContext, String>(plugin, "placeholder") {
    override fun onPluginInit() {
        register("argument", ArgumentProcessor)
        register("player", PlayerProcessor)
        register("permission", PermissionProcessor)
    }

    override fun pump(
        context: PlaceholderContext,
        mode: PlaceholderMode,
    ) = when (mode) {
        PlaceholderMode.TEXT -> interpolateText(context, context.query)
        PlaceholderMode.PRESET -> interpolatePreset(context)
    }

    private fun interpolatePreset(context: PlaceholderContext): String =
        config
            .presets[context.query]
            ?.steps
            ?.map { step ->
                val accepted =
                    try {
                        step.filter.accepts(context)
                    } catch (e: Exception) {
                        plugin.logger.error("Error while interpolating preset '{}'", context.query, e)
                        return@map null
                    }
                if (accepted) {
                    interpolateText(context, step.text)
                } else {
                    null
                }
            }
            ?.filterNotNull()
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "")
            ?: ""

    private fun interpolateText(
        context: PlaceholderContext,
        text: String,
    ) = buildString {
        val matches = PLACEHOLDER_REGEX.findAll(text).toList()
        for (i in matches.indices) {
            val match = matches[i]
            append(text.substring(matches.getOrNull(i - 1)?.range?.last ?: 0, match.range.first))
            val placeholder = match.groupValues[1]
            if (placeholder.isEmpty()) {
                append('%')
            } else {
                val replacement = interpolatePlaceholder(context, placeholder)
                if (replacement != null) {
                    append(replacement)
                } else {
                    append('%')
                    append(match.value)
                    append('%')
                }
            }
        }
        val last =
            matches.lastOrNull()?.range?.last
                ?: (if (matches.isEmpty()) 0 else text.length)
        if (last < text.length) {
            append(text.substring(last))
        }
    }

    private fun interpolatePlaceholder(
        context: PlaceholderContext,
        placeholder: String,
    ): String? {
        val parts = placeholder.split(':', limit = 2)
        return try {
            processor(parts[0])
                ?.process(context.copy(query = parts.getOrNull(1) ?: ""))
                ?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            plugin.logger.error("Error while interpolating placeholder '{}'", placeholder, e)
            null
        }
    }

    companion object {
        private val PLACEHOLDER_REGEX = Regex("%(\\w*)%")
    }
}
