package com.example.readmegenerator.adapter.detector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DefaultLanguageDetectorTest {

    private DefaultLanguageDetector detector;

    @BeforeEach
    void setUp() {
        detector = new DefaultLanguageDetector();
    }

    @Test
    void shouldDetectSingleLanguageWithMinimumThreshold() {
        List<Path> files = List.of(
                Path.of("src/Main.java"),
                Path.of("src/Util.java"),
                Path.of("test/Test.java")
        );

        Set<String> detected = detector.detectLanguages(files);
        assertEquals(Set.of("Java"), detected);
    }

    @Test
    void shouldNotDetectLanguageBelowThreshold() {
        List<Path> files = List.of(
                Path.of("src/Main.java"),
                Path.of("src/Util.java")
        );

        Set<String> detected = detector.detectLanguages(files);
        assertTrue(detected.isEmpty());
    }

    @Test
    void shouldDetectMultipleLanguagesMeetingThreshold() {
        List<Path> files = List.of(
                Path.of("a.java"),
                Path.of("b.java"),
                Path.of("c.java"),
                Path.of("x.py"),
                Path.of("y.py"),
                Path.of("z.py"),
                Path.of("extra.rb") // tylko 1 plik Ruby
        );

        Set<String> detected = detector.detectLanguages(files);
        assertEquals(Set.of("Java", "Python"), detected);
    }

    @Test
    void shouldIgnoreUnknownExtensions() {
        List<Path> files = List.of(
                Path.of("file.unknown"),
                Path.of("anotherfile.random"),
                Path.of("README.md")
        );

        Set<String> detected = detector.detectLanguages(files);
        assertTrue(detected.isEmpty());
    }

    @Test
    void shouldBeCaseInsensitiveForExtensions() {
        List<Path> files = List.of(
                Path.of("Index.HTML"),
                Path.of("Page.Html"),
                Path.of("template.html")
        );

        Set<String> detected = detector.detectLanguages(files);
        assertEquals(Set.of("HTML"), detected);
    }

    @Test
    void shouldDetectBladePhpBeforeRegularPhp() {
        List<Path> files = List.of(
                Path.of("view1.blade.php"),
                Path.of("view2.blade.php"),
                Path.of("view3.blade.php"),
                Path.of("controller.php"),
                Path.of("model.php")
        );

        DefaultLanguageDetector detector = new DefaultLanguageDetector();
        Set<String> result = detector.detectLanguages(files);

        assertTrue(result.contains("PHP (Blade)"));
        assertFalse(result.contains("PHP"));
    }
}
