package io.wdsj.asw.bukkit.ai;

import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
import io.github.amithkoujalgi.ollama4j.core.models.OllamaResult;
import io.github.amithkoujalgi.ollama4j.core.utils.OptionsBuilder;
import io.github.amithkoujalgi.ollama4j.core.utils.PromptBuilder;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

/**
 * Ollama AI processor
 */
public enum OllamaProcessor implements AIProcessor {
    INSTANCE;
    public static boolean isOllamaInit = false;
    private static PromptBuilder promptBuilder;
    private static OllamaAPI api;
    private static String modelName;

    /**
     * Initialize the Ollama service
     * @param modelAddress Ollama server address
     * @param name Model name to use
     * @param timeOut Timeout in seconds
     * @param debug Whether to enable debug logging
     */
    public void initService(String modelAddress, String name, int timeOut, boolean debug) {
        modelName = name;
        api = new OllamaAPI(modelAddress);
        api.setRequestTimeoutSeconds(timeOut);
        try {
            if (!api.ping()) {
                LOGGER.warning("Ollama ping failed, please check the api address");
                isOllamaInit = false;
                return;
            }
        } catch (Exception e) {
            LOGGER.warning("Ollama ping failed, please check the api address");
            isOllamaInit = false;
            return;
        }
        LOGGER.info("Successfully connect to ollama server");
        if (debug) {
            LOGGER.info("Ollama debug logging enabled");
        }
        api.setVerbose(debug);
        isOllamaInit = true;
    }

    @Override
    public void shutdown() {
        if (!THREAD_POOL.isShutdown()) {
            THREAD_POOL.shutdownNow();
        }
        api = null;
        isOllamaInit = false;
    }

    /**
     * Process the input message using OllamaProcessor
     * @param inputMessage The message to process
     * @return A future that will contain the returned message from Ollama server, the future inside may be null
     */
    @NotNull
    public static CompletableFuture<String> process(String inputMessage) {
        if (!isOllamaInit) {
            throw new IllegalStateException("OllamaProcessor is not initialized");
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                promptBuilder = new PromptBuilder().addLine(settingsManager.getProperty(PluginSettings.AI_MODEL_PROMPT)).addSeparator()
                        .add(inputMessage);
                OllamaResult response = api.generate(modelName, promptBuilder.build(),
                        new OptionsBuilder().build());
                return response.getResponse();
            } catch (Exception e) {
                LOGGER.warning("Error occurred while communicating with Ollama server: " + e.getMessage());
                return null;
            }
        }, THREAD_POOL);
    }
}
