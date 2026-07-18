package servlets;

import java.util.HashMap;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import server.dtos.HTTPRequest;
import server.dtos.HTTPResponse;
import server.enums.HTTPStatus;
import server.exceptions.HTTPException;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link ConfLoader} servlet ({@code POST /upload}): a parameterized suite of
 * invalid payloads that must all yield {@code 400 Bad Request}, plus a happy-path test that a
 * valid configuration yields {@code 200 OK}.
 */
@DisplayName("ConfLoader upload servlet")
class ConfLoaderTest {

    /**
     * THE DATA SOURCE: This acts just like pytest.mark.parametrize.
     * It provides the payload and a description for the test logs.
     */
    private static Stream<Arguments> provideBadPayloads() {
        return Stream.of(
            // 1. Empty Input
            Arguments.of("", "Empty input"),
            
            // 2. Invalid JSON Syntax
            Arguments.of("{\"agents\": [{", "Missing closing braces"),
            Arguments.of("{agents: []}", "Illegal key without quotes"),
            Arguments.of("{\"agents\": [{\"name\": \"A1\",}]}", "Trailing comma in array"),
            
            // 3. Valid JSON, missing "agents" array completely
            Arguments.of("{}", "Valid empty JSON"),
            Arguments.of("{\"nodes\": []}", "Missing 'agents' root key"),

            // 4. Invalid Agent structures (Missing mandatory fields)
            Arguments.of("{\"agents\": [{\"pubs\": [\"Topic1\"]}]}", "Agent missing 'type'"),
            Arguments.of("{\"agents\": [{\"type\": \"A1\"}]}", "Agent missing 'pubs' and 'subs' arrays"),
            
            // 6. Graph with Cycles 
            // Agent A publishes T1 and subscribes T2. Agent B subscribes T1 and publishes T2.
            Arguments.of("{\"agents\": [" +
                    "{\"type\": \"IncAgent\", \"subs\": [\"LoopTopic1\"], \"pubs\": [\"LoopTopic2\"]}, " +
                    "{\"type\": \"IncAgent\", \"subs\": [\"LoopTopic2\"], \"pubs\": [\"LoopTopic1\"]}" +
                    "]}", "Cyclic graph detected")
        );
    }

    /**
     * THE NEGATIVE TEST RUNNER
     * The `name` parameter formats the test output so you know exactly which case failed.
     */
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("provideBadPayloads")
    @DisplayName("Invalid configurations are rejected with 400")
    void testUploadInvalidConfigurationsThrowsBadRequest(String payload, String description) throws Exception {
        // Arrange
        HTTPRequest mockRequest = Mockito.mock(HTTPRequest.class);
        when(mockRequest.getHttpCommand()).thenReturn("POST");
        
        Map<String, String> mockParams = new HashMap<>();
        mockParams.put("config", URLEncoder.encode(payload, StandardCharsets.UTF_8)); // Use 'validPayload' in the Happy Path
        when(mockRequest.getParameters()).thenReturn(mockParams);

        ConfLoader confLoader = new ConfLoader();

        // Act & Assert
        HTTPException exception = assertThrows(HTTPException.class, () -> {
            confLoader.handle(mockRequest);
        }, "Expected exception was not thrown for: " + description);

        // Verify it specifically throws a 400 Bad Request
        assertEquals(HTTPStatus.BAD_REQUEST, exception.getStatus(), 
            "Wrong status code for: " + description);
    }

    /**
     * THE HAPPY PATH TEST
     * Verifies that a perfectly valid JSON configuration returns a 200 OK.
     */
    @Test
    @DisplayName("A valid configuration returns 200 OK")
    void testUploadValidConfigurationReturns200() throws Exception {
        // Arrange
        // FIX: Added the "type" field to both agents so Jackson validates them properly
        String validPayload = "{\"agents\": [" +
                "{\"type\": \"MulAgent\", \"pubs\": [\"NewsTopic\"], \"subs\": []}, " +
                "{\"type\": \"IncAgent\", \"pubs\": [], \"subs\": [\"NewsTopic\"]}" +
                "]}";

        HTTPRequest mockRequest = Mockito.mock(HTTPRequest.class);
        when(mockRequest.getHttpCommand()).thenReturn("POST");
        
        Map<String, String> mockParams = new HashMap<>();
        mockParams.put("config", java.net.URLEncoder.encode(validPayload, java.nio.charset.StandardCharsets.UTF_8)); 
        when(mockRequest.getParameters()).thenReturn(mockParams);

        ConfLoader confLoader = new ConfLoader();

        // Act
        HTTPResponse response = confLoader.handle(mockRequest);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertEquals(HTTPStatus.OK, response.getStatus(), 
            "Valid configuration should return 200 OK");
    }
}