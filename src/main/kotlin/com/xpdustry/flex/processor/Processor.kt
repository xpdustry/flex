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
package com.xpdustry.flex.processor

import arc.Core
import java.util.concurrent.CompletableFuture
import java.util.function.Function

public fun interface Processor<I : Any, O : Any> {
    public fun process(context: I): CompletableFuture<O>

    public companion object {
        @JvmStatic
        public fun <I : Any, O : Any> simple(process: Function<I, O>): Processor<I, O> =
            Processor {
                CompletableFuture.completedFuture(process.apply(it))
            }

        @JvmStatic
        public fun <I : Any, O : Any> synchronous(process: Function<I, O>): Processor<I, O> =
            Processor {
                CompletableFuture.supplyAsync({ process.apply(it) }, Core.app::post)
            }
    }
}