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

import com.xpdustry.flex.FlexScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

internal sealed interface PlaceholderFilter {
    fun accepts(context: PlaceholderContext): CompletableFuture<Boolean>

    data class Raw(val placeholder: String, val pipeline: PlaceholderPipeline) : PlaceholderFilter {
        override fun accepts(context: PlaceholderContext): CompletableFuture<Boolean> =
            pipeline
                .pump(context.copy(query = "%$placeholder%"), PlaceholderPipeline.Mode.PLACEHOLDER)
                .thenApply { it.isNotEmpty() }
    }

    data class Any(val filters: List<PlaceholderFilter>) : PlaceholderFilter {
        override fun accepts(context: PlaceholderContext) =
            FlexScope.future {
                filters.map { async { it.accepts(context).await() } }.any { it.await() }
            }
    }

    data class And(val filters: List<PlaceholderFilter>) : PlaceholderFilter {
        override fun accepts(context: PlaceholderContext) =
            FlexScope.future {
                filters.map { async { it.accepts(context).await() } }.all { it.await() }
            }
    }

    data class Not(val filters: List<PlaceholderFilter>) : PlaceholderFilter {
        override fun accepts(context: PlaceholderContext) =
            FlexScope.future {
                filters.map { async { it.accepts(context).await() } }.all { !it.await() }
            }
    }

    data object None : PlaceholderFilter {
        override fun accepts(context: PlaceholderContext): CompletableFuture<Boolean> = CompletableFuture.completedFuture(true)
    }
}
