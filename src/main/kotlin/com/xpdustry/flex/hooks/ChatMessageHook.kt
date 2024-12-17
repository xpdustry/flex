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

import arc.Core
import arc.util.CommandHandler.ResponseType
import arc.util.Strings
import arc.util.Time
import com.xpdustry.distributor.api.Distributor
import com.xpdustry.distributor.api.plugin.PluginListener
import com.xpdustry.flex.FlexScope
import com.xpdustry.flex.message.FlexPlayerChatEvent
import com.xpdustry.flex.message.MessageContext
import com.xpdustry.flex.message.MessagePipeline
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import mindustry.Vars
import mindustry.game.EventType.PlayerChatEvent
import mindustry.gen.SendChatMessageCallPacket
import mindustry.net.Administration
import mindustry.net.NetConnection
import mindustry.net.Packets.KickReason
import mindustry.net.ValidateException
import org.slf4j.LoggerFactory

internal class ChatMessageHook(
    private val messages: MessagePipeline,
    private val hooks: HooksConfig,
) : PluginListener {
    override fun onPluginInit() {
        if (hooks.chat) {
            Vars.net.handleServer(SendChatMessageCallPacket::class.java, ::interceptChatPacket)
        }
    }

    private fun interceptChatPacket(
        connection: NetConnection,
        packet: SendChatMessageCallPacket,
    ) {
        if (connection.player == null || packet.message == null) {
            return
        }

        val audience = Distributor.get().audienceProvider.getPlayer(connection.player)
        var message = packet.message

        // do not receive chat messages from clients that are too young or not registered
        if (Vars.net.server() &&
            (
                Time.timeSinceMillis(audience.player.con().connectTime) < 500 ||
                    !audience.player.con().hasConnected ||
                    !audience.player.isAdded
            )
        ) {
            return
        }

        // detect and kick for foul play
        if (!audience.player.con().chatRate.allow(2000, Administration.Config.chatSpamLimit.num())) {
            audience.player.con().kick(KickReason.kick)
            Vars.netServer.admins.blacklistDos(audience.player.con().address)
            return
        }

        if (message.length > Vars.maxTextLength) {
            throw ValidateException(audience.player, "Player has sent a message above the text limit.")
        }

        message = message.replace("\n", "")
        Distributor.get().eventBus.post(PlayerChatEvent(audience.player, message))
        val prefix = Vars.netServer.clientCommands.getPrefix()

        FlexScope.launch {
            val isCommand = message.startsWith(prefix)
            var forServer =
                messages.pump(
                    MessageContext(
                        audience,
                        Distributor.get().audienceProvider.server,
                        if (isCommand && message.length >= prefix.length) message.drop(prefix.length) else message,
                        filter = true,
                        if (isCommand) MessageContext.Kind.COMMAND else MessageContext.Kind.CHAT,
                    ),
                ).await()

            if (forServer.isBlank()) {
                return@launch
            }

            if (isCommand) {
                forServer = "$prefix$forServer"
                root.info("<&fi{}: {}&fr>", "&lk${audience.player.plainName()}", "&lw$forServer")
                Core.app.post {
                    val response = Vars.netServer.clientCommands.handleMessage(forServer, audience.player)
                    when (response.type) {
                        ResponseType.valid -> Unit
                        else -> {
                            val text = Vars.netServer.invalidHandler.handle(audience.player, response)
                            if (text != null) audience.player.sendMessage(text)
                        }
                    }
                }
                return@launch
            }

            root.info("&fi{}: {}", "&lc${audience.player.plainName()}", "&lw${Strings.stripColors(forServer)}")

            Core.app.post {
                Distributor.get().eventBus.post(FlexPlayerChatEvent(audience, forServer))
            }

            messages.broadcast(audience, Distributor.get().audienceProvider.players, message).await()
        }
    }

    companion object {
        private val root = LoggerFactory.getLogger("ROOT")
    }
}
