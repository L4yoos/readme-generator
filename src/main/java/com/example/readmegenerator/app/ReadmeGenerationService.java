package com.example.readmegenerator.app;

import com.example.readmegenerator.domain.model.ReadmeGenerationConfig;
import com.example.readmegenerator.domain.port.*;
import com.example.readmegenerator.domain.service.DependencyExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReadmeGenerationService {

    private final ProjectAnalyzerPort analyzer;
    private final LLMClientPort client;
    private final ReadmeWriterPort writer;
    private final LanguageDetectorPort languageDetector;
    private final PromptBuilderPort promptBuilder;
    private final TestAnalyzerPort testAnalyzer;
    private final boolean dryRun;
    private final boolean showPrompt;

    public ReadmeGenerationService(
            ProjectAnalyzerPort analyzer, LLMClientPort client,
            ReadmeWriterPort writer, LanguageDetectorPort languageDetector,
            PromptBuilderPort promptBuilder, TestAnalyzerPort testAnalyzer,
            boolean dryRun, boolean showPrompt
    ) {
        this.analyzer = analyzer;
        this.client = client;
        this.writer = writer;
        this.languageDetector = languageDetector;
        this.promptBuilder = promptBuilder;
        this.testAnalyzer = testAnalyzer;
        this.dryRun = dryRun;
        this.showPrompt = showPrompt;
    }

    public void generate(Path projectDir, ReadmeGenerationConfig config) throws Exception {
        List<Path> allFiles = listProjectFiles(projectDir);

        Set<String> detectedLanguages = languageDetector.detectLanguages(allFiles);
        if (detectedLanguages.isEmpty()) {
            return;
        }

        List<Path> relevantFiles = allFiles.stream()
                .filter(file -> isRelevant(file, detectedLanguages))
                .filter(file -> isCoreDomainFile(file))
                .collect(Collectors.toList());
        if (relevantFiles.isEmpty()) {
            return;
        }

        String projectName = projectDir.getFileName().toString();
        String summary = analyzer.analyze(relevantFiles);

        String testSummary = testAnalyzer.analyzeTests(allFiles);
        if (testSummary != null && !testSummary.isBlank()) {
            summary += "\n\n" + testSummary;
        }

        String dependencies = DependencyExtractor.extractDependencies(relevantFiles);
        if (!dependencies.isBlank()) {
            summary += "\n\n" + dependencies;
        }

        String prompt = promptBuilder.build(summary, projectName, config);

        if (showPrompt) {
            System.out.println("📤 [PROMPT]:\n" + prompt);
        }

        String readme = client.generateReadme(prompt);

        if (dryRun) {
            System.out.println("\n📄 [README PREVIEW]:\n" + readme);
            return;
        }

        writer.write(projectDir, readme);
    }

    private boolean isProjectFile(Path path) {
        String normalized = path.toString().toLowerCase();
        return !normalized.contains("node_modules") &&
                !normalized.contains(".git") &&
                !normalized.contains("vendor") &&
                !normalized.contains("build") &&
                !normalized.contains("dist") &&
                !normalized.contains("target") &&
                !normalized.contains("out");
    }

    private boolean isRelevant(Path path, Set<String> langs) {
        String name = path.getFileName().toString().toLowerCase();

        if (langs.contains("PHP")) {
            return name.endsWith(".php") || name.equals("composer.json");
        } else if (langs.contains("Java")) {
            return name.endsWith(".java") || name.equals("pom.xml") || name.equals("build.gradle");
        } else if (langs.contains("JavaScript") || langs.contains("TypeScript")) {
            return name.endsWith(".js") || name.endsWith(".ts") || name.equals("package.json") || name.equals("angular.json");
        } else if (langs.contains("Python")) {
            return name.endsWith(".py") || name.equals("requirements.txt") || name.equals("pyproject.toml");
        } else if (langs.contains("C/C++") || langs.contains("C/C++ Header")) {
            return name.endsWith(".cpp") || name.endsWith(".c") || name.endsWith(".h") || name.endsWith(".hpp") ||
                    name.equals("cmakelists.txt") || name.equals("makefile");
        }

        return false;
    }

    protected List<Path> listProjectFiles(Path projectDir) throws IOException {
        return Files.walk(projectDir)
                .filter(Files::isRegularFile)
                .filter(this::isProjectFile)
                .collect(Collectors.toList());
    }

    private boolean isCoreDomainFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.equals("pom.xml") ||
                name.contains("controller") ||
                name.contains("service") ||
                name.contains("application") ||
                name.contains("model") ||
                name.contains("entity") ||
                name.contains("domain");
    }
}
