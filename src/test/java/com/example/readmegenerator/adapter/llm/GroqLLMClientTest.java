package com.example.readmegenerator.adapter.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class GroqLLMClientTest {

    @BeforeEach
    void setupConfig() throws IOException {
        Properties props = new Properties();
        props.setProperty("groq.api.url", "https://mock.api.url");
        props.setProperty("groq.api.key", "fake-key");
        props.setProperty("groq.api.model", "test-model");
        props.setProperty("groq.api.temperature", "0.5");

        try (FileOutputStream out = new FileOutputStream("target/test-classes/config.properties")) {
            props.store(out, null);
        }
    }

    @Test
    void shouldInitializeFromProperties() {
        GroqLLMClient client = new GroqLLMClient();
        assertNotNull(client);
    }

    @Test
    void shouldThrowIfConfigMissing() {
        InputStream missingStream = null;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            new GroqLLMClient(missingStream);
        });
        assertTrue(exception.getMessage().contains("Nie znaleziono pliku config.properties"));
    }

    @Test
    void shouldThrowOnInvalidUrl() {
        // Konfiguracja z nieistniejącym URL-em
        Properties props = new Properties();
        props.setProperty("groq.api.url", "http://localhost:9999/invalid");
        props.setProperty("groq.api.key", "key");
        props.setProperty("groq.api.model", "model");
        props.setProperty("groq.api.temperature", "0.7");

        try (FileOutputStream out = new FileOutputStream("target/test-classes/config.properties")) {
            props.store(out, null);
        } catch (IOException e) {
            fail("Nie udało się zapisać config.properties");
        }

        GroqLLMClient client = new GroqLLMClient();
        assertThrows(IOException.class, () -> client.generateReadme("prompt"));
    }
}
