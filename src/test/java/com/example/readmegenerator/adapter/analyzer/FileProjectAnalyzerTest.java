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
import java.util.Collections;
import java.util.Comparator;
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
        try (var walk = Files.walk(tempDir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            System.err.println("Failed to delete " + p + ": " + e.getMessage());
                        }
                    });
        }
    }

    @Test
    void testAnalyzeEmptyFileListReturnsEmptyString() throws IOException {
        List<Path> emptyFiles = Collections.emptyList();
        String result = analyzer.analyze(emptyFiles);
        assertEquals("", result, "Empty file list should return empty string");
    }

    @Test
    void testAnalyzeTextBasedFile() throws IOException {
        Path pomFile = Files.writeString(tempDir.resolve("pom.xml"), """
            <project>
                <groupId>com.example</groupId>
                <artifactId>sample-project</artifactId>
                <version>1.0.0</version>
                <name>Sample Project</name>
                <description>A sample project for testing.</description>
                <properties>
                    <java.version>17</java.version>
                </properties>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter</artifactId>
                    </dependency>
                </dependencies>
            </project>
            """);
        List<Path> files = List.of(pomFile);
        String result = analyzer.analyze(files);

        String expected = """
            ### Maven Project Configuration (pom.xml):
              - Name: Sample Project
              - Group ID: com.example
              - Artifact ID: sample-project
              - Version: 1.0.0
              - Java Version: 17
              - Description: A sample project for testing.
              - Key Dependencies:
                - org.springframework.boot:spring-boot-starter
            
            """;
        assertEquals(expected, result, "Should correctly process text-based file (pom.xml) with new format");
    }

    @Test
    void testAnalyzeDependencyFile() throws IOException {
        Path packageJson = Files.writeString(tempDir.resolve("package.json"), "{\"dependencies\": {\"express\": \"^4.17.1\"}}");
        List<Path> files = List.of(packageJson);
        String mockDependencies = "Dependencies: express@^4.17.1";
        try (MockedStatic<DependencyExtractor> mocked = mockStatic(DependencyExtractor.class)) {
            mocked.when(() -> DependencyExtractor.extractDependencies(packageJson)).thenReturn(mockDependencies);
            String result = analyzer.analyze(files);
            String expected = """
                ### Detected Dependencies from package.json:
                Dependencies: express@^4.17.1

                """;
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
            ### Code File Summary: `Sample.java`
            ```java
            public class Sample {
            public void method() {
            ```

            """;
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
            ### Code File Summary: `main.cpp`
            ```cpp
            #include <iostream>
            int main() {
            ```

            """;
        assertEquals(expected, result, "Should filter and include only relevant lines from C++ file");
    }

    @Test
    void testAnalyzeMixedFiles() throws IOException {
        Path pomFile = Files.writeString(tempDir.resolve("pom.xml"), """
            <project>
                <groupId>com.example</groupId>
                <artifactId>mixed-project</artifactId>
                <version>1.0.0</version>
                <name>Mixed Project</name>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-core</artifactId>
                    </dependency>
                </dependencies>
            </project>
            """);
        Path javaFile = Files.writeString(tempDir.resolve("Main.java"), """
            public class Main {
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
                ### Maven Project Configuration (pom.xml):
                  - Name: Mixed Project
                  - Group ID: com.example
                  - Artifact ID: mixed-project
                  - Version: 1.0.0
                  - Key Dependencies:
                    - org.springframework:spring-core
                
                ### Code File Summary: `Main.java`
                ```java
                public class Main {
                public void run() {}
                ```
                
                ### Detected Dependencies from package.json:
                Dependencies: lodash@^4.17.21
                
                """;
            assertEquals(expected, result, "Should correctly process mixed file types with new formats");
        }
    }

//    @Test
//    void testAnalyzeThrowsIOExceptionForInvalidFile() throws IOException {
//        Path unreadableDir = tempDir.resolve("unreadable_dir");
//        Files.createDirectory(unreadableDir);
//
//        List<Path> files = List.of(unreadableDir);
//        assertThrows(IOException.class, () -> analyzer.analyze(files), "Should throw IOException when trying to read a directory as a file");
//    }

    @Test
    void testAnalyzeIgnoresNonRelevantFiles() throws IOException {
        Path ignoredFile = Files.writeString(tempDir.resolve("image.png"), "binary data");
        List<Path> files = List.of(ignoredFile);
        String result = analyzer.analyze(files);
        assertEquals("", result, "Should ignore non-relevant files like images");
    }

    @Test
    void testAnalyzeDockerComposeFile() throws IOException {
        Path dockerComposeFile = Files.writeString(tempDir.resolve("docker-compose.yml"), """
            version: '3.8'
            services:
              web:
                build: .
                ports:
                  - "80:80"
              db:
                image: postgres:13
                environment:
                  POSTGRES_DB: mydb
            """);
        List<Path> files = List.of(dockerComposeFile);
        String result = analyzer.analyze(files);
        String expected = """
            ### Docker Compose Configuration (docker-compose.yml):
              - Defined Services:
                - web (Build Context: `.`)
                - db (Image: `postgres:13`)
            
            ---
            ### Detected CI/CD Tools and Configuration Files:
            
            The project utilizes the following CI/CD tools and configuration files:
            <ul>
              <li>**Docker Compose**: `docker-compose.yml`</li>
            </ul>
            
            """;
        assertEquals(expected, result, "Should correctly process docker-compose.yml and identify it as CI/CD");
    }
}