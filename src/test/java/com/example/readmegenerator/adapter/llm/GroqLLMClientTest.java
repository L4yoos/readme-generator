package com.example.readmegenerator.adapter.llm;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

class GroqLLMClientTest {

    @TempDir
    Path tempDir;

    private Path configPropertiesPath;

    // Use MockedStatic in individual tests to avoid interference and ensure proper closing
    private MockedStatic<HttpClient> httpClientMockedStatic;

    @BeforeEach
    void setUp() throws IOException {
        System.setProperty("user.dir", tempDir.toString());
        configPropertiesPath = tempDir.resolve("target/test-classes/config.properties");
        Files.createDirectories(configPropertiesPath.getParent());
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("user.dir");
        if (httpClientMockedStatic != null) {
            httpClientMockedStatic.close();
            httpClientMockedStatic = null; // Reset for next test
        }
    }

    @Test
    void shouldThrowIfConfigMissing() {
        // Direct call to the constructor that takes an InputStream, passing null.
        // This explicitly simulates the case where the resource stream is null (not found).
        assertThrows(RuntimeException.class, () -> new GroqLLMClient(null));
    }


    @Test
    void shouldThrowOnInvalidUrl() throws IOException, InterruptedException {
        Properties props = new Properties();
        props.setProperty("groq.api.url", "http://localhost:9999/invalid");
        props.setProperty("groq.api.key", "key");
        props.setProperty("groq.api.model", "model");
        props.setProperty("groq.api.temperature", "0.7");

        try (FileOutputStream out = new FileOutputStream(configPropertiesPath.toFile())) {
            props.store(out, null);
        } catch (IOException e) {
            fail("Failed to save config.properties: " + e.getMessage());
        }

        httpClientMockedStatic = mockStatic(HttpClient.class);
        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        httpClientMockedStatic.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);

        Mockito.when(mockHttpClient.send(Mockito.any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new IOException("Simulated network connection error"));

        GroqLLMClient client = new GroqLLMClient();
        assertThrows(IOException.class, () -> client.generateReadme("prompt"));
    }

    @Test
    void shouldLoadConfigSuccessfully() throws IOException {
        Properties props = new Properties();
        props.setProperty("groq.api.url", "https://api.groq.com/openai/v1/chat/completions");
        props.setProperty("groq.api.key", "valid_key");
        props.setProperty("groq.api.model", "llama3-70b-8192");
        props.setProperty("groq.api.temperature", "0.7");

        try (FileOutputStream out = new FileOutputStream(configPropertiesPath.toFile())) {
            props.store(out, null);
        } catch (IOException e) {
            fail("Failed to save config.properties: " + e.getMessage());
        }

        assertDoesNotThrow(() -> new GroqLLMClient());
    }

    @Test
    void shouldGenerateReadmeSuccessfully() throws IOException, InterruptedException {
        Properties props = new Properties();
        props.setProperty("groq.api.url", "https://api.groq.com/openai/v1/chat/completions");
        props.setProperty("groq.api.key", "valid_key");
        props.setProperty("groq.api.model", "llama3-70b-8192");
        props.setProperty("groq.api.temperature", "0.7");
        try (FileOutputStream out = new FileOutputStream(configPropertiesPath.toFile())) {
            props.store(out, null);
        }

        httpClientMockedStatic = mockStatic(HttpClient.class);
        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        HttpResponse<String> mockHttpResponse = Mockito.mock(HttpResponse.class);

        httpClientMockedStatic.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);
        Mockito.when(mockHttpClient.send(Mockito.any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockHttpResponse);

        Mockito.when(mockHttpResponse.statusCode()).thenReturn(200);
        String mockResponseBody = "{\"choices\": [{\"message\": {\"content\": \"This is a generated README.\"}}]}";
        Mockito.when(mockHttpResponse.body()).thenReturn(mockResponseBody);

        GroqLLMClient client = new GroqLLMClient();
        String generatedReadme = client.generateReadme("Test prompt");

        assertEquals("This is a generated README.", generatedReadme);
    }
}