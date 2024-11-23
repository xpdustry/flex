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
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

public abstract class AbstractPriorityProcessorPipeline<I, O>(
    public val plugin: MindustryPlugin,
    private val identifier: String,
) : PriorityProcessorPipeline<I, O> {
    private val _processors = ConcurrentHashMap<String, PriorityProcessor>()
    public val processors: Map<String, PriorityProcessor> get() = Collections.unmodifiableMap(_processors)

    override fun register(
        name: String,
        priority: Priority,
        processor: Processor<I, O>,
    ) {
        if (_processors.containsKey(name)) {
            throw IllegalArgumentException("Processor with name $name already registered")
        }
        val wrapped = PriorityProcessor(processor, priority)
        _processors[name] = wrapped
        plugin.logger.debug("Registered processor {} to {} with priority {}", name, identifier, priority)
    }

    public inner class PriorityProcessor(
        delegate: Processor<I, O>,
        public val priority: Priority,
    ) : Processor<I, O> by delegate, Comparable<PriorityProcessor> {
        override fun compareTo(other: PriorityProcessor): Int = priority.compareTo(other.priority)
    }
}
