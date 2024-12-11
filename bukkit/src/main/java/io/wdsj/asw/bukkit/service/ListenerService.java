package io.wdsj.asw.bukkit.service;

import com.github.retrooper.packetevents.PacketEvents;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.annotation.PaperEventHandler;
import io.wdsj.asw.bukkit.listener.*;
import io.wdsj.asw.bukkit.listener.packet.ASWBookPacketListener;
import io.wdsj.asw.bukkit.listener.packet.ASWChatPacketListener;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.*;
import static io.wdsj.asw.bukkit.util.Utils.isClassLoaded;

public class ListenerService {
    private final AdvancedSensitiveWords plugin;
    private static final boolean isModernPaper = Utils.isClassLoaded(
            "io.papermc.paper.event.player.AsyncChatEvent"
    );
    public ListenerService(AdvancedSensitiveWords plugin) {
        this.plugin = plugin;
    }

    public void registerListeners() {
        if (!AdvancedSensitiveWords.isEventMode()) {
            if (USE_PE) {
                try {
                    if (settingsManager.getProperty(PluginSettings.ENABLE_CHAT_CHECK)) {
                        PacketEvents.getAPI().getEventManager().registerListener(ASWChatPacketListener.class.getConstructor().newInstance());
                    }
                    if (settingsManager.getProperty(PluginSettings.ENABLE_BOOK_EDIT_CHECK)) {
                        PacketEvents.getAPI().getEventManager().registerListener(ASWBookPacketListener.class.getConstructor().newInstance());
                    }
                } catch (Exception e) {
                    LOGGER.severe("Failed to register packetevents listener." +
                            " This should not happen, please report to the author");
                    LOGGER.severe(e.getMessage());
                }
                PacketEvents.getAPI().init();
            } else {
                LOGGER.warning("Cannot use packetevents, using event mode instead.");
                registerChatBookEventListeners();
                setEventMode(true);
            }
        } else {
            registerChatBookEventListeners();
        }
        registerEventListener(new ShadowListener());
        registerEventListener(new AltsListener());
        if (!registerEventListener(new PaperFakeMessageExecutor())) {
            registerEventListener(new FakeMessageExecutor());
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_SIGN_EDIT_CHECK)) {
            registerEventListener(new SignListener());
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_ANVIL_EDIT_CHECK)) {
            registerEventListener(new AnvilListener());
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_NAME_CHECK)) {
            registerEventListener(new PlayerLoginListener());
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_ITEM_CHECK)) {
            registerEventListener(new PlayerItemListener());
            if (settingsManager.getProperty(PluginSettings.ITEM_MONITOR_SPAWN)) {
                registerEventListener(new ItemSpawnListener());
            }
        }
        if (settingsManager.getProperty(PluginSettings.CHAT_BROADCAST_CHECK)) {
            if (isClassLoaded("org.bukkit.event.server.BroadcastMessageEvent")) {
                registerEventListener(new BroadCastListener());
            } else {
                LOGGER.info("BroadcastMessage is not available, please disable chat broadcast check in config.yml");
            }
        }
        if (settingsManager.getProperty(PluginSettings.CLEAN_PLAYER_DATA_CACHE)) {
            registerEventListener(new QuitDataCleaner());
        }
        if (settingsManager.getProperty(PluginSettings.CHECK_FOR_UPDATE)) {
            registerEventListener(new JoinUpdateNotifier());
        }
    }

    public void unregisterListeners() {
        if (!isEventMode()) {
            if (USE_PE) {
                PacketEvents.getAPI().terminate();
            }
        }
        HandlerList.unregisterAll(plugin);
    }
    
    
    private boolean registerEventListener(Listener listener) {
        if (!isTargetListenerHasAllClasses(listener)) {
            return false;
        }
        if (isPaperListener(listener)) {
            if (isModernPaper) {
                Bukkit.getPluginManager().registerEvents(listener, plugin);
                LOGGER.info("Using Paper events for " + listener.getClass().getSimpleName() + ".");
                return true;
            }
            return false;
        } else {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
            return true;
        }
    }

    private boolean isTargetListenerHasAllClasses(Listener listener) {
        try {
            Method[] methods = listener.getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getAnnotation(EventHandler.class) == null || method.getParameterCount() != 1) continue;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    private boolean isPaperListener(Listener listener) {
        return listener.getClass().getAnnotation(PaperEventHandler.class) != null;
    }

    private void registerChatBookEventListeners() {
        if (settingsManager.getProperty(PluginSettings.ENABLE_CHAT_CHECK)) {
            registerEventListener(new ChatListener());
            registerEventListener(new CommandListener());
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_BOOK_EDIT_CHECK)) {
            registerEventListener(new BookListener());
        }
    }

}
