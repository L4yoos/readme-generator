package com.example.readmegenerator.domain.service;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

public class DependencyExtractor {

    public static String extractDependencies(List<Path> files) {
        StringBuilder all = new StringBuilder();

        for (Path file : files) {
            String name = file.getFileName().toString().toLowerCase();

            if (name.matches("package\\.json|composer\\.json|requirements\\.txt|pyproject\\.toml")) {
                try {
                    all.append(extractFromSingleFile(file)).append("\n");
                } catch (Exception e) {
                    all.append("> ⚠️ Could not parse ").append(name).append("\n");
                }
            }
        }

        return all.toString();
    }

    public static String extractDependencies(Path file) {
        return extractDependencies(List.of(file));
    }

    private static String extractFromSingleFile(Path file) throws IOException {
        String fileName = file.getFileName().toString().toLowerCase();
        String content = Files.readString(file);
        StringBuilder sb = new StringBuilder();

        switch (fileName) {
            case "composer.json":
                extractFromComposerJson(sb, content);
                break;

            case "package.json":
                extractFromPackageJson(sb, content);
                break;

            case "requirements.txt":
                extractFromRequirementsTxt(sb, content);
                break;

            case "pyproject.toml":
                extractFromPyprojectToml(sb, content);
                break;
        }

        return sb.toString();
    }

    private static void extractFromComposerJson(StringBuilder sb, String content) {
        JSONObject json = new JSONObject(content);
        sb.append("### 🧩 Built With (PHP)\n");

        if (json.has("require")) {
            addDeps(sb, json.getJSONObject("require"), "");
        }
        if (json.has("require-dev")) {
            addDeps(sb, json.getJSONObject("require-dev"), " *(dev)*");
        }
    }

    private static void extractFromPackageJson(StringBuilder sb, String content) {
        JSONObject json = new JSONObject(content);
        sb.append("### 🧩 Built With (JavaScript)\n");

        if (json.has("dependencies")) {
            addDeps(sb, json.getJSONObject("dependencies"), "");
        }
        if (json.has("devDependencies")) {
            addDeps(sb, json.getJSONObject("devDependencies"), " *(dev)*");
        }
    }

    private static void extractFromRequirementsTxt(StringBuilder sb, String content) {
        sb.append("### 🧩 Built With (Python - requirements.txt)\n");

        for (String line : content.split("\n")) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                sb.append("- ").append(line).append("\n");
            }
        }
    }

    private static void extractFromPyprojectToml(StringBuilder sb, String content) {
        sb.append("### 🧩 Built With (Python - pyproject.toml)\n");

        boolean inDeps = false;
        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.equals("[tool.poetry.dependencies]") || line.equals("[project.dependencies]")) {
                inDeps = true;
                continue;
            }
            if (line.startsWith("[") && !line.contains("dependencies")) {
                inDeps = false;
            }
            if (inDeps && line.contains("=")) {
                sb.append("- ").append(line).append("\n");
            }
        }
    }

    private static void addDeps(StringBuilder sb, JSONObject deps, String suffix) {
        for (Iterator<String> it = deps.keys(); it.hasNext(); ) {
            String name = it.next();
            String version = deps.optString(name, "unknown");

            if (HeuristicDependencyFilter.isRelevant(name)) {
                sb.append("- ").append(name).append(" ").append(version).append(suffix).append("\n");
            }
        }
    }
}

