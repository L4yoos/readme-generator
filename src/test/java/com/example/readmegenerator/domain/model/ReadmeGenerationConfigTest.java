package com.example.readmegenerator.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReadmeGenerationConfigTest {

    @Test
    void shouldReturnCorrectHeaderAlignment() {
        ReadmeGenerationConfig config = new ReadmeGenerationConfig(
                ReadmeGenerationConfig.HeaderAlignment.CENTER,
                ReadmeGenerationConfig.ListStyle.BULLET
        );

        assertEquals(ReadmeGenerationConfig.HeaderAlignment.CENTER, config.getHeaderAlignment());
    }

    @Test
    void shouldReturnCorrectListStyle() {
        ReadmeGenerationConfig config = new ReadmeGenerationConfig(
                ReadmeGenerationConfig.HeaderAlignment.LEFT,
                ReadmeGenerationConfig.ListStyle.NUMBERED
        );

        assertEquals(ReadmeGenerationConfig.ListStyle.NUMBERED, config.getListStyle());
    }

    @Test
    void enumsShouldContainExpectedValues() {
        assertTrue(Enum.valueOf(ReadmeGenerationConfig.HeaderAlignment.class, "LEFT") instanceof ReadmeGenerationConfig.HeaderAlignment);
        assertTrue(Enum.valueOf(ReadmeGenerationConfig.ListStyle.class, "BULLET") instanceof ReadmeGenerationConfig.ListStyle);
    }
}
