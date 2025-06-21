package com.example.readmegenerator.adapter.file;

import com.example.readmegenerator.domain.port.ReadmeWriterPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemReadmeWriter implements ReadmeWriterPort {
    @Override
    public void write(Path projectDir, String content) throws IOException {
        Path readmePath = projectDir.resolve("README.md");
        Files.writeString(readmePath, content);
    }
}

