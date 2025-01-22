package io.wdsj.asw.bukkit.service;

import com.github.retrooper.packetevents.PacketEvents;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.annotation.PaperEventHandler;
import io.wdsj.asw.bukkit.listener.*;
import io.wdsj.asw.bukkit.listener.packet.ASWBookPacketListener;
import io.wdsj.asw.bukkit.listener.packet.ASWChatPacketListener;
import io.wdsj.asw.bukkit.listener.paper.PaperChatListener;
import io.wdsj.asw.bukkit.listener.paper.PaperFakeMessageExecutor;
import io.wdsj.asw.bukkit.listener.paper.PaperItemSpawnListener;
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
        registerEventListener(ShadowListener.class);
        registerEventListener(AltsListener.class);
        if (!registerEventListener(PaperFakeMessageExecutor.class)) {
            registerEventListener(FakeMessageExecutor.class);
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_SIGN_EDIT_CHECK)) {
            registerEventListener(SignListener.class);
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_ANVIL_EDIT_CHECK)) {
            registerEventListener(AnvilListener.class);
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_NAME_CHECK)) {
            registerEventListener(PlayerLoginListener.class);
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_ITEM_CHECK)) {
            registerEventListener(PlayerItemListener.class);
            if (settingsManager.getProperty(PluginSettings.ITEM_MONITOR_SPAWN)) {
                if (!registerEventListener(PaperItemSpawnListener.class)) {
                    registerEventListener(ItemSpawnListener.class);
                }
            }
        }
        if (settingsManager.getProperty(PluginSettings.CHAT_BROADCAST_CHECK)) {
            if (isClassLoaded("org.bukkit.event.server.BroadcastMessageEvent")) {
                registerEventListener(BroadCastListener.class);
            } else {
                LOGGER.info("BroadcastMessage is not available, please disable chat broadcast check in config.yml");
            }
        }
        if (settingsManager.getProperty(PluginSettings.CLEAN_PLAYER_DATA_CACHE)) {
            registerEventListener(QuitDataCleaner.class);
        }
        if (settingsManager.getProperty(PluginSettings.CHECK_FOR_UPDATE)) {
            registerEventListener(JoinUpdateNotifier.class);
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
    
    
    private boolean registerEventListener(Class<? extends Listener> listenerClass) {
        if (!isTargetListenerHasAllClasses(listenerClass)) {
            return false;
        }
        if (isPaperListener(listenerClass)) {
            if (isModernPaper) {
                Bukkit.getPluginManager().registerEvents(newListenerWithNoArgConstructor(listenerClass), plugin);
                LOGGER.info("Using Paper event listener " + listenerClass.getSimpleName() + ".");
                return true;
            }
            return false;
        } else {
            Bukkit.getPluginManager().registerEvents(newListenerWithNoArgConstructor(listenerClass), plugin);
            return true;
        }
    }

    private Listener newListenerWithNoArgConstructor(Class<? extends Listener> listenerClass) {
        try {
            return listenerClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to register listener " + listenerClass.getSimpleName());
        }
    }

    private boolean isTargetListenerHasAllClasses(Class<? extends Listener> listener) {
        try {
            Method[] methods = listener.getDeclaredMethods();
            for (Method method : methods) {
                //noinspection StatementWithEmptyBody
                if (method.getAnnotation(EventHandler.class) == null || method.getParameterCount() != 1) {
                }
            }
        } catch (Throwable e) {
            return false;
        }
        return true;
    }
    private boolean isPaperListener(Class<? extends Listener> listener) {
        return listener.getAnnotation(PaperEventHandler.class) != null;
    }

    private void registerChatBookEventListeners() {
        if (settingsManager.getProperty(PluginSettings.ENABLE_CHAT_CHECK)) {
            if (Bukkit.getPluginManager().isPluginEnabled("TrChat") || !registerEventListener(PaperChatListener.class)) {
                registerEventListener(ChatListener.class);
            }
            registerEventListener(CommandListener.class);
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_BOOK_EDIT_CHECK)) {
            registerEventListener(BookListener.class);
        }
    }

}
