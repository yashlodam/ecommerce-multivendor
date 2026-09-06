package com.zosh.service.ai;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zosh.service.ai.llm.GroqService;

class GroqServiceTest {

    private GroqService groqService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        groqService = new GroqService(objectMapper, "test-api-key", "llama-3.3-70b-versatile");
    }

    @Test
    @DisplayName("isAvailable returns true when API key is provided")
    void testIsAvailableWithKey() {
        assertTrue(groqService.isAvailable());
    }

    @Test
    @DisplayName("isAvailable returns false when API key is empty or null")
    void testIsAvailableWithoutKey() {
        GroqService emptyService = new GroqService(objectMapper, "", "llama-3.3-70b-versatile");
        assertFalse(emptyService.isAvailable());

        GroqService nullService = new GroqService(objectMapper, null, "llama-3.3-70b-versatile");
        assertFalse(nullService.isAvailable());
    }

    @Test
    @DisplayName("formatToolsForGroq correctly formats domain tools into OpenAI/Groq function format")
    void testFormatToolsForGroq() {
        List<Map<String, Object>> domainTools = List.of(
                Map.of(
                        "name", "search_products",
                        "description", "Search products by keyword",
                        "parameters", Map.of(
                                "type", "OBJECT",
                                "properties", Map.of(
                                        "query", Map.of("type", "STRING", "description", "Keyword"),
                                        "maxPrice", Map.of("type", "INTEGER", "description", "Max budget")
                                ),
                                "required", List.of("query")
                        )
                )
        );

        List<Map<String, Object>> groqTools = groqService.formatToolsForGroq(domainTools);

        assertNotNull(groqTools);
        assertEquals(1, groqTools.size());

        Map<String, Object> tool = groqTools.get(0);
        assertEquals("function", tool.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> function = (Map<String, Object>) tool.get("function");
        assertEquals("search_products", function.get("name"));
        assertEquals("Search products by keyword", function.get("description"));

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) function.get("parameters");
        assertEquals("object", params.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) params.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> queryProp = (Map<String, Object>) props.get("query");
        assertEquals("string", queryProp.get("type")); // Converted to lowercase

        @SuppressWarnings("unchecked")
        Map<String, Object> priceProp = (Map<String, Object>) props.get("maxPrice");
        assertEquals("integer", priceProp.get("type")); // Converted to lowercase
    }

    @Test
    @DisplayName("Message construction helpers generate valid OpenAI/Groq messages")
    void testMessageConstruction() {
        Map<String, Object> sys = groqService.buildSystemMessage("You are a helper");
        assertEquals("system", sys.get("role"));
        assertEquals("You are a helper", sys.get("content"));

        Map<String, Object> user = groqService.buildUserMessage("Show shirts");
        assertEquals("user", user.get("role"));
        assertEquals("Show shirts", user.get("content"));

        Map<String, Object> assistant = groqService.buildAssistantMessage("Here are shirts");
        assertEquals("assistant", assistant.get("role"));
        assertEquals("Here are shirts", assistant.get("content"));

        Map<String, Object> toolResult = groqService.buildToolResultMessage("call_1", "search_products", Map.of("count", 2));
        assertEquals("tool", toolResult.get("role"));
        assertEquals("call_1", toolResult.get("tool_call_id"));
        assertEquals("search_products", toolResult.get("name"));
        assertTrue(toolResult.get("content").toString().contains("\"count\":2"));
    }

    @Test
    @DisplayName("extractTextFromResponse correctly parses choices[0].message.content")
    void testExtractTextFromResponse() {
        Map<String, Object> response = Map.of(
                "choices", List.of(
                        Map.of("message", Map.of("content", "Hello from Groq!"))
                )
        );

        String text = groqService.extractTextFromResponse(response);
        assertEquals("Hello from Groq!", text);
    }

    @Test
    @DisplayName("extractToolCalls correctly extracts tool_calls from choice message")
    void testExtractToolCalls() {
        Map<String, Object> toolCall = Map.of(
                "id", "call_abc123",
                "type", "function",
                "function", Map.of(
                        "name", "search_products",
                        "arguments", "{\"query\":\"formal shirt\"}"
                )
        );

        Map<String, Object> response = Map.of(
                "choices", List.of(
                        Map.of("message", Map.of("tool_calls", List.of(toolCall)))
                )
        );

        List<Map<String, Object>> toolCalls = groqService.extractToolCalls(response);
        assertNotNull(toolCalls);
        assertEquals(1, toolCalls.size());

        Map<String, Object> parsedArgs = groqService.parseToolArguments(toolCalls.get(0));
        assertEquals("formal shirt", parsedArgs.get("query"));
    }
}
