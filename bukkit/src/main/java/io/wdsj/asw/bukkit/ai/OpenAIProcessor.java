package io.wdsj.asw.bukkit.ai;

import com.github.houbb.heaven.support.tuple.impl.Pair;
import dev.ai4j.openai4j.OpenAiClient;
import dev.ai4j.openai4j.moderation.Categories;
import dev.ai4j.openai4j.moderation.ModerationRequest;
import dev.ai4j.openai4j.moderation.ModerationResponse;
import dev.ai4j.openai4j.moderation.ModerationResult;
import io.wdsj.asw.bukkit.setting.PluginSettings;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static dev.ai4j.openai4j.moderation.ModerationModel.TEXT_MODERATION_LATEST;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

/**
 * OpenAI Moderation Processor.
 */
public class OpenAIProcessor implements AIProcessor {
    public static boolean isOpenAiInit = false;
    private static OpenAiClient client;
    public OpenAIProcessor() {
    }

    /**
     * Initialize the OpenAI moderation service.
     * @param apikey the openai key
     * @param debug whether to enable debug logging
     */
    public void initService(String apikey, boolean debug) {
        @SuppressWarnings("rawtypes")
        OpenAiClient.Builder builder = OpenAiClient.builder()
                        .openAiApiKey(apikey);
        if (settingsManager.getProperty(PluginSettings.OPENAI_ENABLE_HTTP_PROXY)) {
            builder.proxy(Proxy.Type.HTTP, settingsManager.getProperty(PluginSettings.OPENAI_HTTP_PROXY_ADDRESS), settingsManager.getProperty(PluginSettings.OPENAI_HTTP_PROXY_PORT));
        }
        if (debug) {
            builder.logResponses(true)
                    .logRequests(true);
        }
        client = builder.build();
        isOpenAiInit = true;
    }

    @Override
    public void shutdown() {
        if (isOpenAiInit && client != null) {
            client.shutdown();
        }
        if (!THREAD_POOL.isShutdown()) {
            THREAD_POOL.shutdownNow();
        }
        isOpenAiInit = false;
    }

    /**
     * Process the input message using OpenAI moderation.
     * @param inputMessage the input message
     * @return A future contains the moderation response
     */
    public static CompletableFuture<Boolean> process(String inputMessage) {
        if (!isOpenAiInit) {
            throw new IllegalStateException("OpenAI Moderation Processor is not initialized");
        }
        ModerationRequest request = ModerationRequest.builder()
                .input(inputMessage)
                .model(TEXT_MODERATION_LATEST)
                .build();
        return CompletableFuture.supplyAsync(() -> {
            try {
                ModerationResponse resp = client.moderation(request)
                        .execute();
                if (resp != null) {
                    for (ModerationResult result : resp.results()) {
                        if (result.isFlagged()) {
                            Categories categories = result.categories();
                            List<Pair<Boolean, Boolean>> categoryChecks = new ArrayList<>();
                            categoryChecks.add(Pair.of(categories.hateThreatening(), settingsManager.getProperty(PluginSettings.OPENAI_ENABLE_HATE_THREATENING_CHECK)));
                            categoryChecks.add(Pair.of(categories.hate(), settingsManager.getProperty(PluginSettings.OPENAI_ENABLE_HATE_CHECK)));
                            categoryChecks.add(Pair.of(categories.selfHarm(), settingsManager.getProperty(PluginSettings.OPENAI_ENABLE_SELF_HARM_CHECK)));
                            categoryChecks.add(Pair.of(categories.sexual(), settingsManager.getProperty(PluginSettings.OPENAI_ENABLE_SEXUAL_CONTENT_CHECK)));
                            categoryChecks.add(Pair.of(categories.sexualMinors(), settingsManager.getProperty(PluginSettings.OPENAI_ENABLE_SEXUAL_MINORS_CHECK)));
                            categoryChecks.add(Pair.of(categories.violence(), settingsManager.getProperty(PluginSettings.OPENAI_ENABLE_VIOLENCE_CHECK)));
                            return categoryChecks.stream()
                                    .anyMatch(pair -> pair.getValueOne() && pair.getValueTwo());
                        }
                    }
                }
                return null;
            } catch (Exception e) {
                LOGGER.warning("OpenAI Moderation error: " + e.getMessage());
                return null;
            }
        }, THREAD_POOL);
    }
}
