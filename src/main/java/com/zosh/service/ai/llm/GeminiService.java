package com.zosh.service.ai.llm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.model:gemini-2.5-flash}")
    private String model;

    @Value("${app.ai.max-tokens:1024}")
    private int maxTokens;

    @Value("${app.ai.temperature:0.7}")
    private double temperature;

    public GeminiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generateContent(
            String systemPrompt,
            List<Map<String, Object>> conversationHistory,
            List<Map<String, Object>> toolDeclarations) {

        if (!isAvailable()) {
            return null;
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();

            // System instruction
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                Map<String, Object> sysInstruction = new HashMap<>();
                List<Map<String, Object>> sysParts = new ArrayList<>();
                sysParts.add(Map.of("text", systemPrompt));
                sysInstruction.put("parts", sysParts);
                requestBody.put("systemInstruction", sysInstruction);
            }

            // Conversation contents
            requestBody.put("contents", conversationHistory);

            // Tools (function declarations)
            if (toolDeclarations != null && !toolDeclarations.isEmpty()) {
                List<Map<String, Object>> tools = new ArrayList<>();
                Map<String, Object> toolWrapper = new HashMap<>();
                toolWrapper.put("functionDeclarations", toolDeclarations);
                tools.add(toolWrapper);
                requestBody.put("tools", tools);
            }

            // Generation config
            Map<String, Object> genConfig = new HashMap<>();
            genConfig.put("temperature", temperature);
            genConfig.put("maxOutputTokens", maxTokens);
            requestBody.put("generationConfig", genConfig);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            log.debug("Gemini request to model {}", model);

            String responseStr = restClient.post()
                    .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);

            return objectMapper.readValue(responseStr, new TypeReference<Map<String, Object>>() {});

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) return null;
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return null;
            for (Map<String, Object> part : parts) {
                if (part.containsKey("text")) {
                    return (String) part.get("text");
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting text from Gemini response: {}", e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> extractFunctionCallFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) return null;
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return null;
            for (Map<String, Object> part : parts) {
                if (part.containsKey("functionCall")) {
                    return (Map<String, Object>) part.get("functionCall");
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting function call: {}", e.getMessage());
        }
        return null;
    }

    public Map<String, Object> buildUserMessage(String text) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "user");
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", text));
        msg.put("parts", parts);
        return msg;
    }

    public Map<String, Object> buildModelMessage(String text) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "model");
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", text));
        msg.put("parts", parts);
        return msg;
    }

    public Map<String, Object> buildFunctionCallMessage(String functionName, Map<String, Object> args) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "model");
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> fcPart = new HashMap<>();
        Map<String, Object> fc = new HashMap<>();
        fc.put("name", functionName);
        fc.put("args", args != null ? args : new HashMap<>());
        fcPart.put("functionCall", fc);
        parts.add(fcPart);
        msg.put("parts", parts);
        return msg;
    }

    public Map<String, Object> buildFunctionResponseMessage(String functionName, Map<String, Object> result) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "user");
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> frPart = new HashMap<>();
        Map<String, Object> fr = new HashMap<>();
        fr.put("name", functionName);
        fr.put("response", result != null ? result : Map.of("result", "No data"));
        frPart.put("functionResponse", fr);
        parts.add(frPart);
        msg.put("parts", parts);
        return msg;
    }
}
