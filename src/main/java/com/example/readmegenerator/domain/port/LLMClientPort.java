package com.example.readmegenerator.domain.port;

import java.io.IOException;

public interface LLMClientPort {
    String generateReadme(String prompt) throws IOException, InterruptedException;
}
