package io.wdsj.asw.bukkit.listener.paper

import cc.baka9.catseedlogin.bukkit.CatSeedLoginAPI
import fr.xephi.authme.api.v3.AuthMeApi
import io.papermc.paper.event.player.AsyncChatEvent
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.*
import io.wdsj.asw.bukkit.ai.OllamaProcessor
import io.wdsj.asw.bukkit.ai.OpenAIProcessor
import io.wdsj.asw.bukkit.annotation.PaperEventHandler
import io.wdsj.asw.bukkit.listener.abstraction.AbstractFakeMessageExecutor
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
import io.wdsj.asw.bukkit.util.SchedulingUtils
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.context.ChatContext
import io.wdsj.asw.bukkit.util.message.MessageUtils
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.util.*

@PaperEventHandler
class PaperChatListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onChat(event: AsyncChatEvent) {
        if (!settingsManager.getProperty(PluginSettings.ENABLE_CHAT_CHECK)) return
        val player = event.player
        if (shouldNotProcess(player)) return
        val isCancelMode = settingsManager.getProperty(PluginSettings.CHAT_METHOD).equals("cancel", ignoreCase = true)
        val originalMessage = if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) {
            val replacementConfig = TextReplacementConfig.builder()
                .match(Utils.preProcessRegex.toPattern())
                .replacement("")
                .build()
            event.message().replaceText(replacementConfig)
        } else {
            event.message()
        }
        val originalPlainText = PlainTextComponentSerializer.plainText().serialize(originalMessage)
        val censoredWordList = sensitiveWordBs.findAll(originalPlainText)
        val startTime = System.currentTimeMillis()
        if (censoredWordList.isNotEmpty()) {
            Utils.messagesFilteredNum.getAndIncrement()
            val processedMessage = sensitiveWordBs.replace(originalPlainText)
            if (isCancelMode) {
                if (settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL)) {
                    AbstractFakeMessageExecutor.selfIncrement(player)
                } else {
                    event.isCancelled = true
                }
            } else {
                val cfg = TextReplacementConfig.builder()
                    .matchLiteral(originalPlainText)
                    .replacement(processedMessage)
                    .build()
                val processedWithLiteral = originalMessage.replaceText(cfg)
                event.message(processedWithLiteral)
            }
            if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                MessageUtils.sendMessage(player, messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", player.name).replace("%integrated_message%", originalPlainText))
            }
            if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                LoggingUtils.logViolation(player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Chat)", originalPlainText + censoredWordList)
            }
            ViolationCounter.incrementViolationCount(player)
            if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                VelocitySender.sendNotifyMessage(player, ModuleType.CHAT, originalPlainText, censoredWordList)
            }
            if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                BungeeSender.sendNotifyMessage(player, ModuleType.CHAT, originalPlainText, censoredWordList)
            }
            val endTime = System.currentTimeMillis()
            TimingUtils.addProcessStatistic(endTime, startTime)
            if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, ModuleType.CHAT, originalPlainText, censoredWordList)
            if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) {
                SchedulingUtils.runSyncIfEventAsync(event) {
                    Punishment.punish(player)
                }
            }
            return
        } else {
            if (settingsManager.getProperty(PluginSettings.ENABLE_OLLAMA_AI_MODEL_CHECK)
                && OllamaProcessor.INSTANCE.isInitialized) {
                OllamaProcessor.process(originalPlainText)
                    .thenAccept {
                        try {
                            val rating = it?.toInt() ?: 0
                            if (rating > settingsManager.getProperty(PluginSettings.OLLAMA_AI_SENSITIVE_THRESHOLD)) {
                                val unsupportedList = Collections.singletonList("Unsupported")
                                Utils.messagesFilteredNum.getAndIncrement()
                                if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                                    MessageUtils.sendMessage(player, messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", player.name).replace("%integrated_message%", originalPlainText))
                                }
                                if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                                    LoggingUtils.logViolation(player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Chat AI)(LLM output: $it)", originalPlainText + unsupportedList)
                                }
                                ViolationCounter.incrementViolationCount(player)
                                if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                                    VelocitySender.sendNotifyMessage(player, ModuleType.CHAT_AI, originalPlainText, unsupportedList)
                                }
                                if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                                    BungeeSender.sendNotifyMessage(player, ModuleType.CHAT_AI, originalPlainText, unsupportedList)
                                }
                                if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
                                    Notifier.notice(player, ModuleType.CHAT_AI, originalPlainText, unsupportedList)
                                }
                                if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH) && settingsManager.getProperty(
                                        PluginSettings.OLLAMA_AI_PUNISH)) {
                                    SchedulingUtils.runSyncIfEventAsync(event) {
                                        Punishment.punish(player)
                                    }
                                }
                            }
                        } catch (e: NumberFormatException) {
                            LOGGER.warning("Failed to parse Ollama output to a number: $it")
                        }
                    }
            }
            if (settingsManager.getProperty(PluginSettings.ENABLE_OPENAI_AI_MODEL_CHECK)
                && OpenAIProcessor.INSTANCE.isInitialized) {
                OpenAIProcessor.process(originalPlainText)
                    .thenAccept {
                        val flag = it ?: return@thenAccept
                        if (flag) {
                            val unsupportedList = Collections.singletonList("Unsupported")
                            Utils.messagesFilteredNum.getAndIncrement()
                            if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                                MessageUtils.sendMessage(player, messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", player.name).replace("%integrated_message%", originalPlainText))
                            }
                            if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                                LoggingUtils.logViolation(player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Chat AI)(OPENAI)", originalPlainText + unsupportedList)
                            }
                            ViolationCounter.incrementViolationCount(player)
                            if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                                VelocitySender.sendNotifyMessage(player, ModuleType.CHAT_AI, originalPlainText, unsupportedList)
                            }
                            if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                                BungeeSender.sendNotifyMessage(player, ModuleType.CHAT_AI, originalPlainText, unsupportedList)
                            }
                            if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
                                Notifier.notice(player, ModuleType.CHAT_AI, originalPlainText, unsupportedList)
                            }
                            if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH) && settingsManager.getProperty(
                                    PluginSettings.OPENAI_AI_PUNISH)) {
                                SchedulingUtils.runSyncIfEventAsync(event) {
                                    Punishment.punish(player)
                                }
                            }
                        }
                    }
            }
        }

        if (settingsManager.getProperty(PluginSettings.CHAT_CONTEXT_CHECK)) {
            ChatContext.addMessage(player, originalPlainText)
            val queue = ChatContext.getHistory(player)
            val originalContext = queue.joinToString("")
            val censoredContextList = sensitiveWordBs.findAll(originalContext)
            if (censoredContextList.isNotEmpty()) {
                ChatContext.pollPlayerContext(player)
                Utils.messagesFilteredNum.getAndIncrement()
                if (settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL)) {
                    AbstractFakeMessageExecutor.selfIncrement(player)
                } else {
                    event.isCancelled = true
                }
                if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                    MessageUtils.sendMessage(player, messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", player.name).replace("%integrated_message%", originalPlainText))
                }
                if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                    LoggingUtils.logViolation(player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Chat)(Context)", originalContext + censoredContextList)
                }
                ViolationCounter.incrementViolationCount(player)
                if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                    VelocitySender.sendNotifyMessage(player, ModuleType.CHAT, originalContext, censoredContextList)
                }
                if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                    BungeeSender.sendNotifyMessage(player, ModuleType.CHAT, originalContext, censoredContextList)
                }
                val endTime = System.currentTimeMillis()
                TimingUtils.addProcessStatistic(endTime, startTime)
                if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, ModuleType.CHAT, originalContext, censoredContextList)
                if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) {
                    SchedulingUtils.runSyncIfEventAsync(event) {
                        Punishment.punish(player)
                    }
                }
            }
        }
    }

    private fun shouldNotProcess(player: Player): Boolean {
        if (isInitialized && !CachingPermTool.hasPermission(
                PermissionsEnum.BYPASS, player)) {
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
