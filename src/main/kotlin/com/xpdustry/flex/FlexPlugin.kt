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

import arc.util.CommandHandler
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addPathSource
import com.xpdustry.distributor.api.DistributorProvider
import com.xpdustry.distributor.api.annotation.PluginAnnotationProcessor
import com.xpdustry.distributor.api.plugin.AbstractMindustryPlugin
import com.xpdustry.distributor.api.plugin.PluginListener
import com.xpdustry.flex.extension.ArgumentExtension
import com.xpdustry.flex.extension.FlexExtension
import com.xpdustry.flex.extension.PermissionExtension
import com.xpdustry.flex.extension.PlayerExtension
import kotlin.io.path.notExists
import kotlin.io.path.outputStream

internal class FlexPlugin : AbstractMindustryPlugin(), FlexAPI {
    private lateinit var config: FlexConfig
    private val file = directory.resolve("config.yaml")
    private val processor: PluginAnnotationProcessor<*> = PluginAnnotationProcessor.events(this)
    private val loader =
        ConfigLoader {
            @OptIn(ExperimentalHoplite::class)
            withExplicitSealedTypes()
            withClassLoader(javaClass.classLoader)
            addDefaults()
            addPathSource(file)
            addDecoder(FlexFilterDecoder())
        }

    override fun onInit() {
        reload()
        addListener(FlexConnectListener(::config))

        val services = DistributorProvider.get().serviceManager
        services.register(this, FlexExtension::class.java, ArgumentExtension(this))
        services.register(this, FlexExtension::class.java, PlayerExtension(this))
        services.register(this, FlexExtension::class.java, PermissionExtension(this))
    }

    override fun addListener(listener: PluginListener) {
        super.addListener(listener)
        processor.process(listener)
    }

    override fun onServerCommandsRegistration(handler: CommandHandler) {
        handler.register<Unit>("flex-reload", "Reloads the flex configuration") { _, _ ->
            try {
                reload()
            } catch (e: Exception) {
                logger.error("Error while reloading flex configuration", e)
            }
        }
    }

    override fun findExtension(identifier: String): FlexExtension? =
        DistributorProvider.get()
            .serviceManager
            .getProviders(FlexExtension::class.java)
            .find { it.instance.identifier.equals(identifier, ignoreCase = true) }
            ?.instance

    override fun interpolatePipeline(
        context: FlexContext,
        pipeline: String,
    ): String? =
        config.pipelines[pipeline]?.mapNotNull { step ->
            if (step.filter.accepts(context)) {
                interpolateText(context, step.text)
            } else {
                null
            }
        }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "")

    override fun interpolatePlaceholder(
        context: FlexContext,
        placeholder: String,
    ): String? {
        val parts = placeholder.split(':', limit = 2)
        return try {
            findExtension(parts[0])?.onPlaceholderRequest(context, parts.getOrNull(1) ?: "")
        } catch (e: Exception) {
            logger.error("Error while interpolating placeholder '{}'", placeholder, e)
            null
        }
    }

    override fun interpolateText(
        context: FlexContext,
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

    private fun reload() {
        if (file.notExists()) {
            logger.warn("Configuration file does not exist, creating default configuration")
            javaClass.classLoader.getResource("com/xpdustry/flex/default.yaml")!!
                .openStream().buffered()
                .use { input ->
                    file.outputStream().buffered()
                        .use { output -> input.copyTo(output) }
                }
        }
        config = loader.loadConfigOrThrow()
        config.pipelines.keys.forEach { name ->
            logger.debug("Loaded pipeline '{}'", name)
        }
    }

    companion object {
        private val PLACEHOLDER_REGEX = Regex("%(\\w*)%")
    }
}
