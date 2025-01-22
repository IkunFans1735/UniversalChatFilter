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
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.context.SignContext
import io.wdsj.asw.bukkit.util.message.MessageUtils
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent

// TODO: Paper event handler
class SignListener : Listener {
    private var outMessage: String = ""
    private var outList: List<String> = ArrayList()
    private var outProcessedMessage = ""

    @EventHandler(priority = EventPriority.LOW)
    fun onSign(event: SignChangeEvent) {
        if (!isInitialized) return
        if (!settingsManager.getProperty(PluginSettings.ENABLE_SIGN_EDIT_CHECK)) return
        val player = event.player
        if (CachingPermTool.hasPermission(PermissionsEnum.BYPASS, player) || event.lines.isEmpty()) return
        var shouldSendMessage = false
        val startTime = System.currentTimeMillis()
        val indexList: MutableList<Int> = ArrayList()
        val originalMultiMessages = StringBuilder()
        for (line in 0 until event.lines.size) {
            var originalMessage = event.getLine(line) ?: continue
            if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalMessage =
                originalMessage.replace(
                    Utils.preProcessRegex.toRegex(), ""
                )
            val censoredWordList = sensitiveWordBs.findAll(originalMessage)
            if (censoredWordList.isNotEmpty()) {
                val processedMessage = sensitiveWordBs.replace(originalMessage)
                outMessage = originalMessage
                outProcessedMessage = processedMessage
                outList = censoredWordList
                if (settingsManager.getProperty(PluginSettings.SIGN_METHOD)
                        .equals("cancel", ignoreCase = true)
                ) {
                    event.isCancelled = true
                }
                event.setLine(line, processedMessage)
                shouldSendMessage = true
            } else if (originalMessage.trim().isNotEmpty()) {
                indexList.add(line)
                originalMultiMessages.append(originalMessage)
            }
        }
        if (settingsManager.getProperty(PluginSettings.SIGN_MULTI_LINE_CHECK) && indexList.isNotEmpty()) {
            val originalMessagesString = originalMultiMessages.toString()
            val censoredWordList = sensitiveWordBs.findAll(originalMessagesString)
            if (censoredWordList.isNotEmpty()) {
                val processedMessagesString = sensitiveWordBs.replace(originalMessagesString)
                outMessage = originalMessagesString
                outProcessedMessage = processedMessagesString
                outList = censoredWordList
                if (settingsManager.getProperty(PluginSettings.SIGN_METHOD)
                        .equals("cancel", ignoreCase = true)
                ) {
                    shouldSendMessage = true
                    event.isCancelled = true
                } else {
                    shouldSendMessage = true
                    for (i in indexList) {
                        event.setLine(i, processedMessagesString)
                    }
                }
            }
        }

        // Sign context check
        if (settingsManager.getProperty(PluginSettings.SIGN_CONTEXT_CHECK) && !shouldSendMessage) {
            val originalAllMessage = event.lines.joinToString("")
            SignContext.addMessage(player, originalAllMessage)
            val originalContext = SignContext.getHistory(player).joinToString("")
            val censoredContextList = sensitiveWordBs.findAll(originalContext)
            if (censoredContextList.isNotEmpty()) {
                SignContext.pollPlayerContext(player)
                val processedContext = sensitiveWordBs.replace(originalContext)
                shouldSendMessage = true
                event.isCancelled = true
                outMessage = originalContext
                outProcessedMessage = processedContext
                outList = censoredContextList
            }
        }

        if (settingsManager.getProperty(PluginSettings.SIGN_SEND_MESSAGE) && shouldSendMessage) {
            MessageUtils.sendMessage(
                player,
                PluginMessages.MESSAGE_ON_SIGN
            )
        }

        if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION) && shouldSendMessage) {
            val location = event.block.location
            val locationLog = "World: ${location.world?.name ?: "Unknown"}, X: ${location.x}, Y: ${location.y}, Z: ${location.z}"
            LoggingUtils.logViolation(player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Sign)(" + locationLog + ")", outMessage + outList)
        }

        if (shouldSendMessage) {
            ViolationCounter.INSTANCE.incrementViolationCount(player)
        }

        if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY) && shouldSendMessage) {
            VelocitySender.sendNotifyMessage(player, ModuleType.SIGN, outMessage, outList)
        }

        if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD) && shouldSendMessage) {
            BungeeSender.sendNotifyMessage(player, ModuleType.SIGN, outMessage, outList)
        }

        if (shouldSendMessage) {
            Utils.messagesFilteredNum.getAndIncrement()
            val endTime = System.currentTimeMillis()
            TimingUtils.addProcessStatistic(endTime, startTime)
            if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(
                player,
                ModuleType.SIGN,
                outMessage,
                outList
            )
            if (settingsManager.getProperty(PluginSettings.SIGN_PUNISH)) Punishment.punish(player)
        }
    }
}
