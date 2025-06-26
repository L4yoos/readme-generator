package com.example.readmegenerator.adapter.analyzer;

import com.example.readmegenerator.domain.port.TestAnalyzerPort;

import java.nio.file.Path;
import java.util.List;

public class FileTestAnalyzer implements TestAnalyzerPort {

    @Override
    public String analyzeTests(List<Path> allFiles) {
        boolean hasTests = allFiles.stream()
                .anyMatch(path -> {
                    String filePath = path.toString().toLowerCase();
                    return filePath.contains("/test/") ||
                            filePath.contains("\\test\\") ||
                            filePath.contains("test");
                });

        return hasTests
                ? "🧪 This project contains test files."
                : "";
    }
}

