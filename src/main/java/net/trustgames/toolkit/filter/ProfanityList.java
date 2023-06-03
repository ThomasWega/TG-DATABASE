package net.trustgames.toolkit.filter;

import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.managers.file.FileLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ProfanityList {

    /**
     * Load all the words specified in the txt file
     * to a hashset and create a new file in the selected
     * directory (if not exists yet)
     *
     * @param dir Where to put the file
     * @return Set of all words present in the file
     */
    public static HashSet<String> loadSet(File dir) {
        String fileName = "profanity.txt";
        try {
            File file = FileLoader.loadFile(Toolkit.class.getClassLoader(), dir, fileName);

            BufferedReader reader = new BufferedReader(new FileReader(file));
            HashSet<String> hashSet = new HashSet<>(reader.lines().collect(Collectors.toUnmodifiableSet()));
            reader.close();

            return hashSet;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load wordlist " + fileName ,e);
        }
    }
}
