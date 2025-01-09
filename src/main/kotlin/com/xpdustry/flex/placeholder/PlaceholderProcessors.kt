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

import arc.graphics.Color
import com.xpdustry.distributor.api.audience.PlayerAudience
import com.xpdustry.distributor.api.component.render.ComponentStringBuilder
import com.xpdustry.distributor.api.component.style.ComponentColor
import com.xpdustry.distributor.api.key.Key
import com.xpdustry.distributor.api.key.StandardKeys
import com.xpdustry.flex.processor.Processor

internal val ArgumentProcessor =
    Processor<PlaceholderContext, String?> { ctx ->
        val parts = ctx.query.split(":", limit = 2)
        if (parts.size != 2) return@Processor null
        val (namespace, name) = parts
        ctx.arguments[Key.of(namespace, name, Any::class.java)]?.toString()
    }

internal val PlayerProcessor =
    Processor<PlaceholderContext, String?> { ctx ->
        if (ctx.subject !is PlayerAudience) return@Processor ""
        val player = ctx.subject.player
        when (ctx.query.lowercase()) {
            "name" -> player.info.plainLastName()
            "name_colored" -> player.name()
            "tile_x" -> player.tileX().toString()
            "tile_y" -> player.tileY().toString()
            "world_x" -> player.getX().toString()
            "world_y" -> player.getY().toString()
            "color" -> player.color().toHex()
            "team_color" -> player.team().color.toHex()
            else -> null
        }
    }

internal val AudienceProcessor =
    Processor<PlaceholderContext, String?> { ctx ->
        when (ctx.query.lowercase()) {
            "name" -> ctx.subject.metadata[StandardKeys.NAME]?.toString()
            "name_colored" ->
                ctx.subject.metadata[StandardKeys.DECORATED_NAME]?.let {
                    ComponentStringBuilder.mindustry(ctx.subject.metadata).append(it).toString()
                }
            "color" -> ctx.subject.metadata[StandardKeys.COLOR]?.toHex()
            "team_color" -> ctx.subject.metadata[StandardKeys.TEAM]?.color?.toHex()
            else -> null
        }
    }

internal val PermissionProcessor =
    Processor<PlaceholderContext, String?> { ctx ->
        if (ctx.subject.permissions.getPermission(ctx.query).asBoolean()) ctx.query else ""
    }

private fun Color.toHex() = String.format("#%06X", rgb888())

private fun ComponentColor.toHex() = String.format("#%06X", rgb)
