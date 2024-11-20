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
package com.xpdustry.flex.message

import arc.Core
import arc.util.CommandHandler.ResponseType
import arc.util.Strings
import arc.util.Time
import com.xpdustry.distributor.api.DistributorProvider
import com.xpdustry.distributor.api.audience.PlayerAudience
import com.xpdustry.distributor.api.plugin.PluginListener
import com.xpdustry.flex.FlexScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import mindustry.Vars
import mindustry.game.EventType.PlayerChatEvent
import mindustry.gen.SendChatMessageCallPacket
import mindustry.net.Administration
import mindustry.net.NetConnection
import mindustry.net.Packets.KickReason
import mindustry.net.ValidateException
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

internal class FlexChatMessageHook(
    private val messages: MessagePipeline,
    private val config: MessageConfig,
) : PluginListener {
    override fun onPluginInit() {
        if (config.chat) {
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

        val audience = DistributorProvider.get().audienceProvider.getPlayer(connection.player) as PlayerAudience
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
        DistributorProvider.get().eventBus.post(PlayerChatEvent(audience.player, message))

        FlexScope.launch {
            val isCommand = message.startsWith(Vars.netServer.clientCommands.getPrefix())
            val result =
                messages.pump(
                    MessageContext(
                        audience,
                        DistributorProvider.get().audienceProvider.server,
                        message,
                        if (isCommand) MessageContext.Kind.COMMAND else MessageContext.Kind.CHAT,
                    ),
                ).await()

            if (result.isBlank()) {
                return@launch
            } else if (isCommand) {
                ROOT_LOGGER.info("<&fi{}: {}&fr>", "&lk${audience.player.plainName()}", "&lw$result")
            }

            val hadCommand = CompletableDeferred<Boolean>()
            Core.app.post {
                // check if it's a command
                val response = Vars.netServer.clientCommands.handleMessage(result, audience.player)
                if (response.type == ResponseType.noCommand) { // no command to handle
                    hadCommand.complete(false)
                } else {
                    // a command was sent, now get the output
                    if (response.type != ResponseType.valid) {
                        val text = Vars.netServer.invalidHandler.handle(audience.player, response)
                        if (text != null) {
                            audience.player.sendMessage(text)
                        }
                    }
                    hadCommand.complete(true)
                }
            }

            if (withTimeoutOrNull(3.seconds) { hadCommand.await() } == true) {
                return@launch
            }

            ROOT_LOGGER.info(
                "&fi{}: {}",
                "&lc${audience.player.plainName()}",
                "&lw${Strings.stripColors(result)}",
            )

            messages.chat(
                audience,
                DistributorProvider.get().audienceProvider.players,
                result,
            )
        }
    }

    companion object {
        private val ROOT_LOGGER = LoggerFactory.getLogger("ROOT")
    }
}
