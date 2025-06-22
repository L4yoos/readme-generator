package com.example.readmegenerator.adapter.detector;

import com.example.readmegenerator.domain.port.LanguageDetectorPort;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultLanguageDetector implements LanguageDetectorPort {

    private static final Map<String, String> EXT_TO_LANGUAGE = Map.ofEntries(
            Map.entry(".blade.php", "PHP (Blade)"),
            Map.entry(".php", "PHP"),
            Map.entry(".js", "JavaScript"),
            Map.entry(".ts", "TypeScript"),
            Map.entry(".vue", "Vue"),
            Map.entry(".scss", "SCSS"),
            Map.entry(".css", "CSS"),
            Map.entry(".html", "HTML"),
            Map.entry(".java", "Java"),
            Map.entry(".py", "Python"),
            Map.entry(".go", "Go"),
            Map.entry(".rb", "Ruby"),
            Map.entry(".kt", "Kotlin"),
            Map.entry(".cs", "C#"),
            Map.entry(".cpp", "C++"),
            Map.entry(".c", "C"),
            Map.entry(".h", "C/C++ Header")
    );

    @Override
    public Set<String> detectLanguages(List<Path> files) {
        Map<String, Integer> languageCounts = new HashMap<>();

        List<Map.Entry<String, String>> sortedExtensions = EXT_TO_LANGUAGE.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getKey().length(), e1.getKey().length()))
                .toList();

        for (Path file : files) {
            String name = file.getFileName().toString().toLowerCase();
            for (Map.Entry<String, String> entry : sortedExtensions) {
                if (name.endsWith(entry.getKey())) {
                    String lang = entry.getValue();
                    languageCounts.put(lang, languageCounts.getOrDefault(lang, 0) + 1);
                    break;
                }
            }
        }

        return languageCounts.entrySet().stream()
                .filter(e -> e.getValue() >= 3) // Tylko języki z min. 3 plikami
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}

