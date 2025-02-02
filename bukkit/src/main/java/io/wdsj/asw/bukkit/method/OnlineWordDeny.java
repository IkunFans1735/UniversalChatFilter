package io.wdsj.asw.bukkit.method;

import com.github.houbb.sensitive.word.api.IWordDeny;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

/**
 * OnlineWordDeny for ASW.
 * @since Dragon
 */
public class OnlineWordDeny implements IWordDeny {
    private final File dataFolder;
    private final File cacheFile;
    private final File timestampFile;
    private final String charset = settingsManager.getProperty(PluginSettings.ONLINE_WORDS_ENCODING);
    private final boolean isCacheEnabled = settingsManager.getProperty(PluginSettings.CACHE_ONLINE_WORDS);

    public OnlineWordDeny(Plugin plugin) {
        this.dataFolder = Paths.get(plugin.getDataFolder().getPath(), "cache").toFile();
        this.cacheFile = new File(dataFolder, "cache_online_deny_words.txt");
        this.timestampFile = new File(dataFolder, "cache_online_deny_words_timestamp.txt");
    }
    @Override
    public List<String> deny() {
        if (isCacheEnabled && cacheFile.exists() && !isCacheExpired()) {
            try {
                LOGGER.info("Loading cached words from file.");
                return Files.readAllLines(cacheFile.toPath());
            } catch (IOException e) {
                LOGGER.warning("Failed to load cached words from file: " + e.getMessage());
            }
        }

        String link = settingsManager.getProperty(PluginSettings.ONLINE_WORDS_URL);
        List<String> lines = new ArrayList<>();
        URI uri = URI.create(link);

        try {
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(10000);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), charset))) {
                reader.lines().forEach(lines::add);
            } finally {
                connection.disconnect();
            }
            if (isCacheEnabled) saveToCache(lines);
        } catch (Exception e) {
            LOGGER.warning("Failed to load online words list from URL: " + link);
            return Collections.emptyList();
        }

        LOGGER.info("Loaded " + lines.size() + " word(s) online.");
        return lines;
    }

    private boolean isCacheExpired() {
        if (!timestampFile.exists()) {
            return true;
        }
        final long timeout = settingsManager.getProperty(PluginSettings.ONLINE_WORDS_CACHE_TIMEOUT);
        if (timeout < 0) {
            return false;
        }
        try {
            long lastModified = Long.parseLong(new String(Files.readAllBytes(timestampFile.toPath())).trim());
            long currentTime = System.currentTimeMillis();
            return (currentTime - lastModified) > timeout * 60L * 1000L;
        } catch (IOException | NumberFormatException e) {
            LOGGER.warning("Failed to read cache timestamp: " + e.getMessage());
            return true;
        }
    }

    private void saveToCache(List<String> words) {
        if (!Files.exists(dataFolder.toPath())) {
            try {
                Files.createDirectories(dataFolder.toPath());
            } catch (IOException e) {
                LOGGER.severe("Error occurred while creating cache directory: " + e.getMessage());
            }
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(cacheFile, false), charset)) {
            for (String word : words) {
                writer.write(word + System.lineSeparator());
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to save words to cache file: " + e.getMessage());
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(timestampFile, false), charset)) {
            writer.write(String.valueOf(System.currentTimeMillis()));
        } catch (IOException e) {
            LOGGER.warning("Failed to update cache timestamp: " + e.getMessage());
        }
    }
}