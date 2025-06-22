package com.example.readmegenerator.adapter.prompt;

import com.example.readmegenerator.domain.model.ReadmeGenerationConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultPromptBuilderTest {

    private final DefaultPromptBuilder promptBuilder = new DefaultPromptBuilder();

    @Test
    void buildsPrompt_withDefaultValues() {
        ReadmeGenerationConfig config = new ReadmeGenerationConfig(
                ReadmeGenerationConfig.HeaderAlignment.LEFT,
                ReadmeGenerationConfig.ListStyle.BULLET
        );

        String summary = "Short project summary";
        String projectName = "MyProject";

        String result = promptBuilder.build(summary, projectName, config);

        assertTrue(result.contains("<h1 align=\"left\">" + projectName + "</h1>"));
        assertTrue(result.contains("<ul align=\"left\">"));
        assertTrue(result.contains(summary));
        assertTrue(result.contains("- Use `<ul>` and `<li>` for all lists"));
    }

    @Test
    void buildsPrompt_withCenteredHeader_andNumberedList() {
        ReadmeGenerationConfig config = new ReadmeGenerationConfig(
                ReadmeGenerationConfig.HeaderAlignment.CENTER,
                ReadmeGenerationConfig.ListStyle.NUMBERED
        );

        String summary = "Another summary";
        String projectName = "CenteredProject";

        String result = promptBuilder.build(summary, projectName, config);

        assertTrue(result.contains("<h1 align=\"center\">" + projectName + "</h1>"));
        assertTrue(result.contains("<ol align=\"center\">"));
        assertTrue(result.contains("- Use `<ol>` and `<li>` for all lists"));
    }

    @Test
    void buildsPrompt_withRightAlignedHeader_andBulletedList() {
        ReadmeGenerationConfig config = new ReadmeGenerationConfig(
                ReadmeGenerationConfig.HeaderAlignment.RIGHT,
                ReadmeGenerationConfig.ListStyle.BULLET
        );

        String summary = "Right aligned summary";
        String projectName = "RightProject";

        String result = promptBuilder.build(summary, projectName, config);

        assertTrue(result.contains("<h1 align=\"right\">" + projectName + "</h1>"));
        assertTrue(result.contains("<ul align=\"right\">"));
        assertTrue(result.contains("- Use `<ul>` and `<li>` for all lists"));
    }

    @Test
    void trimsSummaryIfTooLong() {
        ReadmeGenerationConfig config = new ReadmeGenerationConfig(
                ReadmeGenerationConfig.HeaderAlignment.LEFT,
                ReadmeGenerationConfig.ListStyle.BULLET
        );

        StringBuilder longSummaryBuilder = new StringBuilder();
        for (int i = 0; i < 15000; i++) {
            longSummaryBuilder.append("a");
        }
        String longSummary = longSummaryBuilder.toString();

        String result = promptBuilder.build(longSummary, "LongProject", config);

        assertTrue(result.contains("[...Content trimmed due to size limits...]"));
        assertTrue(result.length() < longSummary.length());
        assertTrue(result.contains("<h1 align=\"left\">LongProject</h1>"));
    }
}
