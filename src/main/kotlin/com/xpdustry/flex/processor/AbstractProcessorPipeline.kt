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

import com.xpdustry.distributor.api.plugin.MindustryPlugin
import com.xpdustry.distributor.api.util.Priority

public abstract class AbstractProcessorPipeline<I : Any, O : Any>(
    public val plugin: MindustryPlugin,
    private val identifier: String,
) : ProcessorPipeline<I, O> {
    protected val processors: List<Processor<I, O>> get() = inner.map { it.processor }

    protected fun processor(name: String): Processor<I, O>? = inner.firstOrNull { it.name == name }?.processor

    private val inner = mutableListOf<ProcessorWithData>()

    override fun register(
        name: String,
        priority: Priority,
        processor: Processor<I, O>,
    ) {
        if (inner.any { it.name == name }) {
            throw IllegalArgumentException("Processor with name $name already registered")
        }
        inner.add(ProcessorWithData(processor, name, priority))
        inner.sortBy { it.priority }
        plugin.logger.debug("Registered processor {} to {} with priority {}", name, identifier, priority)
    }

    private inner class ProcessorWithData(
        val processor: Processor<I, O>,
        val name: String,
        val priority: Priority,
    )
}
