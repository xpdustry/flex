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
import com.sksamuel.hoplite.KebabCaseParamMapper
import com.sksamuel.hoplite.addPathSource
import com.xpdustry.distributor.api.annotation.PluginAnnotationProcessor
import com.xpdustry.distributor.api.plugin.AbstractMindustryPlugin
import com.xpdustry.distributor.api.plugin.PluginListener
import com.xpdustry.distributor.api.util.Priority
import com.xpdustry.flex.hooks.ChatMessageHook
import com.xpdustry.flex.hooks.ConnectionNotificationHook
import com.xpdustry.flex.hooks.PlayerNameHook
import com.xpdustry.flex.message.AdminFilterProcessor
import com.xpdustry.flex.message.MessagePipeline
import com.xpdustry.flex.message.MessagePipelineImpl
import com.xpdustry.flex.message.TranslationProcessor
import com.xpdustry.flex.placeholder.ArgumentProcessor
import com.xpdustry.flex.placeholder.PermissionProcessor
import com.xpdustry.flex.placeholder.PlaceholderFilterDecoder
import com.xpdustry.flex.placeholder.PlaceholderPipeline
import com.xpdustry.flex.placeholder.PlaceholderPipelineImpl
import com.xpdustry.flex.placeholder.PlayerProcessor
import com.xpdustry.flex.placeholder.TemplateProcessor
import com.xpdustry.flex.translator.DeeplTranslator
import com.xpdustry.flex.translator.LibreTranslateTranslator
import com.xpdustry.flex.translator.Translator
import com.xpdustry.flex.translator.TranslatorConfig
import kotlin.io.path.notExists
import kotlin.io.path.outputStream

internal class FlexPlugin : AbstractMindustryPlugin(), FlexAPI {
    override lateinit var placeholders: PlaceholderPipeline
    override lateinit var messages: MessagePipeline
    override lateinit var translator: Translator
    private val processor = PluginAnnotationProcessor.events(this)
    private lateinit var templates: TemplateProcessor

    override fun onInit() {
        val config = loadConfig()

        translator = createTranslator(config.translator)

        placeholders = PlaceholderPipelineImpl(this)
        templates = TemplateProcessor(placeholders)
        templates.config = config.templates
        placeholders.register("template", templates)
        placeholders.register("argument", ArgumentProcessor)
        placeholders.register("player", PlayerProcessor)
        placeholders.register("permission", PermissionProcessor)

        messages = MessagePipelineImpl(this, placeholders).also(::addListener)
        messages.register("admin_filter", Priority.HIGH, AdminFilterProcessor)
        messages.register("flex_translator", Priority.LOW, TranslationProcessor(placeholders, translator))

        addListener(ChatMessageHook(messages, config.hooks))
        addListener(ConnectionNotificationHook(placeholders, config.hooks))
        addListener(PlayerNameHook(placeholders, this, config.hooks))
    }

    override fun onServerCommandsRegistration(handler: CommandHandler) {
        handler.register("flex-reload-templates", "Reload the placeholder templates") {
            try {
                val config = loadConfig()
                templates.config = config.templates
                logger.info("Reloaded templates")
            } catch (e: Exception) {
                logger.error("Failed to reload templates", e)
            }
        }
    }

    override fun addListener(listener: PluginListener) {
        super.addListener(listener)
        processor.process(listener)
    }

    private fun loadConfig(): FlexConfig {
        val file = directory.resolve("config.yaml")

        val loader =
            ConfigLoader {
                withClassLoader(javaClass.classLoader)
                addDefaults()
                addPathSource(file)
                addDecoder(PlaceholderFilterDecoder())
                withReport()
                withReportPrintFn(logger::debug)
                addParameterMapper(KebabCaseParamMapper)
                strict()
            }

        if (file.notExists()) {
            logger.warn("Configuration file does not exist, creating default configuration")
            javaClass.classLoader.getResource("com/xpdustry/flex/default.yaml")!!
                .openStream().buffered()
                .use { input ->
                    file.outputStream().buffered().use { output -> input.copyTo(output) }
                }
        }

        return loader.loadConfigOrThrow()
    }

    private fun createTranslator(config: TranslatorConfig): Translator {
        val translator =
            when (config) {
                is TranslatorConfig.None -> Translator.None
                is TranslatorConfig.LibreTranslate -> LibreTranslateTranslator(config)
                is TranslatorConfig.DeepL -> DeeplTranslator(config, metadata.version)
            }
        if (translator is PluginListener) {
            addListener(translator as PluginListener)
        }
        return Translator.caching(translator)
    }
}
