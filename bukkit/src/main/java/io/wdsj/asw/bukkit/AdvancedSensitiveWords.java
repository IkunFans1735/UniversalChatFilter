package io.wdsj.asw.bukkit;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.github.houbb.sensitive.word.api.IWordAllow;
import com.github.houbb.sensitive.word.api.IWordDeny;
import com.github.houbb.sensitive.word.api.IWordResultCondition;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.allow.WordAllows;
import com.github.houbb.sensitive.word.support.deny.WordDenys;
import com.github.houbb.sensitive.word.support.resultcondition.WordResultConditions;
import com.github.houbb.sensitive.word.support.tag.WordTags;
import io.wdsj.asw.bukkit.ai.OllamaProcessor;
import io.wdsj.asw.bukkit.ai.OpenAIProcessor;
import io.wdsj.asw.bukkit.command.ConstructCommandExecutor;
import io.wdsj.asw.bukkit.command.ConstructTabCompleter;
import io.wdsj.asw.bukkit.core.condition.WordResultConditionNumMatch;
import io.wdsj.asw.bukkit.integration.placeholder.ASWExpansion;
import io.wdsj.asw.bukkit.manage.punish.PlayerAltController;
import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController;
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter;
import io.wdsj.asw.bukkit.method.*;
import io.wdsj.asw.bukkit.permission.cache.CachingPermTool;
import io.wdsj.asw.bukkit.proxy.bungee.BungeeCordChannel;
import io.wdsj.asw.bukkit.proxy.bungee.BungeeReceiver;
import io.wdsj.asw.bukkit.proxy.velocity.VelocityChannel;
import io.wdsj.asw.bukkit.proxy.velocity.VelocityReceiver;
import io.wdsj.asw.bukkit.service.BukkitLibraryService;
import io.wdsj.asw.bukkit.service.ListenerService;
import io.wdsj.asw.bukkit.service.hook.VoiceChatHookService;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.task.punish.ViolationResetTask;
import io.wdsj.asw.bukkit.util.SchedulingUtils;
import io.wdsj.asw.bukkit.util.TimingUtils;
import io.wdsj.asw.bukkit.util.cache.BookCache;
import io.wdsj.asw.bukkit.util.context.ChatContext;
import io.wdsj.asw.bukkit.util.context.SignContext;
import io.wdsj.asw.common.template.PluginVersionTemplate;
import io.wdsj.asw.common.update.Updater;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static io.wdsj.asw.bukkit.util.LoggingUtils.purgeLog;
import static io.wdsj.asw.bukkit.util.TimingUtils.resetStatistics;
import static io.wdsj.asw.bukkit.util.Utils.*;


public final class AdvancedSensitiveWords extends JavaPlugin {
    public static boolean isInitialized = false;
    public static SensitiveWordBs sensitiveWordBs;
    private final File CONFIG_FILE = new File(getDataFolder(), "config.yml");
    public static boolean isAuthMeAvailable;
    public static boolean isCslAvailable;
    public static SettingsManager settingsManager;
    public static SettingsManager messagesManager;
    public static final String PLUGIN_VERSION = PluginVersionTemplate.VERSION;
    private static AdvancedSensitiveWords instance;
    public static boolean USE_PE = false;
    private static TaskScheduler scheduler;
    private static boolean isEventMode = false;
    public static Logger LOGGER;
    private static BukkitLibraryService libraryService;
    private VoiceChatHookService voiceChatHookService;
    private ListenerService listenerService;
    private CachingPermTool permCache;
    public static TaskScheduler getScheduler() {
        return scheduler;
    }

    public static AdvancedSensitiveWords getInstance() {
        return instance;
    }
    public static boolean isEventMode() {
        return isEventMode;
    }
    public static void setEventMode(boolean mode) {
        isEventMode = mode;
    }
    private MyScheduledTask violationResetTask;
    private boolean isFirstLoad;
    @Override
    public void onLoad() {
        LOGGER = getLogger();
        instance = this;
        isFirstLoad = !getDataFolder().exists();
        settingsManager = SettingsManagerBuilder
                .withYamlFile(CONFIG_FILE)
                .configurationData(PluginSettings.class)
                .useDefaultMigrationService()
                .create();
        File msgFile = new File(getDataFolder(), "messages_" + settingsManager.getProperty(PluginSettings.PLUGIN_LANGUAGE) +
                ".yml");
        if (!msgFile.exists()) {
            saveResource("messages_" + settingsManager.getProperty(PluginSettings.PLUGIN_LANGUAGE) + ".yml", false);
        }
        messagesManager = SettingsManagerBuilder
                .withYamlFile(msgFile)
                .configurationData(PluginMessages.class)
                .useDefaultMigrationService()
                .create();
        libraryService = new BukkitLibraryService(this);
        isEventMode = settingsManager.getProperty(PluginSettings.DETECTION_MODE).equalsIgnoreCase("event");
        if (canUsePE() &&
                !isEventMode) {
            USE_PE = true;
        }
    }

