package com.example.readmegenerator.domain.port;

import com.example.readmegenerator.domain.model.ReadmeGenerationConfig;

public interface PromptBuilderPort {
    String build(String summary, String projectName,  ReadmeGenerationConfig config);
}
