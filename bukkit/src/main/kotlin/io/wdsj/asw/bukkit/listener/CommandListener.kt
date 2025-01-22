package io.wdsj.asw.bukkit.listener

import cc.baka9.catseedlogin.bukkit.CatSeedLoginAPI
import fr.xephi.authme.api.v3.AuthMeApi
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
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.message.MessageUtils
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent

class CommandListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        if (!settingsManager.getProperty(PluginSettings.ENABLE_CHAT_CHECK)) return
        val player = event.player
        val originalCommand =
            if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) event.message.replace(
                Utils.preProcessRegex.toRegex(), ""
            ) else event.message
        if (shouldNotProcess(player, originalCommand)) return
        val censoredWordList = sensitiveWordBs.findAll(originalCommand)
        val startTime = System.currentTimeMillis()
        if (censoredWordList.isNotEmpty()) {
            Utils.messagesFilteredNum.getAndIncrement()
            val processedCommand = sensitiveWordBs.replace(originalCommand)
            if (settingsManager.getProperty(PluginSettings.CHAT_METHOD)
                    .equals("cancel", ignoreCase = true)
            ) {
                event.isCancelled = true
            } else {
                if (Utils.isCommand(processedCommand)) {
                    event.message = processedCommand
                } else {
                    event.message = "/$processedCommand"
                }
            }
            if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                MessageUtils.sendMessage(
                    player,
                    messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT)
                        .replace("%integrated_player%", player.name)
                        .replace("%integrated_message%", originalCommand)
                )
            }
            if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                LoggingUtils.logViolation(
                    player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Chat)",
                    originalCommand + censoredWordList
                )
            }
            ViolationCounter.INSTANCE.incrementViolationCount(player)
            if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                VelocitySender.sendNotifyMessage(player, ModuleType.CHAT, originalCommand, censoredWordList)
            }
            if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                BungeeSender.sendNotifyMessage(player, ModuleType.CHAT, originalCommand, censoredWordList)
            }
            val endTime = System.currentTimeMillis()
            TimingUtils.addProcessStatistic(endTime, startTime)
            if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(
                player,
                ModuleType.CHAT,
                originalCommand,
                censoredWordList
            )
            if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) Punishment.punish(player)
        }
    }

    private fun shouldNotProcess(player: Player, message: String): Boolean {
        if (isInitialized && !CachingPermTool.hasPermission(
                PermissionsEnum.BYPASS, player) && !Utils.isCommandAndWhiteListed(message)) {
            if (isAuthMeAvailable && settingsManager.getProperty(PluginSettings.ENABLE_AUTHME_COMPATIBILITY)) {
                if (!AuthMeApi.getInstance().isAuthenticated(player)) return true
            }
            if (isCslAvailable && settingsManager.getProperty(PluginSettings.ENABLE_CSL_COMPATIBILITY)) {
                return !CatSeedLoginAPI.isLogin(player.name) || !CatSeedLoginAPI.isRegister(player.name)
            }
            return false
        }
        return true
    }
}
