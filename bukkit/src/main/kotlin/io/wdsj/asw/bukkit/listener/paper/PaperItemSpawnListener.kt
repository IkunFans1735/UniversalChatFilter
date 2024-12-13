package io.wdsj.asw.bukkit.listener.paper

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.*
import io.wdsj.asw.bukkit.annotation.PaperEventHandler
import io.wdsj.asw.bukkit.permission.PermissionsEnum
import io.wdsj.asw.bukkit.permission.cache.CachingPermTool
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.util.LoggingUtils
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.ItemSpawnEvent

@PaperEventHandler
class PaperItemSpawnListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onItemSpawn(event: ItemSpawnEvent) {
        if (!isInitialized || !settingsManager.getProperty(PluginSettings.ITEM_MONITOR_SPAWN) || !settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_ITEM_CHECK)) return
                val itemEntity = event.entity
        val throwerPlayer = itemEntity.thrower?.let { Bukkit.getPlayer(it) }
        if (throwerPlayer != null && CachingPermTool.hasPermission(PermissionsEnum.BYPASS, throwerPlayer)) return
        itemEntity.customName()?.let {
            var originalName = it
            if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) {
                val replacementConfig = TextReplacementConfig.builder()
                    .match(Utils.preProcessRegex.toPattern())
                    .replacement("")
                    .build()
                originalName = originalName.replaceText(replacementConfig)
            }
            val startTime = System.currentTimeMillis()
            val plainTextOriginalName = PlainTextComponentSerializer.plainText().serialize(originalName)
            val censoredWordList = sensitiveWordBs.findAll(plainTextOriginalName)
            if (censoredWordList.isNotEmpty()) {
                Utils.messagesFilteredNum.getAndIncrement()
                if (settingsManager.getProperty(PluginSettings.ITEM_METHOD).equals("cancel", ignoreCase = true)) {
                    itemEntity.customName(null)
                } else {
                    val processedName = sensitiveWordBs.replace(plainTextOriginalName)
                    val cfg = TextReplacementConfig.builder()
                        .matchLiteral(plainTextOriginalName)
                        .replacement(processedName)
                        .build()
                    itemEntity.customName(originalName.replaceText(cfg))
                }
                val locationLog = itemEntity.location.toLogString()
                if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                    LoggingUtils.logViolation("ItemSpawn(IP: None)(ItemSpawn)($locationLog)", plainTextOriginalName + censoredWordList)
                }
                val endTime = System.currentTimeMillis()
                TimingUtils.addProcessStatistic(endTime, startTime)
            }
        }
    }
    private fun Location.toLogString(): String {
        return "World: ${this.world?.name ?: "Unknown"}, X: ${this.x}, Y: ${this.y}, Z: ${this.z}"
    }
}