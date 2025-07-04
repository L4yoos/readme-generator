package com.example.readmegenerator.adapter.llm;

import com.example.readmegenerator.domain.port.LLMClientPort;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.util.Properties;

public class GroqLLMClient implements LLMClientPort {

    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final double temperature;

    public GroqLLMClient() {
        this(GroqLLMClient.class.getClassLoader().getResourceAsStream("config.properties"));
    }

    public GroqLLMClient(InputStream configStream) {
        Properties props = new Properties();
        try (InputStream input = configStream) {
            if (input == null) {
                throw new RuntimeException("The config.properties file was not found in the resource.");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error while loading config: " + e.getMessage(), e);
        }

        this.apiUrl = props.getProperty("groq.api.url");
        this.apiKey = props.getProperty("groq.api.key");
        this.model = props.getProperty("groq.api.model");
        this.temperature = Double.parseDouble(props.getProperty("groq.api.temperature", "0.7"));
    }

    @Override
    public String generateReadme(String prompt) throws IOException, InterruptedException {
        JSONObject json = new JSONObject();
        json.put("model", model);

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.put(message);

        json.put("messages", messages);
        json.put("temperature", temperature);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject responseJson = new JSONObject(response.body());
        return responseJson
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }
}

