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
import com.xpdustry.flex.placeholder.AudienceProcessor
import com.xpdustry.flex.placeholder.PermissionProcessor
import com.xpdustry.flex.placeholder.PlaceholderPipeline
import com.xpdustry.flex.placeholder.PlaceholderPipelineImpl
import com.xpdustry.flex.placeholder.PlayerProcessor
import com.xpdustry.flex.placeholder.template.Template
import com.xpdustry.flex.placeholder.template.TemplateFilterDecoder
import com.xpdustry.flex.placeholder.template.TemplateManager
import com.xpdustry.flex.placeholder.template.TemplateManagerImpl
import com.xpdustry.flex.placeholder.template.TemplateProcessor
import com.xpdustry.flex.placeholder.template.TemplateStep
import com.xpdustry.flex.translator.DeeplTranslator
import com.xpdustry.flex.translator.LibreTranslateTranslator
import com.xpdustry.flex.translator.Translator
import com.xpdustry.flex.translator.TranslatorConfig
import kotlin.io.path.notExists
import kotlin.io.path.writeText

internal class FlexPlugin : AbstractMindustryPlugin(), FlexAPI {
    override lateinit var placeholders: PlaceholderPipeline
    override lateinit var messages: MessagePipeline
    override lateinit var translator: Translator
    override lateinit var templates: TemplateManager
    private val processor = PluginAnnotationProcessor.events(this)

    override fun onInit() {
        val config = loadConfig()

        translator = createTranslator(config.translator)

        templates = TemplateManagerImpl(config.templates).also(::addListener)
        templates.setDefaultTemplate(
            TemplateManager.JOIN_TEMPLATE_NAME,
            Template(listOf(TemplateStep("[accent]%audience:name% has connected."))),
        )
        templates.setDefaultTemplate(
            TemplateManager.QUIT_TEMPLATE_NAME,
            Template(listOf(TemplateStep("[accent]%audience:name% has disconnected."))),
        )
        templates.setDefaultTemplate(
            TemplateManager.NAME_TEMPLATE_NAME,
            Template(listOf(TemplateStep("[%audience:color%]%audience:name_colored%"))),
        )
        templates.setDefaultTemplate(
            TemplateManager.CHAT_TEMPLATE_NAME,
            Template(listOf(TemplateStep("[coral][[[%audience:color%]%audience:name_colored%[coral]]:[white] %argument:flex_message%"))),
        )

        placeholders = PlaceholderPipelineImpl(this)
        placeholders.register("template", TemplateProcessor(placeholders, templates).also(::addListener))
        placeholders.register("argument", ArgumentProcessor)
        placeholders.register("player", PlayerProcessor)
        placeholders.register("permission", PermissionProcessor)
        placeholders.register("audience", AudienceProcessor)

        messages = MessagePipelineImpl(this, placeholders).also(::addListener)
        messages.register("admin_filter", Priority.HIGH, AdminFilterProcessor)
        messages.register("translator", Priority.LOW, TranslationProcessor(translator, placeholders))

        addListener(ChatMessageHook(messages, config.hooks))
        addListener(ConnectionNotificationHook(placeholders, config.hooks))
        addListener(PlayerNameHook(placeholders, this, config.hooks))
    }

    override fun onServerCommandsRegistration(handler: CommandHandler) {
        handler.register("flex-reload", "Reload flex configuration") {
            try {
                val config = loadConfig()
                listeners.forEach { if (it is FlexListener) it.onFlexConfigReload(config) }
                logger.info("Flex reload successful")
            } catch (e: Exception) {
                logger.error("Failed to reload flex", e)
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
                addDecoder(TemplateFilterDecoder())
                withReport()
                withReportPrintFn(logger::debug)
                addParameterMapper(KebabCaseParamMapper)
                strict()
            }

        if (file.notExists()) {
            logger.warn("Configuration file does not exist, creating default configuration")
            file.writeText(
                """
                hooks:
                  chat: false
                  join: false
                  quit: false
                  name:
                    enabled: false    
                """.trimIndent(),
            )
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
            addListener(translator)
        }
        return Translator.caching(translator)
    }
}
