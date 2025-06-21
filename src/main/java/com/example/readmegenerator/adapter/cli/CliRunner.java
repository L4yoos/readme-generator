package com.example.readmegenerator.adapter.cli;

import com.example.readmegenerator.adapter.analyzer.FileProjectAnalyzer;
import com.example.readmegenerator.adapter.analyzer.FileTestAnalyzer;
import com.example.readmegenerator.adapter.detector.DefaultLanguageDetector;
import com.example.readmegenerator.adapter.file.FileSystemReadmeWriter;
import com.example.readmegenerator.adapter.llm.GroqLLMClient;
import com.example.readmegenerator.adapter.prompt.DefaultPromptBuilder;
import com.example.readmegenerator.app.ReadmeGenerationService;
import com.example.readmegenerator.domain.model.ReadmeGenerationConfig;
import com.example.readmegenerator.domain.port.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class CliRunner {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Użycie: java -jar readmegenerator.jar /ścieżka/do/projektu");
            System.exit(1);
        }

        Path projectDir = Paths.get(args[0]);
        boolean dryRun = Arrays.asList(args).contains("--dry-run");
        boolean showPrompt = Arrays.asList(args).contains("--show-prompt");

        ReadmeGenerationConfig.HeaderAlignment alignment = ReadmeGenerationConfig.HeaderAlignment.LEFT;
        ReadmeGenerationConfig.ListStyle listStyle = ReadmeGenerationConfig.ListStyle.BULLET;

        for (String arg : args) {
            if (arg.startsWith("--header-align=")) {
                String value = arg.split("=")[1].toUpperCase();
                try {
                    alignment = ReadmeGenerationConfig.HeaderAlignment.valueOf(value);
                } catch (IllegalArgumentException e) {
                    System.err.println("❌ Nieprawidłowa wartość dla --header-align. Dozwolone: LEFT, CENTER, RIGHT");
                    System.exit(1);
                }
            } else if (arg.startsWith("--list-style=")) {
                String value = arg.split("=")[1].toUpperCase();
                try {
                    listStyle = ReadmeGenerationConfig.ListStyle.valueOf(value);
                } catch (IllegalArgumentException e) {
                    System.err.println("❌ Nieprawidłowa wartość dla --list-style. Dozwolone: BULLET, NUMBERED");
                    System.exit(1);
                }
            }
        }

        ReadmeGenerationConfig config = new ReadmeGenerationConfig(alignment, listStyle);

        ProjectAnalyzerPort analyzer = new FileProjectAnalyzer();
        LLMClientPort client = new GroqLLMClient();
        ReadmeWriterPort writer = new FileSystemReadmeWriter();
        LanguageDetectorPort languageDetector = new DefaultLanguageDetector();
        PromptBuilderPort promptBuilder = new DefaultPromptBuilder();
        TestAnalyzerPort testAnalyzer = new FileTestAnalyzer();

        ReadmeGenerationService service = new ReadmeGenerationService(analyzer, client, writer, languageDetector, promptBuilder, testAnalyzer, dryRun, showPrompt);
        service.generate(projectDir, config);

        System.out.println("✅ README.md wygenerowany!");
    }
}

