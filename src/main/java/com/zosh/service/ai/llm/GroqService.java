package com.zosh.service.ai.llm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Groq LLM Service using OpenAI-compatible /chat/completions API.
 * High-speed inference for Llama 3.3 70B Versatile and other open models.
 */
@Service
public class GroqService {

    private static final Logger log = LoggerFactory.getLogger(GroqService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.groq.api-key:}")
    private String apiKey;

    @Value("${app.ai.groq.base-url:https://api.groq.com/openai/v1}")
    private String baseUrl;

    @Value("${app.ai.groq.model:openai/gpt-oss-120b}")
    private String model;

    @Value("${app.ai.groq.max-tokens:1024}")
    private int maxTokens;

    @Value("${app.ai.groq.temperature:0.5}")
    private double temperature;

    @org.springframework.beans.factory.annotation.Autowired
    public GroqService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    // Constructor for testing
    public GroqService(ObjectMapper objectMapper, String apiKey, String model) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = "https://api.groq.com/openai/v1";
        this.maxTokens = 1024;
        this.temperature = 0.5;
        this.restClient = RestClient.builder().build();
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isBlank();
    }

    public String getModel() {
        return model;
    }

    /**
     * Executes chat completion with Groq API, supporting tools/function calling.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> generateChatCompletion(
            List<Map<String, Object>> messages,
            List<Map<String, Object>> tools) {

        if (!isAvailable()) {
            log.debug("Groq API key not configured, skipping call");
            return null;
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            if (tools != null && !tools.isEmpty()) {
                requestBody.put("tools", tools);
                requestBody.put("tool_choice", "auto");
            }

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            log.debug("Sending chat completion to Groq model: {}", model);

            String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";

            String responseStr = restClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                    .header(HttpHeaders.USER_AGENT, "ShopSphere-AI/1.0")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);

            return objectMapper.readValue(responseStr, new TypeReference<Map<String, Object>>() {});

        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extracts final natural language text content from Groq response.
     */
    @SuppressWarnings("unchecked")
    public String extractTextFromResponse(Map<String, Object> response) {
        try {
            if (response == null || !response.containsKey("choices")) return null;
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return null;

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) return null;

            Object content = message.get("content");
            return content != null ? content.toString() : null;
        } catch (Exception e) {
            log.warn("Error extracting text from Groq response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts tool calls from Groq response if present.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> extractToolCalls(Map<String, Object> response) {
        try {
            if (response == null || !response.containsKey("choices")) return null;
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return null;

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) return null;

            if (message.containsKey("tool_calls")) {
                Object tc = message.get("tool_calls");
                if (tc instanceof List) {
                    return (List<Map<String, Object>>) tc;
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting tool calls from Groq response: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Parses tool call arguments from JSON string to Map.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseToolArguments(Map<String, Object> toolCall) {
        try {
            if (toolCall == null || !toolCall.containsKey("function")) return new HashMap<>();
            Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
            if (function == null || !function.containsKey("arguments")) return new HashMap<>();

            Object argsObj = function.get("arguments");
            if (argsObj instanceof Map) {
                return (Map<String, Object>) argsObj;
            }
            if (argsObj instanceof String) {
                String argsStr = (String) argsObj;
                if (argsStr.trim().isEmpty()) return new HashMap<>();
                return objectMapper.readValue(argsStr, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("Error parsing tool arguments: {}", e.getMessage());
        }
        return new HashMap<>();
    }

    /**
     * Adapts raw domain tool declarations into OpenAI/Groq standard JSON Schema format.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> formatToolsForGroq(List<Map<String, Object>> rawTools) {
        List<Map<String, Object>> groqTools = new ArrayList<>();
        if (rawTools == null) return groqTools;

        for (Map<String, Object> raw : rawTools) {
            Map<String, Object> toolWrapper = new LinkedHashMap<>();
            toolWrapper.put("type", "function");

            Map<String, Object> functionDef = new LinkedHashMap<>();
            functionDef.put("name", raw.get("name"));
            functionDef.put("description", raw.get("description"));

            if (raw.containsKey("parameters")) {
                Map<String, Object> rawParams = (Map<String, Object>) raw.get("parameters");
                Map<String, Object> adaptedParams = new LinkedHashMap<>();
                adaptedParams.put("type", "object");

                if (rawParams.containsKey("properties")) {
                    Map<String, Object> rawProps = (Map<String, Object>) rawParams.get("properties");
                    Map<String, Object> adaptedProps = new LinkedHashMap<>();

                    for (Map.Entry<String, Object> entry : rawProps.entrySet()) {
                        if (entry.getValue() instanceof Map) {
                            Map<String, Object> propMap = new LinkedHashMap<>((Map<String, Object>) entry.getValue());
                            // Convert uppercase types (STRING, INTEGER, etc.) to lowercase
                            if (propMap.containsKey("type")) {
                                String typeVal = propMap.get("type").toString().toLowerCase();
                                propMap.put("type", typeVal);
                            }
                            adaptedProps.put(entry.getKey(), propMap);
                        }
                    }
                    adaptedParams.put("properties", adaptedProps);
                }

                if (rawParams.containsKey("required")) {
                    adaptedParams.put("required", rawParams.get("required"));
                } else {
                    adaptedParams.put("required", List.of());
                }

                functionDef.put("parameters", adaptedParams);
            }

            toolWrapper.put("function", functionDef);
            groqTools.add(toolWrapper);
        }

        return groqTools;
    }

    // ─── Message Construction Helpers ─────────────────────────────────────

    public Map<String, Object> buildSystemMessage(String text) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "system");
        msg.put("content", text != null ? text : "");
        return msg;
    }

    public Map<String, Object> buildUserMessage(String text) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "user");
        msg.put("content", text != null ? text : "");
        return msg;
    }

    public Map<String, Object> buildAssistantMessage(String text) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "assistant");
        msg.put("content", text != null ? text : "");
        return msg;
    }

    public Map<String, Object> buildAssistantToolCallMessage(List<Map<String, Object>> toolCalls) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "assistant");
        msg.put("content", null);
        msg.put("tool_calls", toolCalls);
        return msg;
    }

    public Map<String, Object> buildToolResultMessage(String toolCallId, String toolName, Map<String, Object> result) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "tool");
        msg.put("tool_call_id", toolCallId != null ? toolCallId : "call_" + toolName);
        msg.put("name", toolName);
        try {
            msg.put("content", objectMapper.writeValueAsString(result != null ? result : Map.of("status", "success")));
        } catch (Exception e) {
            msg.put("content", "{\"status\":\"success\"}");
        }
        return msg;
    }
}
