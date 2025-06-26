package com.example.readmegenerator.app;

import com.example.readmegenerator.domain.model.ReadmeGenerationConfig;
import com.example.readmegenerator.domain.port.*;
import com.example.readmegenerator.domain.service.DependencyExtractor;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReadmeGenerationServiceTest {

    @Mock private ProjectAnalyzerPort analyzer;
    @Mock private LLMClientPort client;
    @Mock private ReadmeWriterPort writer;
    @Mock private LanguageDetectorPort languageDetector;
    @Mock private PromptBuilderPort promptBuilder;
    @Mock private TestAnalyzerPort testAnalyzer;

    private ReadmeGenerationConfig config;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        config = new ReadmeGenerationConfig(
                ReadmeGenerationConfig.HeaderAlignment.LEFT,
                ReadmeGenerationConfig.ListStyle.BULLET
        );
        tempDir = Files.createTempDirectory("readme-service-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .map(Path::toFile)
                .forEach(java.io.File::delete);
    }

    @Test
    void shouldGenerateReadmeAndWriteToFile() throws Exception {
        // given
        Path file = Files.writeString(tempDir.resolve("UserService.java"), "public class UserService {}");

        when(languageDetector.detectLanguages(anyList())).thenReturn(Set.of("Java"));
        when(analyzer.analyze(anyList())).thenReturn("Project Summary");
        when(testAnalyzer.analyzeTests(anyList())).thenReturn("Test Summary");
        when(promptBuilder.build(any(), any(), any())).thenReturn("Prompt");
        when(client.generateReadme(anyString())).thenReturn("README");
        // mock static DependencyExtractor
        try (MockedStatic<DependencyExtractor> deps = mockStatic(DependencyExtractor.class)) {
            deps.when(() -> DependencyExtractor.extractDependencies(anyList())).thenReturn("Dependencies");

            ReadmeGenerationService service = new ReadmeGenerationService(
                    analyzer, client, writer, languageDetector,
                    promptBuilder, testAnalyzer, false, false
            );

            // when
            service.generate(tempDir, config);

            // then
            verify(writer).write(eq(tempDir), eq("README"));
        }
    }

    @Test
    void shouldSkipWritingIfDryRunIsEnabled() throws Exception {
        Path file = Files.writeString(tempDir.resolve("MyService.java"), "public class MyService {}");

        when(languageDetector.detectLanguages(anyList())).thenReturn(Set.of("Java"));
        when(analyzer.analyze(anyList())).thenReturn("Summary");
        when(testAnalyzer.analyzeTests(anyList())).thenReturn("");
        when(promptBuilder.build(any(), any(), any())).thenReturn("Prompt");
        when(client.generateReadme(anyString())).thenReturn("README");

        try (MockedStatic<DependencyExtractor> deps = mockStatic(DependencyExtractor.class)) {
            deps.when(() -> DependencyExtractor.extractDependencies(anyList())).thenReturn("");

            ReadmeGenerationService service = new ReadmeGenerationService(
                    analyzer, client, writer, languageDetector,
                    promptBuilder, testAnalyzer, true, false // dryRun = true
            );

            service.generate(tempDir, config);

            verify(writer, never()).write(any(), any());
        }
    }

    @Test
    void shouldExitEarlyIfNoLanguagesDetected() throws Exception {
        Files.writeString(tempDir.resolve("Main.java"), "public class Main {}");
        when(languageDetector.detectLanguages(anyList())).thenReturn(Set.of());

        ReadmeGenerationService service = new ReadmeGenerationService(
                analyzer, client, writer, languageDetector,
                promptBuilder, testAnalyzer, false, false
        );

        service.generate(tempDir, config);

        verifyNoInteractions(analyzer, testAnalyzer, promptBuilder, client, writer);
    }

    @Test
    void shouldExitEarlyIfNoRelevantFiles() throws Exception {
        Files.writeString(tempDir.resolve("README.txt"), "Just a readme");
        when(languageDetector.detectLanguages(anyList())).thenReturn(Set.of("Java"));

        ReadmeGenerationService service = new ReadmeGenerationService(
                analyzer, client, writer, languageDetector,
                promptBuilder, testAnalyzer, false, false
        );

        service.generate(tempDir, config);

        verifyNoInteractions(analyzer, testAnalyzer, promptBuilder, client, writer);
    }

    @Test
    void shouldPrintPromptIfFlagSet() throws Exception {
        Path file = Files.writeString(tempDir.resolve("OrderService.java"), "public class OrderService {}");

        when(languageDetector.detectLanguages(anyList())).thenReturn(Set.of("Java"));
        when(analyzer.analyze(anyList())).thenReturn("Analysis");
        when(testAnalyzer.analyzeTests(anyList())).thenReturn("");
        when(promptBuilder.build(any(), any(), any())).thenReturn("PROMPT");
        when(client.generateReadme(any())).thenReturn("README");

        try (MockedStatic<DependencyExtractor> deps = mockStatic(DependencyExtractor.class)) {
            deps.when(() -> DependencyExtractor.extractDependencies(anyList())).thenReturn("");

            ReadmeGenerationService service = new ReadmeGenerationService(
                    analyzer, client, writer, languageDetector,
                    promptBuilder, testAnalyzer, false, true // showPrompt = true
            );

            assertDoesNotThrow(() -> service.generate(tempDir, config));
            verify(promptBuilder).build(anyString(), anyString(), any());
            verify(client).generateReadme("PROMPT");
        }
    }
}
