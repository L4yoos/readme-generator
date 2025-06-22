package com.example.readmegenerator.app;

import com.example.readmegenerator.domain.model.ReadmeGenerationConfig;
import com.example.readmegenerator.domain.port.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReadmeGenerationServiceTest {

    private ProjectAnalyzerPort analyzer;
    private LLMClientPort client;
    private ReadmeWriterPort writer;
    private LanguageDetectorPort languageDetector;
    private PromptBuilderPort promptBuilder;
    private TestAnalyzerPort testAnalyzer;

    private ReadmeGenerationService service;

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        analyzer = mock(ProjectAnalyzerPort.class);
        client = mock(LLMClientPort.class);
        writer = mock(ReadmeWriterPort.class);
        languageDetector = mock(LanguageDetectorPort.class);
        promptBuilder = mock(PromptBuilderPort.class);
        testAnalyzer = mock(TestAnalyzerPort.class);

        service = new ReadmeGenerationService(
                analyzer, client, writer, languageDetector, promptBuilder, testAnalyzer,
                false, false // dryRun, showPrompt
        );

        tempDir = Files.createTempDirectory("readme-test");
    }

    @AfterEach
    void cleanup() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void testGenerateReadmeSuccessfully() throws Exception {
        Path file = Files.writeString(tempDir.resolve("Test.java"), "public class Test {}");

        Set<String> langs = Set.of("Java");
        String summary = "Project summary";
        String testSummary = "Test summary";
        String prompt = "Final prompt";
        String readme = "# README";

        when(languageDetector.detectLanguages(anyList())).thenReturn(langs);
        when(analyzer.analyze(anyList())).thenReturn(summary);
        when(testAnalyzer.analyzeTests(anyList())).thenReturn(testSummary);
        when(promptBuilder.build(any(), any(), any())).thenReturn(prompt);
        when(client.generateReadme(any())).thenReturn(readme);

        ReadmeGenerationConfig config = new ReadmeGenerationConfig(
                ReadmeGenerationConfig.HeaderAlignment.LEFT,
                ReadmeGenerationConfig.ListStyle.BULLET
        );

        service.generate(tempDir, config);

        verify(analyzer).analyze(anyList());
        verify(testAnalyzer).analyzeTests(anyList());
        verify(promptBuilder).build(contains(summary), eq(tempDir.getFileName().toString()), eq(config));
        verify(client).generateReadme(eq(prompt));
        verify(writer).write(eq(tempDir), eq(readme));
    }

    @Test
    void testGenerateInDryRunMode() throws Exception {
        ReadmeGenerationService dryRunService = new ReadmeGenerationService(
                analyzer, client, writer, languageDetector, promptBuilder, testAnalyzer,
                true, false
        );

        Path file = Files.writeString(tempDir.resolve("DryTest.java"), "public class DryTest {}");

        when(languageDetector.detectLanguages(anyList())).thenReturn(Set.of("Java"));
        when(analyzer.analyze(anyList())).thenReturn("summary");
        when(testAnalyzer.analyzeTests(anyList())).thenReturn("");
        when(promptBuilder.build(any(), any(), any())).thenReturn("prompt");
        when(client.generateReadme(any())).thenReturn("readme content");

        ReadmeGenerationConfig config = new ReadmeGenerationConfig(
                ReadmeGenerationConfig.HeaderAlignment.CENTER,
                ReadmeGenerationConfig.ListStyle.NUMBERED
        );

        dryRunService.generate(tempDir, config);

        verify(writer, never()).write(any(), any());
    }

    @Test
    void testShowPromptFlagPrintsPrompt() throws Exception {
        ReadmeGenerationService showPromptService = new ReadmeGenerationService(
                analyzer, client, writer, languageDetector, promptBuilder, testAnalyzer,
                false, true
        );

        Path file = Files.writeString(tempDir.resolve("PromptTest.java"), "public class PromptTest {}");

        when(languageDetector.detectLanguages(anyList())).thenReturn(Set.of("Java"));
        when(analyzer.analyze(anyList())).thenReturn("Summary");
        when(testAnalyzer.analyzeTests(anyList())).thenReturn("");
        when(promptBuilder.build(any(), any(), any())).thenReturn("PROMPT_TO_SHOW");
        when(client.generateReadme(any())).thenReturn("README CONTENT");

        ReadmeGenerationConfig config = new ReadmeGenerationConfig(
                ReadmeGenerationConfig.HeaderAlignment.LEFT,
                ReadmeGenerationConfig.ListStyle.BULLET
        );

        showPromptService.generate(tempDir, config);

        verify(promptBuilder).build(any(), any(), any());
        verify(client).generateReadme(any());
        verify(writer).write(eq(tempDir), eq("README CONTENT"));
    }

    @Test
    void testEmptyProjectDirectory() throws Exception {
        when(languageDetector.detectLanguages(anyList())).thenReturn(Set.of());

        ReadmeGenerationConfig config = new ReadmeGenerationConfig(
                ReadmeGenerationConfig.HeaderAlignment.LEFT,
                ReadmeGenerationConfig.ListStyle.BULLET
        );

        service.generate(tempDir, config);

        verify(writer, never()).write(any(), any());
    }

    @Test
    void testUnrecognizedLanguageSkipsFiles() throws Exception {
        Files.writeString(tempDir.resolve("unknown.lang"), "???");
        when(languageDetector.detectLanguages(anyList())).thenReturn(Set.of());

        ReadmeGenerationConfig config = new ReadmeGenerationConfig(
                ReadmeGenerationConfig.HeaderAlignment.CENTER,
                ReadmeGenerationConfig.ListStyle.NUMBERED
        );

        service.generate(tempDir, config);

        verify(writer, never()).write(any(), any());
    }

    @Test
    void testNoTestsSummaryIsSkipped() throws Exception {
        Path file = Files.writeString(tempDir.resolve("App.java"), "public class App {}");

        when(languageDetector.detectLanguages(anyList())).thenReturn(Set.of("Java"));
        when(analyzer.analyze(anyList())).thenReturn("Core summary");
        when(testAnalyzer.analyzeTests(anyList())).thenReturn("   ");
        when(promptBuilder.build(any(), any(), any())).thenReturn("Prompt");
        when(client.generateReadme(any())).thenReturn("README");

        ReadmeGenerationConfig config = new ReadmeGenerationConfig(
                ReadmeGenerationConfig.HeaderAlignment.LEFT,
                ReadmeGenerationConfig.ListStyle.BULLET
        );

        service.generate(tempDir, config);

        verify(promptBuilder).build(contains("Core summary"), any(), any());
    }

    @Test
    void testLLMClientThrowsException() throws Exception {
        Path file = Files.writeString(tempDir.resolve("App.java"), "public class App {}");

        when(languageDetector.detectLanguages(anyList())).thenReturn(Set.of("Java"));
        when(analyzer.analyze(anyList())).thenReturn("Summary");
        when(testAnalyzer.analyzeTests(anyList())).thenReturn("");
        when(promptBuilder.build(any(), any(), any())).thenReturn("Prompt");
        when(client.generateReadme(any())).thenThrow(new RuntimeException("LLM error"));

        ReadmeGenerationConfig config = new ReadmeGenerationConfig(
                ReadmeGenerationConfig.HeaderAlignment.LEFT,
                ReadmeGenerationConfig.ListStyle.BULLET
        );

        assertThrows(RuntimeException.class, () -> service.generate(tempDir, config));

        verify(writer, never()).write(any(), any());
    }

    @Test
    void testIgnoredFilesAreSkipped() throws Exception {
        Path nodeModules = Files.createDirectories(tempDir.resolve("node_modules"));
        Files.writeString(nodeModules.resolve("ignored.js"), "// ignored");

        Files.writeString(tempDir.resolve("Main.java"), "public class Main {}");

        when(languageDetector.detectLanguages(anyList())).thenReturn(Set.of("Java"));
        when(analyzer.analyze(anyList())).thenReturn("Summary");
        when(testAnalyzer.analyzeTests(anyList())).thenReturn("");
        when(promptBuilder.build(any(), any(), any())).thenReturn("Prompt");
        when(client.generateReadme(any())).thenReturn("README");

        ReadmeGenerationConfig config = new ReadmeGenerationConfig(
                ReadmeGenerationConfig.HeaderAlignment.LEFT,
                ReadmeGenerationConfig.ListStyle.BULLET
        );

        service.generate(tempDir, config);

        verify(analyzer).analyze(argThat(list -> list.stream().noneMatch(p -> p.toString().contains("node_modules"))));
    }
}