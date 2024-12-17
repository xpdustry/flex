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

import com.sksamuel.hoplite.ArrayNode
import com.sksamuel.hoplite.ConfigFailure
import com.sksamuel.hoplite.DecoderContext
import com.sksamuel.hoplite.MapNode
import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.StringNode
import com.sksamuel.hoplite.decoder.NullHandlingDecoder
import com.sksamuel.hoplite.fp.invalid
import com.sksamuel.hoplite.fp.sequence
import com.sksamuel.hoplite.fp.valid
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmName

internal class TemplateFilterDecoder : NullHandlingDecoder<TemplateFilter> {
    override fun supports(type: KType) = type.classifier == TemplateFilter::class

    override fun safeDecode(
        node: Node,
        type: KType,
        context: DecoderContext,
    ) = when (node) {
        is StringNode -> TemplateFilter.placeholder(node.value).valid()
        is MapNode ->
            if (node.size != 1) {
                ConfigFailure.Generic("Expected a single key in map").invalid()
            } else {
                val (operator, value) = node.map.entries.first()
                val filters =
                    when (value) {
                        is ArrayNode ->
                            value.elements
                                .map { decode(it, type, context) }
                                .sequence()
                                .mapInvalid { ConfigFailure.CollectionElementErrors(value, it) }

                        else -> decode(value, type, context).map { listOf(it) }
                    }
                when (operator) {
                    "not" -> filters.map(TemplateFilter::not).onSuccess { context.usedPaths += node.atKey(operator).path }
                    "any" -> filters.map(TemplateFilter::any).onSuccess { context.usedPaths += node.atKey(operator).path }
                    "and" -> filters.map(TemplateFilter::and).onSuccess { context.usedPaths += node.atKey(operator).path }
                    else -> ConfigFailure.Generic("Unknown operator $operator").invalid()
                }
            }
        else -> ConfigFailure.Generic("Unsupported node type ${node::class.jvmName}").invalid()
    }
}
