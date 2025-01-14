package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.manage.punish.PlayerAltController
import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController
import io.wdsj.asw.bukkit.setting.PluginSettings
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class ShadowListener : Listener { // TODO: Paper event handler
    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        if (PlayerShadowController.isShadowed(player)) {
            val recipients: MutableCollection<Player> = event.recipients
            recipients.clear()
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_ALTS_CHECK) && PlayerAltController.hasAlt(player)) {
                val alts = PlayerAltController.getAlts(player)
                for (alt in alts) {
                    val altPlayer = Bukkit.getPlayer(alt)
                    altPlayer?.let { recipients.add(it) }
                }
            }
            recipients.add(player)
        }
    }
}
