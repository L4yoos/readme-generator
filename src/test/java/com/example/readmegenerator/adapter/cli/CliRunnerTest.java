package com.example.readmegenerator.adapter.cli;

import com.example.readmegenerator.domain.port.*;

import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CliRunnerTest {

    private ProjectAnalyzerPort analyzer;
    private LLMClientPort client;
    private ReadmeWriterPort writer;
    private LanguageDetectorPort languageDetector;
    private PromptBuilderPort promptBuilder;
    private TestAnalyzerPort testAnalyzer;

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        analyzer = mock(ProjectAnalyzerPort.class);
        client = mock(LLMClientPort.class);
        writer = mock(ReadmeWriterPort.class);
        languageDetector = mock(LanguageDetectorPort.class);
        promptBuilder = mock(PromptBuilderPort.class);
        testAnalyzer = mock(TestAnalyzerPort.class);

        tempDir = Files.createTempDirectory("cli-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void shouldRunSuccessfullyWithValidArguments() throws Exception {
        Files.writeString(tempDir.resolve("Main.java"), "public class Main {}");

        when(languageDetector.detectLanguages(anyList())).thenReturn(Set.of("Java"));
        when(analyzer.analyze(anyList())).thenReturn("Summary");
        when(testAnalyzer.analyzeTests(anyList())).thenReturn("Tests summary");
        when(promptBuilder.build(any(), any(), any())).thenReturn("Prompt");
        when(client.generateReadme(any())).thenReturn("README");

        String[] args = { tempDir.toString() };

        int exitCode = CliRunner.run(args, analyzer, client, writer, languageDetector, promptBuilder, testAnalyzer);

        assertEquals(0, exitCode);
        verify(writer).write(eq(tempDir), eq("README"));
    }

    @Test
    void shouldHandleDryRunAndSkipWriting() throws Exception {
        Files.writeString(tempDir.resolve("App.java"), "public class App {}");

        when(languageDetector.detectLanguages(anyList())).thenReturn(Set.of("Java"));
        when(analyzer.analyze(anyList())).thenReturn("Summary");
        when(testAnalyzer.analyzeTests(anyList())).thenReturn("TestSummary");
        when(promptBuilder.build(any(), any(), any())).thenReturn("Prompt");
        when(client.generateReadme(any())).thenReturn("README");

        String[] args = { tempDir.toString(), "--dry-run" };

        int exitCode = CliRunner.run(args, analyzer, client, writer, languageDetector, promptBuilder, testAnalyzer);

        assertEquals(0, exitCode);
        verify(writer, never()).write(any(), any());
    }

    @Test
    void shouldReturnErrorOnInvalidHeaderAlign() throws Exception {
        String[] args = { tempDir.toString(), "--header-align=DIAGONAL" };

        int exitCode = CliRunner.run(args, analyzer, client, writer, languageDetector, promptBuilder, testAnalyzer);

        assertEquals(1, exitCode);
        verifyNoInteractions(analyzer, client, writer, languageDetector, promptBuilder, testAnalyzer);
    }

    @Test
    void shouldReturnErrorOnInvalidListStyle() throws Exception {
        String[] args = { tempDir.toString(), "--list-style=STARS" };

        int exitCode = CliRunner.run(args, analyzer, client, writer, languageDetector, promptBuilder, testAnalyzer);

        assertEquals(1, exitCode);
        verifyNoInteractions(analyzer, client, writer, languageDetector, promptBuilder, testAnalyzer);
    }

    @Test
    void shouldPrintPromptWhenShowPromptFlagIsSet() throws Exception {
        Files.writeString(tempDir.resolve("ShowPrompt.java"), "public class ShowPrompt {}");

        when(languageDetector.detectLanguages(anyList())).thenReturn(Set.of("Java"));
        when(analyzer.analyze(anyList())).thenReturn("Summary");
        when(testAnalyzer.analyzeTests(anyList())).thenReturn("");
        when(promptBuilder.build(any(), any(), any())).thenReturn("THE_PROMPT");
        when(client.generateReadme(any())).thenReturn("README");

        String[] args = { tempDir.toString(), "--show-prompt" };

        int exitCode = CliRunner.run(args, analyzer, client, writer, languageDetector, promptBuilder, testAnalyzer);

        assertEquals(0, exitCode);
        verify(promptBuilder).build(any(), any(), any());
        verify(writer).write(eq(tempDir), eq("README"));
    }
}
