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
package com.xpdustry.flex.placeholder.template

public interface TemplateManager {
    public fun getTemplate(name: String): Template?

    public fun hasTemplate(name: String): Boolean

    public fun setDefaultTemplate(
        name: String,
        template: Template,
    )

    public companion object {
        public const val JOIN_TEMPLATE_NAME: String = "mindustry_join"
        public const val QUIT_TEMPLATE_NAME: String = "mindustry_quit"
        public const val CHAT_TEMPLATE_NAME: String = "mindustry_chat"
        public const val NAME_TEMPLATE_NAME: String = "mindustry_name"
    }
}
