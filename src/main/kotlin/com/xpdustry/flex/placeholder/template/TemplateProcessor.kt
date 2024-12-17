/*
 * This file is part of Flex. An advanced text processing library for Mindustry plugins.
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

import com.xpdustry.flex.FlexListener
import com.xpdustry.flex.placeholder.PlaceholderContext
import com.xpdustry.flex.placeholder.PlaceholderPipeline
import com.xpdustry.flex.processor.Processor
import org.slf4j.LoggerFactory

internal class TemplateProcessor(
    private val placeholders: PlaceholderPipeline,
    private var templates: TemplateManager,
) : Processor<PlaceholderContext, String?>, FlexListener {
    override fun process(context: PlaceholderContext) =
        templates.getTemplate(context.query)
            ?.steps
            ?.map { step ->
                val accepted =
                    try {
                        step.filter.accepts(context)
                    } catch (e: Exception) {
                        logger.error("Error while filtering template step for '{}'", context.query, e)
                        return@map null
                    }
                if (accepted) {
                    placeholders.pump(context.copy(query = step.text))
                } else {
                    null
                }
            }
            ?.filterNotNull()
            ?.joinToString(separator = "")

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateProcessor::class.java)
    }
}
