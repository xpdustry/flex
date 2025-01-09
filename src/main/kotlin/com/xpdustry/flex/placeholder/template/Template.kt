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

import com.sksamuel.hoplite.ConfigAlias
import com.xpdustry.flex.FlexAPI
import com.xpdustry.flex.placeholder.PlaceholderContext

public data class Template(public val steps: List<TemplateStep>)

public data class TemplateStep
@JvmOverloads
constructor(public val text: String, @ConfigAlias("if") public val filter: TemplateFilter = TemplateFilter.none())

public fun interface TemplateFilter {
    public fun accepts(context: PlaceholderContext): Boolean

    public companion object {
        @JvmStatic public fun none(): TemplateFilter = None

        @JvmStatic public fun placeholder(placeholder: String): TemplateFilter = Placeholder(placeholder)

        @JvmStatic public fun any(vararg filters: TemplateFilter): TemplateFilter = Any(filters.toList())

        @JvmStatic public fun any(filters: List<TemplateFilter>): TemplateFilter = Any(filters)

        @JvmStatic public fun and(vararg filters: TemplateFilter): TemplateFilter = And(filters.toList())

        @JvmStatic public fun and(filters: List<TemplateFilter>): TemplateFilter = And(filters)

        @JvmStatic public fun not(vararg filters: TemplateFilter): TemplateFilter = Not(filters.toList())

        @JvmStatic public fun not(filters: List<TemplateFilter>): TemplateFilter = Not(filters)
    }
}

private data class Placeholder(val placeholder: String) : TemplateFilter {
    override fun accepts(context: PlaceholderContext): Boolean =
        FlexAPI.get().placeholders.pump(context.copy(query = "%$placeholder%")).let {
            it.isNotEmpty() && it != "%$placeholder%"
        }
}

private data class Any(val filters: List<TemplateFilter>) : TemplateFilter {
    override fun accepts(context: PlaceholderContext): Boolean = filters.any { it.accepts(context) }
}

private data class And(val filters: List<TemplateFilter>) : TemplateFilter {
    override fun accepts(context: PlaceholderContext): Boolean = filters.all { it.accepts(context) }
}

private data class Not(val filters: List<TemplateFilter>) : TemplateFilter {
    override fun accepts(context: PlaceholderContext): Boolean = filters.none { it.accepts(context) }
}

private data object None : TemplateFilter {
    override fun accepts(context: PlaceholderContext): Boolean = true
}
