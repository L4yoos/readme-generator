package com.example.readmegenerator.domain.port;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public interface LanguageDetectorPort {
    Set<String> detectLanguages(List<Path> files);
}

