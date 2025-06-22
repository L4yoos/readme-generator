package com.example.readmegenerator.adapter.file;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemReadmeWriterTest {

    private FileSystemReadmeWriter writer;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        writer = new FileSystemReadmeWriter();
        tempDir = Files.createTempDirectory("readme-writer-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void shouldWriteReadmeFileToGivenDirectory() throws IOException {
        String content = "# Test README";
        writer.write(tempDir, content);

        Path readmePath = tempDir.resolve("README.md");
        assertTrue(Files.exists(readmePath), "README.md should exist");
        assertEquals(content, Files.readString(readmePath));
    }

    @Test
    void shouldOverwriteExistingReadmeFile() throws IOException {
        Path readmePath = tempDir.resolve("README.md");
        Files.writeString(readmePath, "Old content");

        String newContent = "# New README";
        writer.write(tempDir, newContent);

        assertEquals(newContent, Files.readString(readmePath));
    }

    @Test
    void shouldThrowIOExceptionIfDirectoryIsNotWritable() {
        Path readOnlyDir = tempDir.resolve("readonly");
        try {
            Files.createDirectory(readOnlyDir);
            readOnlyDir.toFile().setWritable(false);

            IOException exception = assertThrows(IOException.class, () -> {
                writer.write(readOnlyDir, "# Fail");
            });

            assertNotNull(exception.getMessage());
        } catch (IOException e) {
            fail("Unexpected setup failure: " + e.getMessage());
        } finally {
            // Reset permissions to allow cleanup
            readOnlyDir.toFile().setWritable(true);
        }
    }
}
