package io.wdsj.asw.bukkit.method;

import com.github.houbb.sensitive.word.api.IWordContext;
import com.github.houbb.sensitive.word.api.IWordReplace;
import com.github.houbb.sensitive.word.api.IWordResult;
import com.github.houbb.sensitive.word.utils.InnerWordCharUtils;
import io.wdsj.asw.bukkit.setting.PluginSettings;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class WordReplace implements IWordReplace {
    private static final Map<String, String> replacementCache = new ConcurrentHashMap<>();
    private static boolean cacheInitialized = false;
    @Override
    public void replace(StringBuilder stringBuilder, char[] rawChars, IWordResult wordResult, IWordContext wordContext) {
        String sensitiveWord = InnerWordCharUtils.getString(rawChars, wordResult);
        if (!cacheInitialized) {
            for (String word : settingsManager.getProperty(PluginSettings.DEFINED_REPLACEMENT)) {
                String[] parts = word.split("\\|");
                if (parts.length == 2) {
                    int l = parts[1].length();
                    String definedSensitiveWord = parts[0];
                    String definedReplacement = parts[1].substring(0, l);
                    replacementCache.put(definedSensitiveWord, definedReplacement);
                }
            }
            cacheInitialized = true;
        }

        for (Map.Entry<String, String> entry : replacementCache.entrySet()) {
            String definedSensitiveWord = entry.getKey();
            String definedReplacement = entry.getValue();
            if (definedSensitiveWord.equals(sensitiveWord)) {
                stringBuilder.append(definedReplacement);
                return;
            }
        }
        int wordLength = wordResult.endIndex() - wordResult.startIndex();

        for (int i = 0; i < wordLength; ++i) {
            stringBuilder.append(settingsManager.getProperty(PluginSettings.REPLACEMENT));
        }

    }

    public static void clearCache() {
        replacementCache.clear();
        cacheInitialized = false;
    }
}
