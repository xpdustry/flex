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

internal class PlaceholderFilterDecoder(private val pipeline: PlaceholderPipeline) : NullHandlingDecoder<PlaceholderFilter> {
    override fun supports(type: KType) = type.classifier == PlaceholderFilter::class

    override fun safeDecode(
        node: Node,
        type: KType,
        context: DecoderContext,
    ) = when (node) {
        is StringNode -> PlaceholderFilter.Raw(node.value, pipeline).valid()
        is MapNode ->
            if (node.size != 1) {
                ConfigFailure.Generic("Expected a single key in map").invalid()
            } else {
                val (key, value) = node.map.entries.first()
                val filters =
                    when (value) {
                        is ArrayNode ->
                            value.elements
                                .map { decode(it, type, context) }
                                .sequence()
                                .mapInvalid { ConfigFailure.CollectionElementErrors(value, it) }

                        else -> decode(value, type, context).map { listOf(it) }
                    }
                when (key) {
                    "not" -> filters.map(PlaceholderFilter::Not)
                    "any" -> filters.map(PlaceholderFilter::Any)
                    "and" -> filters.map(PlaceholderFilter::And)
                    else -> ConfigFailure.Generic("Unknown key $key").invalid()
                }
            }
        else -> ConfigFailure.Generic("Unsupported node type ${node::class}").invalid()
    }
}
