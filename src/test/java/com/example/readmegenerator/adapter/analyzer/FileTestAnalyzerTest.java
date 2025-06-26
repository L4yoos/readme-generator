package com.example.readmegenerator.adapter.analyzer;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileTestAnalyzerTest {

    private final FileTestAnalyzer analyzer = new FileTestAnalyzer();

    @Test
    void shouldReturnMessageWhenTestFilesArePresentInTestFolder() {
        List<Path> files = List.of(
                Path.of("src/test/java/com/example/MyTest.java"),
                Path.of("src/main/java/com/example/MyClass.java")
        );

        String result = analyzer.analyzeTests(files);

        assertEquals("🧪 This project contains test files.", result);
    }

    @Test
    void shouldReturnMessageWhenFileNameContainsTest() {
        List<Path> files = List.of(
                Path.of("src/main/java/com/example/PaymentServiceTest.java")
        );

        String result = analyzer.analyzeTests(files);

        assertEquals("🧪 This project contains test files.", result);
    }

    @Test
    void shouldReturnEmptyStringWhenNoTestFilesPresent() {
        List<Path> files = List.of(
                Path.of("src/main/java/com/example/MyClass.java"),
                Path.of("README.md")
        );

        String result = analyzer.analyzeTests(files);

        assertEquals("", result);
    }

    @Test
    void shouldDetectWindowsStylePaths() {
        List<Path> files = List.of(
                Path.of("C:\\project\\src\\test\\java\\MyTest.java")
        );

        String result = analyzer.analyzeTests(files);

        assertEquals("🧪 This project contains test files.", result);
    }
}
