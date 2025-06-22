package com.example.readmegenerator.adapter.analyzer;

import com.example.readmegenerator.domain.port.ProjectAnalyzerPort;
import com.example.readmegenerator.domain.service.DependencyExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileProjectAnalyzerTest {

    private ProjectAnalyzerPort analyzer;
    private Path tempDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        this.tempDir = tempDir;
        this.analyzer = new FileProjectAnalyzer();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted((p1, p2) -> -p1.compareTo(p2))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Test
    void testAnalyzeEmptyFileListReturnsEmptyString() throws IOException {
        List<Path> emptyFiles = Collections.emptyList();
        String result = analyzer.analyze(emptyFiles);
        assertEquals("", result, "Empty file list should return empty string");
    }

    @Test
    void testAnalyzeTextBasedFile() throws IOException {
        Path pomFile = Files.writeString(tempDir.resolve("pom.xml"), "<project>Sample</project>");
        List<Path> files = List.of(pomFile);
        String result = analyzer.analyze(files);
        String expected = "Found pom.xml:\n<project>Sample</project>\n\n";
        assertEquals(expected, result, "Should correctly process text-based file (pom.xml)");
    }

    @Test
    void testAnalyzeDependencyFile() throws IOException {
        Path packageJson = Files.writeString(tempDir.resolve("package.json"), "{\"dependencies\": {\"express\": \"^4.17.1\"}}");
        List<Path> files = List.of(packageJson);
        String mockDependencies = "Dependencies: express@^4.17.1";
        try (MockedStatic<DependencyExtractor> mocked = mockStatic(DependencyExtractor.class)) {
            mocked.when(() -> DependencyExtractor.extractDependencies(packageJson)).thenReturn(mockDependencies);
            String result = analyzer.analyze(files);
            String expected = mockDependencies + "\n";
            assertEquals(expected, result, "Should correctly process dependency file (package.json)");
        }
    }

    @Test
    void testAnalyzeCodeFileWithFilteredLines() throws IOException {
        String javaContent = """
                public class Sample {
                    private String field;
                    public void method() {
                        System.out.println("Hello");
                    }
                    int ignored = 0;
                }
                """;
        Path javaFile = Files.writeString(tempDir.resolve("Sample.java"), javaContent);
        List<Path> files = List.of(javaFile);
        String result = analyzer.analyze(files);
        String expected = """
                File: Sample.java
                public class Sample {
                public void method() {
                \n""";
        assertEquals(expected, result, "Should filter and include only relevant lines from Java file");
    }

    @Test
    void testAnalyzeCppFileWithFilteredLines() throws IOException {
        String cppContent = """
                #include <iostream>
                int main() {
                    std::cout << "Hello";
                    int x = 0;
                    return 0;
                }
                """;
        Path cppFile = Files.writeString(tempDir.resolve("main.cpp"), cppContent);
        List<Path> files = List.of(cppFile);
        String result = analyzer.analyze(files);
        String expected = """
                File: main.cpp
                #include <iostream>
                int main() {
                \n""";
        assertEquals(expected, result, "Should filter and include only relevant lines from C++ file");
    }

    @Test
    void testAnalyzeMixedFiles() throws IOException {
        Path pomFile = Files.writeString(tempDir.resolve("pom.xml"), "<project>Test</project>");
        Path javaFile = Files.writeString(tempDir.resolve("Test.java"), """
            public class Test {
                public void run() {}
            }
            """);
        Path packageJson = Files.writeString(tempDir.resolve("package.json"), "{\"dependencies\": {\"lodash\": \"^4.17.21\"}}");
        List<Path> files = List.of(pomFile, javaFile, packageJson);

        String mockDependencies = "Dependencies: lodash@^4.17.21";
        try (MockedStatic<DependencyExtractor> mocked = mockStatic(DependencyExtractor.class)) {
            mocked.when(() -> DependencyExtractor.extractDependencies(packageJson)).thenReturn(mockDependencies);

            String result = analyzer.analyze(files);

            String expected = """
                Found pom.xml:
                <project>Test</project>
                
                File: Test.java
                public class Test {
                public void run() {}
                
                Dependencies: lodash@^4.17.21
                """;
            assertEquals(expected, result, "Should correctly process mixed file types");
        }
    }

    @Test
    void testAnalyzeThrowsIOExceptionForInvalidFile() throws IOException {
        Path invalidFile = tempDir.resolve("pom.xml");
        Files.createFile(invalidFile);
        Files.setPosixFilePermissions(invalidFile, PosixFilePermissions.fromString("---------"));
        List<Path> files = List.of(invalidFile);
        assertThrows(IOException.class, () -> analyzer.analyze(files), "Should throw IOException for unreadable file");
    }

    @Test
    void testAnalyzeIgnoresNonRelevantFiles() throws IOException {
        Path ignoredFile = Files.writeString(tempDir.resolve("image.png"), "binary data");
        List<Path> files = List.of(ignoredFile);
        String result = analyzer.analyze(files);
        assertEquals("", result, "Should ignore non-relevant files like images");
    }
}