package io.wdsj.asw.bukkit.proxy.velocity;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter;
import io.wdsj.asw.bukkit.type.ModuleType;
import io.wdsj.asw.common.constant.networking.ChannelDataConstant;
import io.wdsj.asw.common.datatype.io.LimitedByteArrayDataOutput;
import org.bukkit.entity.Player;

import java.util.List;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;

@SuppressWarnings("UnstableApiUsage")
public class VelocitySender {
    public static void sendNotifyMessage(Player violatedPlayer, ModuleType moduleType, String originalMessage, List<String> censoredList) {
        LimitedByteArrayDataOutput out = LimitedByteArrayDataOutput.newDataOutput(32767);
        try {
            out.writeUTF(AdvancedSensitiveWords.PLUGIN_VERSION);
            out.writeUTF(ChannelDataConstant.NOTICE);
            out.writeUTF(violatedPlayer.getName());
            out.writeUTF(moduleType.toString());
            out.writeUTF(String.valueOf(ViolationCounter.INSTANCE.getViolationCount(violatedPlayer)));
            out.writeUTF(originalMessage);
            out.writeUTF(censoredList.toString());
        } catch (Exception e) {
            LOGGER.warning("Failed to send message to Velocity: " + e.getMessage());
            return;
        }
        byte[] data = out.toByteArray();
        violatedPlayer.sendPluginMessage(AdvancedSensitiveWords.getInstance(), VelocityChannel.CHANNEL, data);
    }

    public static void executeVelocityCommand(Player violatedPlayer, String command) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(AdvancedSensitiveWords.PLUGIN_VERSION);
        out.writeUTF(ChannelDataConstant.COMMAND_PROXY);
        out.writeUTF(command);
        byte[] data = out.toByteArray();
        violatedPlayer.sendPluginMessage(AdvancedSensitiveWords.getInstance(), VelocityChannel.CHANNEL, data);
    }
}
