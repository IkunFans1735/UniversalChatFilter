package io.wdsj.asw.bukkit.method;

import com.github.houbb.sensitive.word.api.IWordAllow;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;

public class ExternalWordAllow implements IWordAllow {
    private final File dataFolder;

    public ExternalWordAllow(Plugin plugin) {
        this.dataFolder = Paths.get(plugin.getDataFolder().getPath(),"external","allow").toFile();
    }
    @Override
    public List<String> allow() {
        final List<String> totalList = new ArrayList<>();

        if (Files.notExists(dataFolder.toPath())) {
            try {
                Files.createDirectories(dataFolder.toPath());
            } catch (IOException e) {
                LOGGER.severe("Error occurred while creating external allow directory: " + e.getMessage());
            }
        }
        try (Stream<Path> paths = Files.walk(dataFolder.toPath())) {
            List<File> files = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
            if (files.isEmpty()) return Collections.emptyList();

            files.parallelStream()
                    .forEach(file -> {
                        final boolean isWildCard = file.getName().toLowerCase(Locale.ROOT).contains("wildcard");
                        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
                            synchronized (totalList) {
                                if (isWildCard) {
                                    final WildCardLineResolver parser = new WildCardLineResolver();
                                    reader.lines().forEach(line -> totalList.addAll(parser.resolveWildCardLine(line)));
                                } else reader.lines().forEach(totalList::add);
                            }
                        } catch (IOException e) {
                            LOGGER.severe("Error reading file: " + file.getName());
                        }
                    });
            LOGGER.info("Loaded " + files.size() + " external allow file(s). " + "Total words: " + totalList.size());
        } catch (IOException e) {
            LOGGER.severe("Error occurred while loading external allow files: " + e.getMessage());
            return Collections.emptyList();
        }
        return totalList;
    }
}
