package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.messagesManager
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.permission.PermissionsEnum
import io.wdsj.asw.bukkit.permission.cache.CachingPermTool
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.util.PlayerUtils
import io.wdsj.asw.bukkit.util.message.MessageUtils
import io.wdsj.asw.common.update.Updater
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class JoinUpdateNotifier : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (!settingsManager.getProperty(PluginSettings.CHECK_FOR_UPDATE)
            || !CachingPermTool.hasPermission(PermissionsEnum.UPDATE, player)
            || PlayerUtils.isNpc(player)) return

        if (Updater.hasUpdate()) {
            MessageUtils.sendMessage(player,
                messagesManager.getProperty(PluginMessages.UPDATE_AVAILABLE)
                    .replace("%current_version%", Updater.getCurrentVersion())
                    .replace("%latest_version%", Updater.getLatestVersion())
            )
        }
    }
}