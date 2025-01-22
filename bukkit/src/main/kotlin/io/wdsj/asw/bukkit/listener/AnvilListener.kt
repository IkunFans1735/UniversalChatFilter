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
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType

class AnvilListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onAnvil(event: InventoryClickEvent) {
        if (!AdvancedSensitiveWords.isInitialized) return
        if (!settingsManager.getProperty(PluginSettings.ENABLE_ANVIL_EDIT_CHECK)) return
        if (event.inventory.type == InventoryType.ANVIL) {
            val player = event.whoClicked as Player
            if (CachingPermTool.hasPermission(PermissionsEnum.BYPASS, player)) {
                return
            }
            if (event.rawSlot == 2) {
                val outputItem = event.currentItem
                val isCancelMode = settingsManager.getProperty(PluginSettings.ANVIL_METHOD).equals("cancel", ignoreCase = true)
                if (outputItem != null && outputItem.hasItemMeta()) {
                    val itemMeta = outputItem.itemMeta
                    if (itemMeta != null && itemMeta.hasDisplayName()) {
                        var originalItemName = itemMeta.displayName
                        if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalItemName =
                            originalItemName.replace(
                                Utils.preProcessRegex.toRegex(), ""
                            )
                        val censoredWords = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalItemName)
                        if (censoredWords.isNotEmpty()) {
                            val startTime = System.currentTimeMillis()
                            Utils.messagesFilteredNum.getAndIncrement()
                            val processedItemName = AdvancedSensitiveWords.sensitiveWordBs.replace(originalItemName)
                            if (isCancelMode) {
                                event.isCancelled = true
                            } else {
                                itemMeta.setDisplayName(processedItemName)
                                outputItem.setItemMeta(itemMeta)
                            }

                            if (settingsManager.getProperty(PluginSettings.ANVIL_SEND_MESSAGE)) {
                                MessageUtils.sendMessage(
                                    player,
                                    PluginMessages.MESSAGE_ON_ANVIL_RENAME
                                )
                            }

                            if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                                LoggingUtils.logViolation(
                                    player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Anvil)",
                                    originalItemName + censoredWords
                                )
                            }
                            ViolationCounter.INSTANCE.incrementViolationCount(player)

                            if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                                VelocitySender.sendNotifyMessage(player, ModuleType.ANVIL, originalItemName, censoredWords)
                            }

                            if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                                BungeeSender.sendNotifyMessage(player, ModuleType.ANVIL, originalItemName, censoredWords)
                            }
                            val endTime = System.currentTimeMillis()
                            TimingUtils.addProcessStatistic(endTime, startTime)
                            if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(
                                player,
                                ModuleType.ANVIL,
                                originalItemName,
                                censoredWords
                            )
                            if (settingsManager.getProperty(PluginSettings.ANVIL_PUNISH)) Punishment.punish(
                                player
                            )
                        }
                    }
                }
            }
        }
    }
}
