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
import com.sksamuel.hoplite.addPathSource
import com.xpdustry.distributor.api.annotation.PluginAnnotationProcessor
import com.xpdustry.distributor.api.plugin.AbstractMindustryPlugin
import com.xpdustry.distributor.api.plugin.PluginListener
import com.xpdustry.flex.message.MessagePipeline
import com.xpdustry.flex.message.MessagePipelineImpl
import com.xpdustry.flex.placeholder.PlaceholderFilterDecoder
import com.xpdustry.flex.placeholder.PlaceholderPipeline
import com.xpdustry.flex.placeholder.PlaceholderPipelineImpl
import com.xpdustry.flex.translator.CachingTranslator
import com.xpdustry.flex.translator.DeeplTranslator
import com.xpdustry.flex.translator.LibreTranslateTranslator
import com.xpdustry.flex.translator.Translator
import com.xpdustry.flex.translator.TranslatorConfig
import kotlin.io.path.notExists
import kotlin.io.path.outputStream

internal class FlexPlugin : AbstractMindustryPlugin(), FlexAPI {
    override var translator: Translator = Translator.None
    override lateinit var placeholders: PlaceholderPipeline
    override lateinit var messages: MessagePipeline

    private lateinit var loader: ConfigLoader
    private lateinit var config: FlexConfig
    private val file = directory.resolve("config.yaml")
    private val processor: PluginAnnotationProcessor<*> = PluginAnnotationProcessor.events(this)

    override fun onInit() {
        loader =
            ConfigLoader {
                withClassLoader(javaClass.classLoader)
                addDefaults()
                addPathSource(file)
                addDecoder(PlaceholderFilterDecoder())
                withReport()
                withReportPrintFn(logger::debug)
            }

        reload()

        placeholders = PlaceholderPipelineImpl(this) { this@FlexPlugin.config.placeholders }.also(::addListener)
        messages = MessagePipelineImpl(this, placeholders) { this@FlexPlugin.translator }.also(::addListener)

        // addListener(FlexConnectMessageHook(::config))
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

        val previous = translator
        if (previous is PluginListener) {
            previous.onPluginExit()
        }

        translator =
            when (val t = config.translator) {
                is TranslatorConfig.None -> Translator.None
                is TranslatorConfig.LibreTranslate -> LibreTranslateTranslator(t)
                is TranslatorConfig.DeepL -> DeeplTranslator(t, metadata.version)
            }

        if (translator is PluginListener) {
            (translator as PluginListener).onPluginInit()
        }

        translator = CachingTranslator(translator)
    }
}