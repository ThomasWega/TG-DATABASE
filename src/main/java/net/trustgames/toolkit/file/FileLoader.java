package net.trustgames.toolkit.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FileLoader {

    /**
     * Creates the new file (if not exists)
     * with the default contents of the same file in the resources folder
     *
     * @param classLoader To determine from which plugin resources folder to get the file
     * @param directory   Directory where to save the desired file
     * @param configName  Name of the file
     * @return The created file with filled in default values
     * @throws IOException Trying to get the default content from resources folder
     */
    public static File loadFile(ClassLoader classLoader, File directory, String configName) throws IOException {
        Path configPath = directory.toPath().resolve(configName);
        if (!Files.exists(configPath)) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();

            try (var stream = classLoader.getResourceAsStream(configName)) {
                Files.copy(Objects.requireNonNull(stream,
                        "File " + configName + " is not present in the resources folder of " + classLoader.getName()
                ), configPath);
            }
        }
        return configPath.toFile();
    }

    /**
     * @return Map of: <p>
     * - String - name of the config (eg. database.yml)<p>
     * - File - the created file
     * @see FileLoader#loadFile(ClassLoader, File, String)
     */
    public static Map<String, File> loadAllFiles(ClassLoader classLoader, File directory, String... configNames) throws IOException {
        Map<String, File> configsCreated = new HashMap<>();
        for (String configName : configNames) {
            configsCreated.put(configName, loadFile(classLoader, directory, configName));
        }
        return configsCreated;
    }
}