    @Override
    public void onEnable() {
        if (isFirstLoad) {
            LOGGER.info("Downloading required libraries, this may take minutes to complete.");
        }
        LOGGER.info("Loading libraries...");
        long startTime = System.currentTimeMillis();
        libraryService.loadRequired();
        LOGGER.info("Initializing DFA system...");
        resetStatistics();
        scheduler = UniversalScheduler.getScheduler(this);
        permCache = CachingPermTool.enable(this);
        BookCache.initialize();
        WordReplace.clearCache();
        doInitTasks();
        if (settingsManager.getProperty(PluginSettings.PURGE_LOG_FILE)) purgeLog();
        listenerService = new ListenerService(this);
        listenerService.registerListeners();
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setExecutor(new ConstructCommandExecutor());
        Objects.requireNonNull(getCommand("asw")).setExecutor(new ConstructCommandExecutor());
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setTabCompleter(new ConstructTabCompleter());
        Objects.requireNonNull(getCommand("asw")).setTabCompleter(new ConstructTabCompleter());
        int pluginId = 20661;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SimplePie("default_list", () -> String.valueOf(settingsManager.getProperty(PluginSettings.ENABLE_DEFAULT_WORDS))));
        metrics.addCustomChart(new SimplePie("java_vendor", TimingUtils::getJvmVendor));
        metrics.addCustomChart(new SingleLineChart("total_filtered_messages", () -> (int) messagesFilteredNum.get()));
        if (settingsManager.getProperty(PluginSettings.ENABLE_OLLAMA_AI_MODEL_CHECK)) {
            libraryService.loadOllamaOptional();
            OllamaProcessor.INSTANCE.initService(settingsManager.getProperty(PluginSettings.OLLAMA_AI_API_ADDRESS), settingsManager.getProperty(PluginSettings.OLLAMA_AI_MODEL_NAME), settingsManager.getProperty(PluginSettings.AI_MODEL_TIMEOUT), settingsManager.getProperty(PluginSettings.OLLAMA_AI_DEBUG_LOG));
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_OPENAI_AI_MODEL_CHECK)) {
            libraryService.loadOpenAiOptional();
            OpenAIProcessor.INSTANCE.initService(settingsManager.getProperty(PluginSettings.OPENAI_API_KEY), settingsManager.getProperty(PluginSettings.OPENAI_DEBUG_LOG));
        }
        getServer().getMessenger().registerOutgoingPluginChannel(this, VelocityChannel.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, VelocityChannel.CHANNEL, new VelocityReceiver());
        getServer().getMessenger().registerOutgoingPluginChannel(this, BungeeCordChannel.BUNGEE_CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, BungeeCordChannel.BUNGEE_CHANNEL, new BungeeReceiver());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") &&
            settingsManager.getProperty(PluginSettings.ENABLE_PLACEHOLDER)) {
            new ASWExpansion().register();
            LOGGER.info("Placeholders registered.");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("voicechat") &&
            settingsManager.getProperty(PluginSettings.HOOK_SIMPLE_VOICE_CHAT)) {
            if (settingsManager.getProperty(PluginSettings.VOICE_REALTIME_TRANSCRIBING)) {
                libraryService.loadWhisperJniOptional();
            }
            voiceChatHookService = new VoiceChatHookService(this);
            voiceChatHookService.register();
        }
        violationResetTask = new ViolationResetTask().runTaskTimerAsynchronously(this, settingsManager.getProperty(PluginSettings.VIOLATION_RESET_TIME) * 20L * 60L, settingsManager.getProperty(PluginSettings.VIOLATION_RESET_TIME) * 20L * 60L);
        long endTime = System.currentTimeMillis();
        LOGGER.info("AdvancedSensitiveWords is enabled!(took " + (endTime - startTime) + "ms)");
        if (Updater.isDevChannel()) {
            LOGGER.info("You are running a development version of AdvancedSensitiveWords! Branch: " + PluginVersionTemplate.COMMIT_BRANCH);
        }
        if (settingsManager.getProperty(PluginSettings.CHECK_FOR_UPDATE)) {
            getScheduler().runTaskAsynchronously(() -> {
                LOGGER.info("Checking for update...");
                if (Updater.isUpdateAvailable()) {
                    if (Updater.isDevChannel()) {
                        LOGGER.warning("There is a new development version available: " + Updater.getLatestVersion() +
                                ", you're on: " + Updater.getCurrentVersion());
                    } else {
                        LOGGER.warning("There is a new version available: " + Updater.getLatestVersion() +
                                ", you're on: " + Updater.getCurrentVersion());
                    }
                } else {
                    if (!Updater.isErred()) {
                        LOGGER.info("You are running the latest version.");
                    } else {
                        LOGGER.info("Unable to fetch version info.");
                    }
                }
            });
        }
    }


    public void doInitTasks() {
        isAuthMeAvailable = Bukkit.getPluginManager().getPlugin("AuthMe") != null;
        isCslAvailable = Bukkit.getPluginManager().getPlugin("CatSeedLogin") != null;
        IWordAllow wA = WordAllows.chains(WordAllows.defaults(), new WordAllow(), new ExternalWordAllow());
        AtomicReference<IWordDeny> wD = new AtomicReference<>();
        isInitialized = false;
        sensitiveWordBs = null;
        IWordResultCondition condition;
        switch (settingsManager.getProperty(PluginSettings.FULL_MATCH_MODE)) {
            case 0:
                condition = WordResultConditions.alwaysTrue();
                break;
            case 1:
                condition = WordResultConditions.englishWordMatch();
                break;
            case 2:
                condition = WordResultConditions.englishWordNumMatch();
                break;
            case 3:
                condition = new WordResultConditionNumMatch();
                break;
            default:
                condition = WordResultConditions.alwaysTrue();
                LOGGER.warning("Invalid full match mode, will turn off full match.");
        }
        getScheduler().runTaskAsynchronously(() -> {
            if (settingsManager.getProperty(PluginSettings.ENABLE_DEFAULT_WORDS) && settingsManager.getProperty(PluginSettings.ENABLE_ONLINE_WORDS)) {
                wD.set(WordDenys.chains(WordDenys.defaults(), new WordDeny(), new OnlineWordDeny(), new ExternalWordDeny()));
            } else if (settingsManager.getProperty(PluginSettings.ENABLE_DEFAULT_WORDS)) {
                wD.set(WordDenys.chains(new WordDeny(), WordDenys.defaults(), new ExternalWordDeny()));
            } else if (settingsManager.getProperty(PluginSettings.ENABLE_ONLINE_WORDS)) {
                wD.set(WordDenys.chains(new OnlineWordDeny(), new WordDeny(), new ExternalWordDeny()));
            } else {
                wD.set(WordDenys.chains(new WordDeny(), new ExternalWordDeny()));
            }
            sensitiveWordBs = SensitiveWordBs.newInstance().ignoreCase(settingsManager.getProperty(PluginSettings.IGNORE_CASE)).ignoreWidth(settingsManager.getProperty(PluginSettings.IGNORE_WIDTH)).ignoreNumStyle(settingsManager.getProperty(PluginSettings.IGNORE_NUM_STYLE)).ignoreChineseStyle(settingsManager.getProperty(PluginSettings.IGNORE_CHINESE_STYLE)).ignoreEnglishStyle(settingsManager.getProperty(PluginSettings.IGNORE_ENGLISH_STYLE)).ignoreRepeat(settingsManager.getProperty(PluginSettings.IGNORE_REPEAT)).enableNumCheck(settingsManager.getProperty(PluginSettings.ENABLE_NUM_CHECK)).enableEmailCheck(settingsManager.getProperty(PluginSettings.ENABLE_EMAIL_CHECK)).enableUrlCheck(settingsManager.getProperty(PluginSettings.ENABLE_URL_CHECK)).enableWordCheck(settingsManager.getProperty(PluginSettings.ENABLE_WORD_CHECK)).wordResultCondition(condition).wordDeny(wD.get()).wordAllow(wA).numCheckLen(settingsManager.getProperty(PluginSettings.NUM_CHECK_LEN)).wordReplace(new WordReplace()).wordTag(WordTags.none()).charIgnore(new CharIgnore()).enableIpv4Check(settingsManager.getProperty(PluginSettings.ENABLE_IP_CHECK)).init();
            isInitialized = true;
        });
    }

    @Override
    public void onDisable() {
        listenerService.unregisterListeners();
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        TimingUtils.resetStatistics();
        ChatContext.forceClearContext();
        SignContext.forceClearContext();
        PlayerShadowController.clear();
        PlayerAltController.clear();
        if (voiceChatHookService != null) {
            voiceChatHookService.unregister();
        }
        BookCache.invalidateAll();
        WordReplace.clearCache();
        ViolationCounter.resetAllViolations();
        SchedulingUtils.cancelTaskSafely(violationResetTask);
        if (permCache != null) permCache.disable();
        if (isInitialized) sensitiveWordBs.destroy();
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setExecutor(null);
        Objects.requireNonNull(getCommand("asw")).setExecutor(null);
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setTabCompleter(null);
        Objects.requireNonNull(getCommand("asw")).setTabCompleter(null);
        LOGGER.info("AdvancedSensitiveWords is disabled!");
    }

}
