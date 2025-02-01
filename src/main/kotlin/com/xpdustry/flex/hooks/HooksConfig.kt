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
package com.xpdustry.flex.hooks

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal data class HooksConfig(
    val chat: Boolean = true,
    val join: Boolean = true,
    val quit: Boolean = true,
    val name: Name = Name(),
) {
    data class Name(
        val enabled: Boolean = true,
        val maximumNameSize: Int = 512,
        val updateInterval: Duration = 500.milliseconds,
    ) {
        init {
            require(maximumNameSize >= 1) { "Maximum name size is lower than 1, got $maximumNameSize" }
            require(updateInterval > Duration.ZERO) {
                "Update interval duration must be greater than 0, got $updateInterval"
            }
        }
    }
}
