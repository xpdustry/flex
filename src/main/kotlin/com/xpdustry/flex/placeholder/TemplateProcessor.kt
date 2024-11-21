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

import com.sksamuel.hoplite.ConfigAlias
import com.xpdustry.flex.processor.Processor
import org.slf4j.LoggerFactory

internal typealias TemplateConfig = Map<String, Template>

internal typealias Template = List<Step>

internal data class Step(
    val text: String,
    @ConfigAlias("if") val filter: PlaceholderFilter = PlaceholderFilter.None,
)

internal class TemplateProcessor(private val placeholders: PlaceholderPipeline) : Processor<PlaceholderContext, String> {
    internal var config: TemplateConfig = emptyMap()

    override fun process(context: PlaceholderContext) =
        config[context.query]
            ?.map { step ->
                val accepted =
                    try {
                        step.filter.accepts(context)
                    } catch (e: Exception) {
                        logger.error("Error while processing template '{}'", context.query, e)
                        return@map null
                    }
                if (accepted) {
                    placeholders.pump(context.copy(query = step.text))
                } else {
                    null
                }
            }
            ?.filterNotNull()
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "")
            ?: ""

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateProcessor::class.java)
    }
}
