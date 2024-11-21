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

import com.xpdustry.flex.FlexAPI

internal sealed interface PlaceholderFilter {
    fun accepts(context: PlaceholderContext): Boolean

    data class Raw(val placeholder: String) : PlaceholderFilter {
        override fun accepts(context: PlaceholderContext): Boolean =
            FlexAPI.get().placeholders
                .pump(context.copy(query = "%$placeholder%"))
                .isNotEmpty()
    }

    data class Any(val filters: List<PlaceholderFilter>) : PlaceholderFilter {
        override fun accepts(context: PlaceholderContext) = filters.any { it.accepts(context) }
    }

    data class And(val filters: List<PlaceholderFilter>) : PlaceholderFilter {
        override fun accepts(context: PlaceholderContext) = filters.all { it.accepts(context) }
    }

    data class Not(val filters: List<PlaceholderFilter>) : PlaceholderFilter {
        override fun accepts(context: PlaceholderContext) = filters.none { it.accepts(context) }
    }

    data object None : PlaceholderFilter {
        override fun accepts(context: PlaceholderContext): Boolean = true
    }
}
