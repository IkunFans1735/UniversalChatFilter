package io.wdsj.asw.bukkit.service;

import com.alessiodp.libby.BukkitLibraryManager;
import com.alessiodp.libby.Library;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;

import java.util.Locale;

public class BukkitLibraryService {
    private final BukkitLibraryManager libraryManager;
    private static final Library ollama4j = Library.builder()
            .groupId("com{}github{}HaHaWTH")
            .artifactId("ollama4j-j8")
            .resolveTransitiveDependencies(true)
            .version("8ce2ad8840")
            .build();

    private static final Library openai4j = Library.builder()
            .groupId("dev{}ai4j")
            .artifactId("openai4j")
            .resolveTransitiveDependencies(true)
            .version("0.27.0")
            .build();

    private static final Library caffeine = Library.builder()
            .groupId("com{}github{}ben-manes{}caffeine")
            .artifactId("caffeine")
            .resolveTransitiveDependencies(false)
            .version("2.9.3")
            .build();

    public BukkitLibraryService(AdvancedSensitiveWords plugin) {
        libraryManager = new BukkitLibraryManager(plugin);
        if (Locale.getDefault().getCountry().toUpperCase(Locale.ROOT).equals("CN")) {
            libraryManager.addRepository("https://maven.aliyun.com/repository/public/");
            libraryManager.addRepository("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/");
            libraryManager.addRepository("https://repo.huaweicloud.com/repository/maven/");
        } else {
            libraryManager.addSonatype();
            libraryManager.addMavenCentral();
        }
        libraryManager.addJitPack();
    }

    public void loadRequired() {
        libraryManager.loadLibraries(caffeine);
    }
    public void loadOllamaOptional() {
        libraryManager.loadLibrary(ollama4j);
    }

    public void loadOpenAiOptional() {
        libraryManager.loadLibrary(openai4j);
    }

    public void loadWhisperJniOptional() {
        libraryManager.loadLibraries(Library.builder()
                .groupId("io{}github{}givimad")
                .artifactId("whisper-jni")
                .version("1.6.1")
                .build());
    }
}
