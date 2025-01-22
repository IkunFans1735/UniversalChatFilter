package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.*
import io.wdsj.asw.bukkit.manage.notice.Notifier
import io.wdsj.asw.bukkit.manage.punish.Punishment
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter
import io.wdsj.asw.bukkit.permission.PermissionsEnum
import io.wdsj.asw.bukkit.permission.cache.CachingPermTool
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.util.LoggingUtils
import io.wdsj.asw.bukkit.util.PlayerUtils
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.message.MessageUtils
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.geysermc.floodgate.api.FloodgateApi

class PlayerLoginListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onLogin(event: PlayerLoginEvent) {
        if (!isInitialized || !settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_NAME_CHECK)) return
        val player = event.player
        if (CachingPermTool.hasPermission(PermissionsEnum.BYPASS, player)) return
        if (PlayerUtils.isNpc(player) && settingsManager.getProperty(PluginSettings.NAME_IGNORE_NPC)) return
        if (Bukkit.getPluginManager()
                .getPlugin("floodgate") != null && settingsManager.getProperty(
                PluginSettings.NAME_IGNORE_BEDROCK
            )
        ) {
            if (FloodgateApi.getInstance().isFloodgatePlayer(player.uniqueId)) return
        }
        val playerName = player.name
        val startTime = System.currentTimeMillis()
        val censoredWordList = sensitiveWordBs.findAll(playerName)
        if (censoredWordList.isNotEmpty()) {
            val processedPlayerName = sensitiveWordBs.replace(playerName)
            val playerIp = event.address.hostAddress
            Utils.messagesFilteredNum.getAndIncrement()
            if (settingsManager.getProperty(PluginSettings.NAME_METHOD)
                    .equals("replace", ignoreCase = true)
            ) {
                player.setDisplayName(processedPlayerName)
                player.setPlayerListName(processedPlayerName)
                if (settingsManager.getProperty(PluginSettings.NAME_SEND_MESSAGE)) {
                    getScheduler().runTaskLater({
                        MessageUtils.sendMessage(
                            player,
                            PluginMessages.MESSAGE_ON_NAME
                        )
                    }, 20L * 3L)
                }
            } else {
                event.disallow(
                    PlayerLoginEvent.Result.KICK_OTHER,
                    MessageUtils.retrieveMessage(PluginMessages.MESSAGE_ON_NAME)
                )
            }
            if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                LoggingUtils.logViolation(player.name + "(IP: " + playerIp + ")(Name)", playerName + censoredWordList)
            }
            ViolationCounter.INSTANCE.incrementViolationCount(player)
            if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                VelocitySender.sendNotifyMessage(player, ModuleType.NAME, playerName, censoredWordList)
            }
            if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                BungeeSender.sendNotifyMessage(player, ModuleType.NAME, playerName, censoredWordList)
            }
            val endTime = System.currentTimeMillis()
            TimingUtils.addProcessStatistic(endTime, startTime)
            if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(
                player,
                ModuleType.NAME,
                playerName,
                censoredWordList
            )
            if (settingsManager.getProperty(PluginSettings.NAME_PUNISH)) Punishment.punish(player)
        }
    }
}
