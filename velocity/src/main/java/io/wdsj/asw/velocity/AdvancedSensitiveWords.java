package io.wdsj.asw.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import io.wdsj.asw.common.template.PluginVersionTemplate;
import io.wdsj.asw.common.update.Updater;
import io.wdsj.asw.velocity.config.Config;
import io.wdsj.asw.velocity.subscriber.PluginMessageForwarder;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(
        id = "advancedsensitivewords",
        name = "AdvancedSensitiveWords",
        version = PluginVersionTemplate.VERSION + "-" + PluginVersionTemplate.VERSION_CHANNEL,
        authors = {"HaHaWTH"}
)
public class AdvancedSensitiveWords {

    private final Logger logger;
    private final ProxyServer server;
    private final Metrics.Factory metricsFactory;
    private final File dataFolder;
    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("asw", "main");
    public static final ChannelIdentifier LEGACY_CHANNEL
            = new LegacyChannelIdentifier("asw:main");
    private static Config config;
    @Inject
    public AdvancedSensitiveWords(Logger logger, ProxyServer server, Metrics.Factory metricsFactory, @DataDirectory Path path) {
        this.logger = logger;
        this.server = server;
        this.metricsFactory = metricsFactory;
        this.dataFolder = path.toFile();
    }
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        reloadConfiguration();
        server.getChannelRegistrar().register(CHANNEL, LEGACY_CHANNEL);
        Metrics metrics = metricsFactory.make(this, 21637);
        server.getEventManager().register(this, new PluginMessageForwarder(logger, server));
        if (config.check_for_update) {
            server.getScheduler().buildTask(this, () -> {
                logger.info("Checking for update...");
                if (Updater.isUpdateAvailable()) {
                    if (Updater.isDevChannel()) {
                        logger.warn("There is a new development version available: {}, you're on: {}", Updater.getLatestVersion(), Updater.getCurrentVersion());
                    } else {
                        logger.warn("There is a new version available: {}, you're on: {}", Updater.getLatestVersion(), Updater.getCurrentVersion());
                    }
                } else {
                    if (!Updater.isErred()) {
                        logger.info("You are running the latest version.");
                    } else {
                        logger.info("Unable to fetch version info.");
                    }
                }
            }).schedule();
        }
    }

    private void reloadConfiguration() {
        try {
            createDirectory(dataFolder);
            config = new Config(this, dataFolder);
            config.saveConfig();
        } catch (Throwable t) {
            logger.error("Failed while loading config!", t);
        }
    }

    public void createDirectory(File dir) throws IOException {
        try {
            Files.createDirectories(dir.toPath());
        } catch (FileAlreadyExistsException e) { // Thrown if dir exists but is not a directory
            if (dir.delete()) createDirectory(dir);
        }
    }

    public Logger getLogger() {
        return this.logger;
    }

    public static Config config() {
        return config;
    }
}
