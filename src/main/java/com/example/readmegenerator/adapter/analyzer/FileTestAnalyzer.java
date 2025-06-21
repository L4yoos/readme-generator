package com.example.readmegenerator.adapter.analyzer;

import com.example.readmegenerator.domain.port.TestAnalyzerPort;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class FileTestAnalyzer implements TestAnalyzerPort {

    @Override
    public String analyzeTests(List<Path> allFiles) {
        List<Path> testFiles = allFiles.stream()
                .filter(path -> {
                    String name = path.getFileName().toString().toLowerCase();
                    String fullPath = path.toString().toLowerCase();
                    return name.contains("test") || fullPath.contains("/test") || fullPath.contains("\\test");
                })
                .collect(Collectors.toList());

        if (testFiles.isEmpty()) {
            return "🧪 No tests were detected in this project.";
        }

        // Prosty heurystyczny detektor frameworków
        boolean isJUnit = testFiles.stream().anyMatch(p -> p.toString().endsWith(".java"));
        boolean isPytest = testFiles.stream().anyMatch(p -> p.toString().endsWith(".py"));
        boolean isPhpUnit = testFiles.stream().anyMatch(p -> p.toString().endsWith(".php"));

        String framework = isJUnit ? "JUnit" : isPytest ? "PyTest" : isPhpUnit ? "PHPUnit" : "Unknown";

        return String.format("🧪 Detected %d test files using %s framework.", testFiles.size(), framework);
    }
}

