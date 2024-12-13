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
import com.xpdustry.flex.processor.AbstractPriorityProcessorPipeline
import java.util.regex.Pattern

internal class PlaceholderPipelineImpl(
    plugin: MindustryPlugin,
) : PlaceholderPipeline, AbstractPriorityProcessorPipeline<PlaceholderContext, String?>(plugin, "placeholder") {
    override fun pump(context: PlaceholderContext): String {
        val builder = StringBuilder()
        val matcher = PLACEHOLDER_REGEX.matcher(context.query)
        while (matcher.find()) {
            val name = matcher.group("name")
            val replacement =
                try {
                    processors[name]?.process(context.copy(query = matcher.group("query") ?: ""))
                } catch (e: Exception) {
                    plugin.logger.error("Error while interpolating placeholder '{}'", name, e)
                    null
                }
            if (replacement != null) {
                matcher.appendReplacement(builder, replacement)
            } else {
                matcher.appendReplacement(builder, matcher.group())
            }
        }
        matcher.appendTail(builder)
        return builder.toString()
    }

    companion object {
        private val PLACEHOLDER_REGEX = Pattern.compile("%(?<name>\\w*)(:(?<query>[\\w:]*)?)?%")
    }
}
