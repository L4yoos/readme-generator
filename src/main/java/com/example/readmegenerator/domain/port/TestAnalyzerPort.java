package com.example.readmegenerator.domain.port;

import java.nio.file.Path;
import java.util.List;

public interface TestAnalyzerPort {
    String analyzeTests(List<Path> allFiles);
}
