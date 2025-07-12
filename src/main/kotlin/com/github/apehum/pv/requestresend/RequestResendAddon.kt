package com.github.apehum.pv.requestresend

import com.google.common.collect.Maps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.plo.slib.api.event.player.McPlayerQuitEvent
import su.plo.voice.api.addon.AddonInitializer
import su.plo.voice.api.addon.AddonLoaderScope
import su.plo.voice.api.addon.InjectPlasmoVoice
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.event.config.VoiceServerConfigReloadedEvent
import su.plo.voice.api.server.event.connection.TcpPacketSendEvent
import su.plo.voice.api.server.player.VoiceServerPlayer
import su.plo.voice.proto.packets.tcp.clientbound.PlayerInfoRequestPacket
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@Addon(
    id = "pv-addon-request-resend",
    version = BuildConstants.VERSION,
    authors = ["Apehum"],
    scope = AddonLoaderScope.SERVER,
)
class RequestResendAddon : AddonInitializer {
    @InjectPlasmoVoice
    private lateinit var voiceServer: PlasmoVoiceServer

    private lateinit var config: AddonConfig

    private val tickerInterval = 500.milliseconds
    private val flagChannel = "plasmo:voice/v2/installed"

    private val requestSends: MutableMap<UUID, PlayerRequestSendInfo> = Maps.newConcurrentMap()

    private var tickJob: Job? = null

    init {
        McPlayerQuitEvent.registerListener { requestSends.remove(it.uuid) }
    }

    override fun onAddonInitialize() {
        loadConfig()

        tickJob = CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                // invalidate if:
                // - response is received
                // - player become offline
                // - first send was [sendsTimeout] ago
                requestSends.entries
                    .filter { (_, sendInfo) ->
                        val player = sendInfo.player

                        player.publicKey.isPresent
                                || !player.instance.isOnline
                                || System.currentTimeMillis() - sendInfo.firstSendTimestamp > config.sendsTimeoutMillis
                    }
                    .forEach { requestSends.remove(it.key) }

                requestSends.entries
                    .filter { System.currentTimeMillis() - it.value.lastSendTimestamp > config.sendIntervalMillis }
                    .filter { !config.flagChannelCheck || it.value.player.instance.registeredChannels.contains(flagChannel) }
                    .forEach { (_, sendInfo) ->
                        voiceServer.tcpPacketManager.requestPlayerInfo(sendInfo.player)
                        sendInfo.lastSendTimestamp = System.currentTimeMillis()
                    }

                delay(tickerInterval)
            }
        }
    }

    override fun onAddonShutdown() {
        tickJob?.cancel()
    }

    private fun loadConfig() {
        config = AddonConfig.loadConfig(voiceServer)
    }

    @EventSubscribe
    fun onConfigReloaded(event: VoiceServerConfigReloadedEvent) {
        loadConfig()
    }

    @EventSubscribe
    fun onPacketSend(event: TcpPacketSendEvent) {
        if (event.packet !is PlayerInfoRequestPacket) return

        val player = event.player

        val sendInfo = requestSends.computeIfAbsent(player.instance.uuid) {
            PlayerRequestSendInfo(player, System.currentTimeMillis(), 0L)
        }
        sendInfo.lastSendTimestamp = System.currentTimeMillis()
    }

    data class PlayerRequestSendInfo(
        val player: VoiceServerPlayer,
        val firstSendTimestamp: Long,
        var lastSendTimestamp: Long,
    )
}
