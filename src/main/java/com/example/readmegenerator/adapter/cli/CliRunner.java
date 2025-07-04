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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class CliRunner {
    private static final Logger logger = LoggerFactory.getLogger(CliRunner.class);

    public static void main(String[] args) throws Exception {
        int exitCode = run(args, new FileProjectAnalyzer(), new GroqLLMClient(), new FileSystemReadmeWriter(),
                new DefaultLanguageDetector(), new DefaultPromptBuilder(), new FileTestAnalyzer());
        System.exit(exitCode);
    }

    static int run(String[] args, ProjectAnalyzerPort analyzer, LLMClientPort client, ReadmeWriterPort writer,
                   LanguageDetectorPort languageDetector, PromptBuilderPort promptBuilder, TestAnalyzerPort testAnalyzer)
            throws Exception {
        if (args.length == 0) {
            logger.error("Use: java -jar readmegenerator.jar /path/to/project");
            return 1;
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
                    logger.error("❌ Invalid value for --header-align. Allowed: LEFT, CENTRE, RIGHT");
                    return 1;
                }
            } else if (arg.startsWith("--list-style=")) {
                String value = arg.split("=")[1].toUpperCase();
                try {
                    listStyle = ReadmeGenerationConfig.ListStyle.valueOf(value);
                } catch (IllegalArgumentException e) {
                    logger.error("❌ Invalid value for --list-style. Allowed: BULLET, NUMBERED");
                    return 1;
                }
            }
        }

        ReadmeGenerationConfig config = new ReadmeGenerationConfig(alignment, listStyle);

        ReadmeGenerationService service = new ReadmeGenerationService(analyzer, client, writer, languageDetector,
                promptBuilder, testAnalyzer, dryRun, showPrompt);
        service.generate(projectDir, config);

        logger.info("✅ README.md wygenerowany!");
        return 0;
    }
}