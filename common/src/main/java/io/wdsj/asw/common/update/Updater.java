package io.wdsj.asw.common.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.wdsj.asw.common.template.PluginVersionTemplate;
import org.jetbrains.annotations.NotNull;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class Updater {
    private static String currentVersion = PluginVersionTemplate.VERSION;
    private static String latestVersion;
    private static boolean isUpdateAvailable = false;
    private static boolean isErred = false;
    private static final String USER_NAME = "HaHaWTH";
    private static final String REPOSITORY = "AdvancedSensitiveWords";
    private static final String RELEASE_URL = "https://api.github.com/repos/" + USER_NAME + "/" + REPOSITORY + "/releases/latest";
    private static final String COMMITS_URL = "https://api.github.com/repos/" + USER_NAME + "/" + REPOSITORY + "/commits/" + PluginVersionTemplate.COMMIT_BRANCH;
    @SuppressWarnings("ConstantConditions") // IDE doesn't know this will be replaced
    private static final boolean isDevChannel = PluginVersionTemplate.VERSION_CHANNEL.equalsIgnoreCase("dev");

    /**
     * Check if there is an update available
     * Note: This method will perform a network request!
     * @return true if there is an update available, false otherwise
     */
    public static boolean isUpdateAvailable() {
        currentVersion = PluginVersionTemplate.VERSION;
        if (isDevChannel) {
            return isDevUpdateAvailable();
        }
        URI uri = URI.create(RELEASE_URL);
        try {
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                JsonObject jsonObject = new JsonParser().parse(reader).getAsJsonObject();
                String latest = jsonObject.get("tag_name").getAsString();
                latestVersion = latest;
                isUpdateAvailable = !currentVersion.equals(latest);
                reader.close();
                return isUpdateAvailable;
            }
        } catch (Exception e) {
            isErred = true;
            isUpdateAvailable = false;
            return false;
        }
    }

    protected static boolean isDevUpdateAvailable() {
        URI uri = URI.create(COMMITS_URL);
        try {
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                    JsonObject jsonObject = new JsonParser().parse(reader).getAsJsonObject();
                    String latestHash = jsonObject.get("sha").getAsString();
                    latestVersion = latestHash.substring(0, 7);
                    currentVersion = PluginVersionTemplate.COMMIT_HASH_SHORT;
                    isUpdateAvailable = !PluginVersionTemplate.COMMIT_HASH.equals(latestHash);
                    reader.close();
                    return isUpdateAvailable;
                }
            }
        } catch (Exception ignored) {
        }
        isErred = true;
        isUpdateAvailable = false;
        return false;
    }

    public static String getLatestVersion() {
        return latestVersion;
    }
    @NotNull
    public static String getCurrentVersion() {
        return currentVersion;
    }

    /**
     * Returns true if there is an update available, false otherwise
     * Must be called after {@link Updater#isUpdateAvailable()}
     * @return A boolean indicating whether there is an update available
     */
    public static boolean hasUpdate() {
        return isUpdateAvailable;
    }

    public static boolean isErred() {
        return isErred;
    }

    public static boolean isDevChannel() {
        return isDevChannel;
    }
}