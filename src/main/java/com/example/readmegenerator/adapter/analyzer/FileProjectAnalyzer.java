package com.example.readmegenerator.adapter.analyzer;

import com.example.readmegenerator.domain.port.ProjectAnalyzerPort;
import com.example.readmegenerator.domain.service.DependencyExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FileProjectAnalyzer implements ProjectAnalyzerPort {

    private static final Logger logger = LoggerFactory.getLogger(FileProjectAnalyzer.class);

    private static final Set<String> TEXT_BASED_FILES = Set.of(
            "pom.xml", "docker-compose.yml", "cmakelists.txt", "makefile"
    );

    private static final Set<String> BUILD_FILES = Set.of(
            "pom.xml", "build.gradle", "package.json", "requirements.txt", "docker-compose.yml"
    );

    private static final Set<String> DEPENDENCY_FILES = Set.of(
            "composer.json", "package.json", "requirements.txt", "pyproject.toml"
    );

    private static final Set<String> CODE_EXTENSIONS = Set.of(
            ".java", ".php", ".js", ".py", ".cpp", ".c", ".h", ".hpp"
    );

    @Override
    public String analyze(List<Path> files) throws IOException {
        logger.debug("Starting analysis of {} files", files.size());

        StringBuilder sb = new StringBuilder();

        Path root = findCommonRoot(files);
        if (root != null) {
            logger.debug("Detected root directory: {}", root);
            List<String> subprojects = detectSubprojects(root);

            if (subprojects.size() > 1) {
                sb.append("This project appears to be a monorepo containing the following services:\n");
                for (String service : subprojects) {
                    sb.append("- ").append(service).append("\n");
                }
                sb.append("\n");
            }
        } else {
            logger.debug("Could not detect root directory");
        }

        for (Path file : files) {
            String fileName = file.getFileName().toString().toLowerCase();
            logger.debug("Inspecting file: {}", fileName);

            if (TEXT_BASED_FILES.contains(fileName)) {
                appendTextFile(sb, fileName, file);

            } else if (DEPENDENCY_FILES.contains(fileName)) {
                logger.debug("Found dependency file: {}", fileName);
                sb.append(DependencyExtractor.extractDependencies(file)).append("\n");

            } else if (hasCodeExtension(fileName)) {
                appendCodeSummary(sb, fileName, file);
            }
        }

        return sb.toString();
    }

    private void appendTextFile(StringBuilder sb, String label, Path file) throws IOException {
        sb.append("Found ").append(label).append(":\n");
        sb.append(Files.readString(file)).append("\n\n");
    }

    private boolean hasCodeExtension(String fileName) {
        return CODE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private void appendCodeSummary(StringBuilder sb, String fileName, Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);

        Predicate<String> filter = getLanguageLineFilter(fileName);

        String summary = lines.stream()
                .filter(filter)
                .map(String::trim)
                .limit(30)
                .collect(Collectors.joining("\n"));

        sb.append("File: ").append(file.getFileName()).append("\n");
        sb.append(summary).append("\n\n");
    }

    private Predicate<String> getLanguageLineFilter(String fileName) {
        if (fileName.endsWith(".cpp") || fileName.endsWith(".c") || fileName.endsWith(".h") || fileName.endsWith(".hpp")) {
            return line -> line.matches("^\\s*(#include|#define|class\\s+\\w+|\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{).*");
        }

        return line ->
                line.contains("class") ||
                        line.contains("interface") ||
                        line.contains("@") ||
                        line.contains("public") ||
                        line.contains("function") ||
                        line.contains("def");
    }

    private List<String> detectSubprojects(Path root) throws IOException {
        List<String> subprojects = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    boolean hasBuildFile = Files.list(path)
                            .anyMatch(p -> BUILD_FILES.contains(p.getFileName().toString().toLowerCase()));
                    if (hasBuildFile) {
                        subprojects.add(path.getFileName().toString());
                    }
                }
            }
        }

        return subprojects;
    }

    private Path findCommonRoot(List<Path> files) {
        if (files.isEmpty()) return null;

        Path commonRoot = files.get(0).getParent();

        for (Path file : files) {
            Path parent = file.getParent();
            while (parent != null && !parent.startsWith(commonRoot)) {
                commonRoot = commonRoot.getParent();
            }
        }
        return commonRoot;
    }
}