package io.wdsj.asw.bukkit.listener.packet

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEditBook
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
import io.wdsj.asw.bukkit.util.cache.BookCache
import io.wdsj.asw.bukkit.util.message.MessageUtils
import org.bukkit.entity.Player

class ASWBookPacketListener : PacketListenerAbstract(PacketListenerPriority.LOW) {
    override fun onPacketReceive(event: PacketReceiveEvent) {
        if (!AdvancedSensitiveWords.isInitialized) return
        if (!settingsManager.getProperty(PluginSettings.ENABLE_BOOK_EDIT_CHECK)) return
        val packetType = event.packetType
        val user = event.user
        val userName = user.name
        if (packetType === PacketType.Play.Client.EDIT_BOOK) {
            val player = event.getPlayer() as Player
            if (CachingPermTool.hasPermission(PermissionsEnum.BYPASS, player)) return
            var outMessage = ""
            var outList: List<String?> = ArrayList()
            val skipReturnLine = settingsManager.getProperty(PluginSettings.BOOK_IGNORE_NEWLINE)
            val isCacheEnabled = settingsManager.getProperty(PluginSettings.BOOK_CACHE)
            var shouldSendMessage = false
            val isCancelMode = settingsManager.getProperty(PluginSettings.BOOK_METHOD).equals("cancel", ignoreCase = true)
            val wrapper = WrapperPlayClientEditBook(event)
            val originalPages = wrapper.pages

            // Book content check
            val processedPages: MutableList<String> = ArrayList()
            val startTime = System.currentTimeMillis()
            for (i in 0 until originalPages.size) {
                var originalPage = originalPages[i]
                if (skipReturnLine) {
                    originalPage = originalPage.replace("\n", "").replace("§0", "")
                }
                if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalPage = originalPage.replace(Utils.preProcessRegex.toRegex(), "")
                val isBookCached = BookCache.isBookCached(originalPage)
                val censoredWordList = if (isBookCached && isCacheEnabled) BookCache.getCachedBookSensitiveWordList(originalPage) else AdvancedSensitiveWords.sensitiveWordBs.findAll(originalPage)
                if (censoredWordList.isNotEmpty()) {
                    val processedPage = if (isBookCached && isCacheEnabled) BookCache.getCachedProcessedBookContent(originalPage) else AdvancedSensitiveWords.sensitiveWordBs.replace(originalPage)
                    if (!isBookCached && isCacheEnabled) BookCache.addToBookCache(originalPage, processedPage, censoredWordList)
                    if (isCancelMode) {
                        event.isCancelled = true
                        shouldSendMessage = true
                        outMessage = originalPage
                        outList = censoredWordList
                        break
                    }

                    shouldSendMessage = true
                    outMessage = originalPage
                    outList = censoredWordList
                    processedPages.add(processedPage)
                } else {
                    processedPages.add(originalPages[i])
                }
            }
            if (!isCancelMode && shouldSendMessage) {
                wrapper.pages = processedPages
            }

            // Cross page check
            if (settingsManager.getProperty(PluginSettings.BOOK_CROSS_PAGE) && !shouldSendMessage) {
                var crossPageListString = originalPages.joinToString("").replace("\n", "").replace("§0", "")
                if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) {
                    crossPageListString = crossPageListString.replace(Utils.preProcessRegex.toRegex(), "")
                }
                val censoredWordListCrossPage = AdvancedSensitiveWords.sensitiveWordBs.findAll(crossPageListString)
                if (censoredWordListCrossPage.isNotEmpty()) {
                    shouldSendMessage = true
                    outMessage = crossPageListString
                    outList = censoredWordListCrossPage
                    event.isCancelled = true
                }
            }

            // Book title check
            var originalTitle = wrapper.title
            if (originalTitle != null) {
                if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalTitle = originalTitle.replace(Utils.preProcessRegex.toRegex(), "")
                val censoredWordListTitle = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalTitle)
                if (censoredWordListTitle.isNotEmpty()) {
                    val processedTitle = AdvancedSensitiveWords.sensitiveWordBs.replace(originalTitle)
                    if (isCancelMode) {
                        event.isCancelled = true
                    } else {
                        wrapper.title = processedTitle
                    }
                    shouldSendMessage = true
                    outMessage = originalTitle
                    outList = censoredWordListTitle
                }
            }

            if (shouldSendMessage) {
                Utils.messagesFilteredNum.getAndIncrement()
                ViolationCounter.INSTANCE.incrementViolationCount(player)
                if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                    VelocitySender.sendNotifyMessage(player, ModuleType.BOOK, outMessage, outList)
                }
                if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                    BungeeSender.sendNotifyMessage(player, ModuleType.BOOK, outMessage, outList)
                }
                val endTime = System.currentTimeMillis()
                TimingUtils.addProcessStatistic(endTime, startTime)
                if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
                    Notifier.notice(player, ModuleType.BOOK, outMessage, outList)
                }
                if (settingsManager.getProperty(PluginSettings.BOOK_PUNISH)) {
                    AdvancedSensitiveWords.getScheduler().runTask {
                        Punishment.punish(player)
                    }
                }
                if (settingsManager.getProperty(PluginSettings.BOOK_SEND_MESSAGE)) {
                    MessageUtils.sendMessage(player, PluginMessages.MESSAGE_ON_BOOK)
                }
                if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                    LoggingUtils.logViolation(userName + "(IP: " + Utils.getPlayerIp(player) + ")(Book)", outMessage + outList)
                }
            }
        }
    }
}
