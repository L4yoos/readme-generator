package com.example.readmegenerator.domain.port;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface ProjectAnalyzerPort {
    String analyze(List<Path> files) throws IOException;
}

