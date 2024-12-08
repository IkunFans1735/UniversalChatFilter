package io.wdsj.asw.velocity.subscriber;

import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import io.wdsj.asw.common.constant.networking.ChannelDataConstant;
import io.wdsj.asw.common.datatype.io.LimitedByteArrayDataOutput;
import io.wdsj.asw.common.template.PluginVersionTemplate;
import org.slf4j.Logger;

import java.util.Locale;

import static io.wdsj.asw.velocity.AdvancedSensitiveWords.CHANNEL;
import static io.wdsj.asw.velocity.AdvancedSensitiveWords.LEGACY_CHANNEL;

@SuppressWarnings("UnstableApiUsage")
public class PluginMessageForwarder {
    private boolean warned = false;
    private final Logger logger;
    private final ProxyServer server;
    public PluginMessageForwarder(Logger logger, ProxyServer server) {
        this.logger = logger;
        this.server = server;
    }
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getIdentifier().equals(CHANNEL) || event.getIdentifier().equals(LEGACY_CHANNEL)) {
            if (!(event.getSource() instanceof ServerConnection)) return;
            var serverInfo = ((ServerConnection) event.getSource()).getServerInfo();
            byte[] message = event.getData();
            var input = ByteStreams.newDataInput(message);
            if (!input.readUTF().equals(PluginVersionTemplate.VERSION) && !warned) {
                logger.warn("Plugin version mismatch! Things may not work properly.");
                warned = true;
            }
            switch (input.readUTF().toLowerCase(Locale.ROOT)) {
                case ChannelDataConstant.NOTICE:
                    server.getAllServers().forEach(server -> {
                        if (!server.getServerInfo().equals(serverInfo) && !server.getPlayersConnected().isEmpty()) {
                            var out = LimitedByteArrayDataOutput.newDataOutput(32767);
                            try {
                                out.write(message);
                                out.writeUTF(serverInfo.getName());
                            } catch (Exception e) {
                                logger.error("Failed to write notice message: {}", e.getMessage());
                            }
                            server.sendPluginMessage(CHANNEL, out.toByteArray());
                            logger.debug("Send notice message to {}", server.getServerInfo().getName());
                        }
                    });
                    break;
                case ChannelDataConstant.COMMAND_PROXY:
                    String command = input.readUTF();
                    server.getCommandManager().executeAsync(server.getConsoleCommandSource(), command);
                    break;
            }
            event.setResult(PluginMessageEvent.ForwardResult.handled());
        }
    }
}
