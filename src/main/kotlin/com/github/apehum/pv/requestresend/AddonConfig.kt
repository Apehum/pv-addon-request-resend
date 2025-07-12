package com.github.apehum.pv.requestresend

import su.plo.config.Config
import su.plo.config.ConfigField
import su.plo.config.provider.ConfigurationProvider
import su.plo.config.provider.toml.TomlConfiguration
import su.plo.voice.api.server.PlasmoVoiceServer
import java.io.File

@Config
class AddonConfig {

    @ConfigField(
        comment = "Interval between request sends"
    )
    val sendIntervalMillis = 1000

    @ConfigField(
        comment = "For how long requests will be sent after player joined the server"
    )
    val sendsTimeoutMillis = 10_000

    @ConfigField(
        comment = """
            Whether "plasmo:voice/v2/installed" channel should be checked before sending the request packet.
        """)
    val flagChannelCheck = true

    companion object {

        private val toml = ConfigurationProvider.getProvider<ConfigurationProvider>(TomlConfiguration::class.java)

        fun loadConfig(server: PlasmoVoiceServer): AddonConfig {

            val addonFolder = File(server.minecraftServer.configsFolder, "pv-addon-request-resend")
            val configFile = File(addonFolder, "config.toml")

            return toml.load<AddonConfig>(AddonConfig::class.java, configFile, false)
                .also { toml.save(AddonConfig::class.java, it, configFile) }
        }
    }
}
