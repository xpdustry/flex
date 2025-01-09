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
package com.xpdustry.flex.placeholder

import arc.Core
import arc.mock.MockSettings
import com.xpdustry.distributor.api.audience.Audience
import com.xpdustry.flex.TestPlugin
import com.xpdustry.flex.placeholder.template.TemplateManagerImpl
import com.xpdustry.flex.placeholder.template.TemplateProcessor
import com.xpdustry.flex.placeholder.template.TemplateStep
import mindustry.Vars
import mindustry.core.NetServer
import mindustry.net.ArcNetProvider
import mindustry.net.Net
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PlaceholderPipelineImplTest {
    @BeforeEach
    fun prepare() {
        Core.settings = MockSettings()
        Vars.net = Net(ArcNetProvider())
        Vars.netServer = NetServer()
    }

    @AfterEach
    fun cleanup() {
        Core.settings = null
        Vars.netServer = null
    }

    @Test
    fun `test simple placeholder`() {
        val pipeline = PlaceholderPipelineImpl(TestPlugin)
        pipeline.register("test") { "value" }
        Assertions.assertEquals("test", pipeline.pump(PlaceholderContext(Audience.empty(), "test")))
        Assertions.assertEquals("value", pipeline.pump(PlaceholderContext(Audience.empty(), "%test%")))
        Assertions.assertEquals("hello value world", pipeline.pump(PlaceholderContext(Audience.empty(), "hello %test% world")))
    }

    @Test
    fun `test multiple placeholders`() {
        val pipeline = PlaceholderPipelineImpl(TestPlugin)
        pipeline.register("test1") { "value1" }
        pipeline.register("test2") { "value2" }
        Assertions.assertEquals(
            "hello value1 value2 world",
            pipeline.pump(PlaceholderContext(Audience.empty(), "hello %test1% %test2% world")),
        )
    }

    @Test
    fun `test empty placeholder`() {
        val pipeline = PlaceholderPipelineImpl(TestPlugin)
        pipeline.register("test") { "" }
        Assertions.assertEquals("hello  world", pipeline.pump(PlaceholderContext(Audience.empty(), "hello %test% world")))
    }

    @Test
    fun `test null placeholder`() {
        val pipeline = PlaceholderPipelineImpl(TestPlugin)
        pipeline.register("test") { null }
        Assertions.assertEquals("hello %test% world", pipeline.pump(PlaceholderContext(Audience.empty(), "hello %test% world")))
    }

    @Test
    fun `test unknown placeholder`() {
        val pipeline = PlaceholderPipelineImpl(TestPlugin)
        Assertions.assertEquals("hello %test% world", pipeline.pump(PlaceholderContext(Audience.empty(), "hello %test% world")))
    }

    @Test
    fun `test throwing placeholder`() {
        val pipeline = PlaceholderPipelineImpl(TestPlugin)
        pipeline.register("test") { throw RuntimeException("Expected") }
        Assertions.assertEquals("hello %test% world", pipeline.pump(PlaceholderContext(Audience.empty(), "hello %test% world")))
    }

    @Test
    fun `test template`() {
        val pipeline = PlaceholderPipelineImpl(TestPlugin)
        pipeline.register("test") { "value" }
        val processor = TemplateProcessor(pipeline, TemplateManagerImpl(mapOf("test" to listOf(TemplateStep("hello %test% world")))))
        pipeline.register("template", processor)
        Assertions.assertEquals("hello value world", pipeline.pump(PlaceholderContext(Audience.empty(), "%template:test%")))
    }

    @ParameterizedTest
    @ValueSource(strings = ["$", "\\"])
    fun `test escape meta chars`(char: String) {
        val pipeline = PlaceholderPipelineImpl(TestPlugin)
        pipeline.register("test") { char }
        val processor = TemplateProcessor(pipeline, TemplateManagerImpl(mapOf("test" to listOf(TemplateStep("hello %test% world")))))
        pipeline.register("template", processor)
        Assertions.assertEquals(char, pipeline.pump(PlaceholderContext(Audience.empty(), char)))
        Assertions.assertEquals("hello $char world", pipeline.pump(PlaceholderContext(Audience.empty(), "%template:test%")))
    }
}
