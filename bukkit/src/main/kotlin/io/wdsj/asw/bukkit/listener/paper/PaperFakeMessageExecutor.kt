package io.wdsj.asw.bukkit.listener.paper

import io.papermc.paper.event.player.AsyncChatEvent
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.annotation.PaperEventHandler
import io.wdsj.asw.bukkit.listener.abstraction.AbstractFakeMessageExecutor
import io.wdsj.asw.bukkit.manage.punish.PlayerAltController
import io.wdsj.asw.bukkit.setting.PluginSettings
import net.kyori.adventure.audience.Audience
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

@PaperEventHandler
class PaperFakeMessageExecutor : Listener, AbstractFakeMessageExecutor() {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        if (shouldFakeMessage(player)) {
            selfDecrement(player) // Decrease even the fake message is disabled
            if (settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL)) {
                val players: MutableSet<Audience> = event.viewers()
                players.clear()
                if (settingsManager.getProperty(PluginSettings.ENABLE_ALTS_CHECK) && PlayerAltController.hasAlt(player)) {
                    val alts = PlayerAltController.getAlts(player)
                    for (alt in alts) {
                        val altPlayer = Bukkit.getPlayer(alt)
                        altPlayer?.let { players.add(it) }
                    }
                }
                players.add(player)
            }
        }
    }
}
