package com.example.readmegenerator.domain.port;

import java.io.IOException;
import java.nio.file.Path;

public interface ReadmeWriterPort {
    void write(Path projectDir, String content) throws IOException;
}

