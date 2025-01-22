package io.wdsj.asw.bukkit.task.voicechat;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import io.wdsj.asw.bukkit.integration.voicechat.VoiceChatExtension;
import io.wdsj.asw.bukkit.integration.voicechat.WhisperVoiceTranscribeTool;
import io.wdsj.asw.bukkit.manage.notice.Notifier;
import io.wdsj.asw.bukkit.manage.punish.Punishment;
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter;
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender;
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.type.ModuleType;
import io.wdsj.asw.bukkit.util.LoggingUtils;
import io.wdsj.asw.bukkit.util.SchedulingUtils;
import io.wdsj.asw.bukkit.util.TimingUtils;
import io.wdsj.asw.bukkit.util.Utils;
import io.wdsj.asw.bukkit.util.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.*;

public class VoiceChatTranscribeTask extends UniversalRunnable {
    private final VoiceChatExtension extension;
    private final WhisperVoiceTranscribeTool transcribeTool;
    public VoiceChatTranscribeTask(VoiceChatExtension extension, WhisperVoiceTranscribeTool transcribeTool) {
        this.extension = extension;
        this.transcribeTool = transcribeTool;
    }
    @Override
    public void run() {
        if (!settingsManager.getProperty(PluginSettings.VOICE_REALTIME_TRANSCRIBING)) {
            if (!extension.connectedPlayers.isEmpty()) extension.connectedPlayers.clear();
            return;
        }
        if (extension.connectedPlayers.isEmpty()) return;
        Set<Map.Entry<UUID, float[]>> entries = extension.connectedPlayers.entrySet();
        for (Map.Entry<UUID, float[]> entry : entries) {
            if (!isInitialized) continue;
            UUID uuid = entry.getKey();
            float[] data = entry.getValue();
            Player player = SchedulingUtils.callSyncMethod(() -> Bukkit.getPlayer(uuid));
            extension.connectedPlayers.remove(uuid); // Early remove before transcribing
            if (player == null) continue;
            transcribeTool.transcribe(data)
                    .thenAccept(text -> {
                        if (text.isEmpty()) return;
                        List<String> censoredWordList = sensitiveWordBs.findAll(text);
                        long startTime = System.currentTimeMillis();
                        if (!censoredWordList.isEmpty()) {
                            Utils.messagesFilteredNum.getAndIncrement();
                            if (settingsManager.getProperty(PluginSettings.VOICE_SEND_MESSAGE)) {
                                MessageUtils.sendMessage(player, PluginMessages.MESSAGE_ON_VOICE);
                            }
                            if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                                LoggingUtils.logViolation(player.getName() + "(IP: " + Utils.getPlayerIp(player) + ")(Voice)", text + censoredWordList);
                            }
                            ViolationCounter.INSTANCE.incrementViolationCount(player);
                            if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                                VelocitySender.sendNotifyMessage(player, ModuleType.VOICE, text, censoredWordList);
                            }
                            if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                                BungeeSender.sendNotifyMessage(player, ModuleType.VOICE, text, censoredWordList);
                            }
                            long endTime = System.currentTimeMillis();
                            TimingUtils.addProcessStatistic(endTime, startTime);
                            if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, ModuleType.VOICE, text, censoredWordList);
                            if (settingsManager.getProperty(PluginSettings.VOICE_PUNISH)) {
                                SchedulingUtils.runSyncIfNotOnMainThread(() -> Punishment.punish(player));
                            }
                        }
                    });
        }
    }
}
