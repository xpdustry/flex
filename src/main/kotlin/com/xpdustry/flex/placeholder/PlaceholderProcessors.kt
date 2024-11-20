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

import com.xpdustry.distributor.api.audience.PlayerAudience
import com.xpdustry.distributor.api.key.Key
import com.xpdustry.flex.processor.Processor

internal val ArgumentProcessor =
    Processor<PlaceholderContext, String> { ctx ->
        val parts = ctx.query.split("_", limit = 2)
        if (parts.size != 2) return@Processor ""
        val (namespace, name) = parts
        ctx.arguments[Key.of(namespace, name, Any::class.java)]?.toString() ?: ""
    }

internal val PlayerProcessor =
    Processor<PlaceholderContext, String> { ctx ->
        if (ctx.subject !is PlayerAudience) return@Processor ""
        val player = ctx.subject.player
        when (ctx.query.lowercase()) {
            "name_raw" -> player.plainName()
            "name_colored" -> player.coloredName()
            "tile_x" -> player.tileX().toString()
            "tile_y" -> player.tileY().toString()
            "world_x" -> player.getX().toString()
            "world_y" -> player.getY().toString()
            else -> ""
        }
    }

internal val PermissionProcessor =
    Processor<PlaceholderContext, String> { ctx ->
        if (ctx.subject.permissions.getPermission(ctx.query).asBoolean()) ctx.query else ""
    }
