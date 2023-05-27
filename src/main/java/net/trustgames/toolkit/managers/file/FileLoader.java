package net.trustgames.toolkit.managers.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class FileLoader {

    /**
     * Creates the new file (if not exists)
     * with the default contents of the same file in the resources folder
     *
     * @param directory        Directory where to save the desired file
     * @param configName Name of the file
     * @return The created file with filled in default values
     * @throws IOException Trying to get the default content from resources folder
     */
    public static File loadFile(File directory, String configName) throws IOException {
        Path configPath = directory.toPath().resolve(configName);
        if (!Files.exists(configPath)) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();

            try (var stream = FileLoader.class.getClassLoader().getResourceAsStream(configName)) {
                Files.copy(Objects.requireNonNull(stream), configPath);
            }
        }
        return configPath.toFile();
    }
}
