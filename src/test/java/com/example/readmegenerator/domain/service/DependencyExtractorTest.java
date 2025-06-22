package com.example.readmegenerator.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DependencyExtractorTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("deps-test");
    }

    @Test
    void testExtractFromPackageJson() throws IOException {
        Path file = createFile("package.json", """
            {
              "dependencies": {
                "express": "^2.17.1"
              },
              "devDependencies": {
                "jest": "^27.0.0"
              }
            }
        """);

        String result = DependencyExtractor.extractDependencies(file);

        assertTrue(result.contains("### 🧩 Built With (JavaScript)"));
        assertTrue(result.contains("express"));
        assertTrue(result.contains("jest"));
    }

    @Test
    void testExtractFromComposerJson() throws IOException {
        Path file = createFile("composer.json", """
            {
              "require": {
                "php": "^7.4",
                "guzzlehttp/guzzle": "^7.0"
              },
              "require-dev": {
                "phpunit/phpunit": "^9.5"
              }
            }
        """);

        String result = DependencyExtractor.extractDependencies(file);

        assertTrue(result.contains("Built With (PHP)"));
        assertTrue(result.contains("guzzlehttp/guzzle"));
        assertTrue(result.contains("phpunit/phpunit"));
    }

    @Test
    void testExtractFromRequirementsTxt() throws IOException {
        Path file = createFile("requirements.txt", """
            flask==2.0.1
            # this is a comment
            requests==2.25.1
        """);

        String result = DependencyExtractor.extractDependencies(file);

        assertTrue(result.contains("Built With (Python - requirements.txt)"));
        assertTrue(result.contains("flask==2.0.1"));
        assertTrue(result.contains("requests==2.25.1"));
        assertFalse(result.contains("this is a comment"));
    }

    @Test
    void testExtractFromPyprojectToml() throws IOException {
        Path file = createFile("pyproject.toml", """
            [tool.poetry.dependencies]
            flask = "^2.0"
            requests = "^2.25"

            [build-system]
            requires = ["setuptools", "wheel"]
        """);

        String result = DependencyExtractor.extractDependencies(file);

        assertTrue(result.contains("Built With (Python - pyproject.toml)"));
        assertTrue(result.contains("flask ="));
        assertTrue(result.contains("requests ="));
        assertFalse(result.contains("build-system"));
    }

    @Test
    void testInvalidJsonHandledGracefully() throws IOException {
        Path file = createFile("package.json", "{ invalid json ");

        String result = DependencyExtractor.extractDependencies(file);

        assertTrue(result.contains("⚠️ Could not parse"));
    }

    private Path createFile(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }
}

