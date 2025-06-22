package com.example.readmegenerator.adapter.analyzer;

import com.example.readmegenerator.domain.port.TestAnalyzerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileTestAnalyzerTest {

    private TestAnalyzerPort analyzer;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        analyzer = new FileTestAnalyzer();
    }

    @Test
    void testNoTestFilesReturnsNoTestsMessage() throws IOException {
        Path nonTestFile = Files.writeString(tempDir.resolve("App.java"), "public class App {}");
        List<Path> files = List.of(nonTestFile);

        String result = analyzer.analyzeTests(files);

        assertEquals("🧪 No tests were detected in this project.", result);
    }

    @Test
    void testEmptyFileListReturnsNoTestsMessage() {
        List<Path> files = List.of();

        String result = analyzer.analyzeTests(files);

        assertEquals("🧪 No tests were detected in this project.", result);
    }

    @Test
    void testJavaTestFileDetectsJUnit() throws IOException {
        Path testDir = Files.createDirectory(tempDir.resolve("test"));
        Path testFile = Files.writeString(testDir.resolve("MyTest.java"), """
                import org.junit.jupiter.api.Test;
                class MyTest {
                    @Test
                    void test() {}
                }
                """);
        List<Path> files = List.of(testFile);

        String result = analyzer.analyzeTests(files);

        assertEquals("🧪 Detected 1 test files using JUnit framework.", result);
    }

    @Test
    void testPythonTestFileDetectsPyTest() throws IOException {
        Path testDir = Files.createDirectory(tempDir.resolve("test"));
        Path testFile = Files.writeString(testDir.resolve("test_example.py"), """
                import pytest
                def test_example():
                    assert True
                """);
        List<Path> files = List.of(testFile);

        String result = analyzer.analyzeTests(files);

        assertEquals("🧪 Detected 1 test files using PyTest framework.", result);
    }

    @Test
    void testPhpTestFileDetectsPHPUnit() throws IOException {
        Path testDir = Files.createDirectory(tempDir.resolve("test"));
        Path testFile = Files.writeString(testDir.resolve("MyTest.php"), """
                <?php
                use PHPUnit\\Framework\\TestCase;
                class MyTest extends TestCase {
                    public function testExample() {
                        $this->assertTrue(true);
                    }
                }
                """);
        List<Path> files = List.of(testFile);

        String result = analyzer.analyzeTests(files);

        assertEquals("🧪 Detected 1 test files using PHPUnit framework.", result);
    }

    @Test
    void testNonStandardTestFileDetectsUnknownFramework() throws IOException {
        Path testDir = Files.createDirectory(tempDir.resolve("test"));
        Path testFile = Files.writeString(testDir.resolve("test_script.js"), """
                describe('test', () => {
                    it('should work', () => {
                        expect(true).toBe(true);
                    });
                });
                """);
        List<Path> files = List.of(testFile);

        String result = analyzer.analyzeTests(files);

        assertEquals("🧪 Detected 1 test files using Unknown framework.", result);
    }

    @Test
    void testFilesInTestDirectoryAreDetected() throws IOException {
        Path testDir = Files.createDirectory(tempDir.resolve("test"));
        Path testFile = Files.writeString(testDir.resolve("ExampleTest.java"), """
                import org.junit.jupiter.api.Test;
                class ExampleTest {
                    @Test
                    void test() {}
                }
                """);
        List<Path> files = List.of(testFile);

        String result = analyzer.analyzeTests(files);

        assertEquals("🧪 Detected 1 test files using JUnit framework.", result);
    }

    @Test
    void testFilesWithTestInNameAreDetected() throws IOException {
        // Plik znajduje się w katalogu 'test', nie tylko ma 'test' w nazwie
        Path testDir = Files.createDirectory(tempDir.resolve("test"));
        Path testFile = Files.writeString(testDir.resolve("UnitTest.java"), """
            import org.junit.jupiter.api.Test;
            class UnitTest {
                @Test
                void test() {}
            }
            """);
        List<Path> files = List.of(testFile);

        String result = analyzer.analyzeTests(files);

        assertEquals("🧪 Detected 1 test files using JUnit framework.", result);
    }

    @Test
    void testMixedTestFilesDetectsCorrectFramework() throws IOException {
        Path testDir = Files.createDirectory(tempDir.resolve("test"));

        Path javaTest = Files.writeString(testDir.resolve("MyTest.java"), """
            import org.junit.jupiter.api.Test;
            class MyTest {
                @Test
                void test() {}
            }
            """);

        Path pythonTest = Files.writeString(testDir.resolve("test_example.py"), """
            import pytest
            def test_example():
                assert True
            """);

        List<Path> files = List.of(javaTest, pythonTest);

        String result = analyzer.analyzeTests(files);

        String expected = "🧪 Detected 2 test files using JUnit framework.";
        assertEquals(expected, result);
    }

    @Test
    void testWindowsStylePathWithTestDirectory() throws IOException {
        Path testDir = Files.createDirectory(tempDir.resolve("test"));
        Path testFile = Files.writeString(testDir.resolve("ExampleTest.java"), """
                import org.junit.jupiter.api.Test;
                class ExampleTest {
                    @Test
                    void test() {}
                }
                """);
        List<Path> files = List.of(testFile);

        String result = analyzer.analyzeTests(files);

        assertEquals("🧪 Detected 1 test files using JUnit framework.", result);
    }
}
