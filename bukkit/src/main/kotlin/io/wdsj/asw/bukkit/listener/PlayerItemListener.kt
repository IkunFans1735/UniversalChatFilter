package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
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
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemHeldEvent

class PlayerItemListener : Listener {
    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerHeldItem(event: PlayerItemHeldEvent) {
        if (!AdvancedSensitiveWords.isInitialized || !settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_ITEM_CHECK)) return
        val player = event.player
        if (CachingPermTool.hasPermission(PermissionsEnum.BYPASS, player)) return
        val item = player.inventory.getItem(event.newSlot)
        if (item != null && item.hasItemMeta()) {
            val meta = item.itemMeta
            if (meta != null && meta.hasDisplayName()) {
                var originalName = meta.displayName
                val startTime = System.currentTimeMillis()
                if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalName =
                    originalName.replace(
                        Utils.preProcessRegex.toRegex(), ""
                    )
                val censoredWordList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalName)
                if (censoredWordList.isNotEmpty()) {
                    Utils.messagesFilteredNum.getAndIncrement()
                    val processedName = AdvancedSensitiveWords.sensitiveWordBs.replace(originalName)
                    if (settingsManager.getProperty(PluginSettings.ITEM_METHOD)
                            .equals("cancel", ignoreCase = true)
                    ) {
                        event.isCancelled = true
                    } else {
                        meta.setDisplayName(processedName)
                        item.setItemMeta(meta)
                    }
                    if (settingsManager.getProperty(PluginSettings.ITEM_SEND_MESSAGE)) {
                        MessageUtils.sendMessage(player, PluginMessages.MESSAGE_ON_ITEM)
                    }
                    if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                        LoggingUtils.logViolation(
                            player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Item)",
                            originalName + censoredWordList
                        )
                    }
                    ViolationCounter.INSTANCE.incrementViolationCount(player)
                    if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                        BungeeSender.sendNotifyMessage(player, ModuleType.ITEM, originalName, censoredWordList)
                    }
                    if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                        VelocitySender.sendNotifyMessage(player, ModuleType.ITEM, originalName, censoredWordList)
                    }
                    val endTime = System.currentTimeMillis()
                    TimingUtils.addProcessStatistic(endTime, startTime)
                    if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(
                        player,
                        ModuleType.ITEM,
                        originalName,
                        censoredWordList
                    )
                    if (settingsManager.getProperty(PluginSettings.ITEM_PUNISH)) Punishment.punish(
                        player
                    )
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onDrop(event: PlayerDropItemEvent) {
        if (!AdvancedSensitiveWords.isInitialized || !settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_ITEM_CHECK)) return
        val player = event.player
        if (CachingPermTool.hasPermission(PermissionsEnum.BYPASS, player)) return
        val item = event.itemDrop.itemStack
        if (item.hasItemMeta()) {
            val meta = item.itemMeta
            if (meta != null && meta.hasDisplayName()) {
                var originalName = meta.displayName
                val startTime = System.currentTimeMillis()
                if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalName =
                    originalName.replace(
                        Utils.preProcessRegex.toRegex(), ""
                    )
                val censoredWordList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalName)
                if (censoredWordList.isNotEmpty()) {
                    Utils.messagesFilteredNum.getAndIncrement()
                    val processedName = AdvancedSensitiveWords.sensitiveWordBs.replace(originalName)
                    if (settingsManager.getProperty(PluginSettings.ITEM_METHOD)
                            .equals("cancel", ignoreCase = true)
                    ) {
                        event.isCancelled = true
                    } else {
                        meta.setDisplayName(processedName)
                        item.setItemMeta(meta)
                    }
                    if (settingsManager.getProperty(PluginSettings.ITEM_SEND_MESSAGE)) {
                        MessageUtils.sendMessage(
                            player,
                            PluginMessages.MESSAGE_ON_ITEM
                        )
                    }
                    if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                        LoggingUtils.logViolation(
                            player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Item)",
                            originalName + censoredWordList
                        )
                    }
                    ViolationCounter.INSTANCE.incrementViolationCount(player)
                    if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                        VelocitySender.sendNotifyMessage(player, ModuleType.ITEM, originalName, censoredWordList)
                    }
                    if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                        BungeeSender.sendNotifyMessage(player, ModuleType.ITEM, originalName, censoredWordList)
                    }
                    val endTime = System.currentTimeMillis()
                    TimingUtils.addProcessStatistic(endTime, startTime)
                    if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(
                        player,
                        ModuleType.ITEM,
                        originalName,
                        censoredWordList
                    )
                    if (settingsManager.getProperty(PluginSettings.ITEM_PUNISH)) Punishment.punish(
                        player
                    )
                }
            }
        }
    }
}
