package net.trustgames.toolkit.filter;

import net.trustgames.toolkit.managers.file.FileLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AdvertisementRegexList {

    /**
     * Load all the Patterns specified in the txt file
     * to a hashset and create a new file in the selected
     * directory (if not exists yet)
     *
     * @param dir Where to put the file
     * @return Set of all Patterns present in the file
     */
    public static HashSet<Pattern> loadSet(File dir) {
        String fileName = "ads-regex.txt";
        try {
            File file = FileLoader.loadFile(dir, fileName);

            BufferedReader reader = new BufferedReader(new FileReader(file));
            HashSet<Pattern> patternSet = new HashSet<>(reader.lines()
                    .map(Pattern::compile)
                    .collect(Collectors.toUnmodifiableSet())
            );
            reader.close();

            return patternSet;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load wordlist " + fileName ,e);
        }
    }
}
