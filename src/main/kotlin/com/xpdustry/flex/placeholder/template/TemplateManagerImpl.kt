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
package com.xpdustry.flex.placeholder.template

import com.xpdustry.flex.FlexConfig
import com.xpdustry.flex.FlexListener
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

internal class TemplateManagerImpl(config: TemplateConfig) : TemplateManager, FlexListener {
    private var templates: Map<String, Template> = config.toTemplateMap()
    private val defaults = ConcurrentHashMap<String, Template>()

    override fun onFlexConfigReload(config: FlexConfig) {
        templates = config.templates.toTemplateMap()
        logger.info("Reloaded {} templates", config.templates.size)
    }

    override fun getTemplate(name: String): Template? = templates[name] ?: defaults[name]

    override fun hasTemplate(name: String): Boolean = templates.containsKey(name) || defaults.containsKey(name)

    override fun setDefaultTemplate(
        name: String,
        template: Template,
    ) {
        defaults[name] = template
    }

    private fun TemplateConfig.toTemplateMap(): Map<String, Template> = mapValues { (_, steps) -> Template(steps) }

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateManagerImpl::class.java)
    }
}
