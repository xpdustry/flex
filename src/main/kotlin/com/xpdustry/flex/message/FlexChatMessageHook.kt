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
import com.xpdustry.distributor.api.annotation.EventHandler
import com.xpdustry.distributor.api.audience.Audience
import com.xpdustry.distributor.api.audience.PlayerAudience
import com.xpdustry.distributor.api.key.MutableKeyContainer
import com.xpdustry.distributor.api.key.StandardKeys
import com.xpdustry.distributor.api.player.MUUID
import com.xpdustry.distributor.api.plugin.PluginListener
import com.xpdustry.flex.FlexScope
import com.xpdustry.flex.placeholder.PlaceholderContext
import com.xpdustry.flex.placeholder.PlaceholderPipeline
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import mindustry.Vars
import mindustry.game.EventType
import mindustry.game.EventType.PlayerChatEvent
import mindustry.gen.SendChatMessageCallPacket
import mindustry.net.Administration
import mindustry.net.NetConnection
import mindustry.net.Packets.KickReason
import mindustry.net.ValidateException
import org.slf4j.LoggerFactory

internal class FlexChatMessageHook(
    private val placeholders: PlaceholderPipeline,
    private val messages: MessagePipeline,
) : PluginListener {
    private val foo = mutableSetOf<MUUID>()

    override fun onPluginInit() {
        Vars.netServer.addPacketHandler("fooCheck") { player, _ -> foo += MUUID.from(player) }
        Vars.net.handleServer(SendChatMessageCallPacket::class.java, ::interceptChatPacket)
    }

    @EventHandler
    fun onPlayerQuit(event: EventType.PlayerLeave) {
        foo -= MUUID.from(event.player)
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

        FlexScope.launch server@{
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
                return@server
            } else if (isCommand) {
                ROOT_LOGGER.info("<&fi{}: {}&fr>", "&lk${audience.player.plainName()}", "&lw$result")
            }

            val resume = CompletableDeferred<Boolean>()
            Core.app.post {
                // check if it's a command
                val response = Vars.netServer.clientCommands.handleMessage(result, audience.player)
                if (response.type == ResponseType.noCommand) { // no command to handle
                    resume.complete(true)
                } else {
                    // a command was sent, now get the output
                    if (response.type != ResponseType.valid) {
                        val text = Vars.netServer.invalidHandler.handle(audience.player, response)
                        if (text != null) {
                            audience.player.sendMessage(text)
                        }
                    }
                    resume.complete(false)
                }
            }

            if (!resume.await()) {
                return@server
            }

            ROOT_LOGGER.info(
                "&fi{}: {}",
                "&lc${audience.player.plainName()}",
                "&lw${Strings.stripColors(result)}",
            )

            DistributorProvider.get().audienceProvider.players.audiences.forEach { target ->
                launch player@{
                    val processed =
                        messages.pump(
                            MessageContext(
                                audience,
                                target,
                                message,
                                MessageContext.Kind.CHAT,
                            ),
                        ).await()

                    if (processed.isBlank()) {
                        return@player
                    }

                    val formatted =
                        placeholders.pump(
                            PlaceholderContext(
                                audience,
                                "mindustry-chat",
                                MutableKeyContainer.create().apply { set(PlaceholderPipeline.MESSAGE, message) },
                            ),
                            PlaceholderPipeline.Mode.PRESET,
                        ).await()

                    if (formatted.isBlank()) {
                        return@player
                    }

                    target.sendMessage(
                        DistributorProvider.get().mindustryComponentDecoder.decode(formatted),
                        DistributorProvider.get().mindustryComponentDecoder.decode(processed),
                        audience.takeUnless(::isFooClient) ?: Audience.empty(),
                    )
                }
            }
        }
    }

    private fun isFooClient(audience: Audience) = audience.metadata[StandardKeys.MUUID]?.let { foo.contains(it) } ?: false

    companion object {
        private val ROOT_LOGGER = LoggerFactory.getLogger("ROOT")
    }
}
