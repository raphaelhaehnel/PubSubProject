package servlets;

import graph.TopicManagerSingleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.dtos.HTTPRequest;
import server.enums.HTTPStatus;
import server.exceptions.HTTPException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ConfLoaderTest {

    private ConfLoader confLoader;

    @BeforeEach
    void setUp() {
        confLoader = new ConfLoader();
        TopicManagerSingleton.get().clear();
    }

    @Test
    void testHandleReturns400OnInvalidJson() {
        HTTPRequest mockRequest = Mockito.mock(HTTPRequest.class);
        Map<String, String> params = new HashMap<>();
        
        // Simulating user inputting garbage data instead of valid JSON
        params.put("config", "{ invalid_json ]");
        when(mockRequest.getParameters()).thenReturn(params);

        HTTPException exception = assertThrows(HTTPException.class, () -> confLoader.handle(mockRequest));
        
        assertEquals(HTTPStatus.BAD_REQUEST, exception.getStatus(), "Server should return 400 for bad JSON syntax.");
        assertTrue(exception.getMessage().contains("Invalid JSON format"), "Exception should mention invalid JSON format.");
    }

    @Test
    void testHandleReturns400AndRollsBackOnCyclicGraph() {
        HTTPRequest mockRequest = Mockito.mock(HTTPRequest.class);
        Map<String, String> params = new HashMap<>();
        
        // A perfectly valid JSON syntax, but logically contains a cycle (C -> A and A -> C)
        String cyclicJson = """
        {
            "agents": [
                {
                    "type": "PlusAgent",
                    "subs": ["A", "B"],
                    "pubs": ["C"]
                },
                {
                    "type": "IncAgent",
                    "subs": ["C"],
                    "pubs": ["A"]
                }
            ]
        }
        """;
        
        params.put("config", cyclicJson);
        when(mockRequest.getParameters()).thenReturn(params);

        HTTPException exception = assertThrows(HTTPException.class, () -> confLoader.handle(mockRequest));
        
        assertEquals(HTTPStatus.BAD_REQUEST, exception.getStatus(), "Server should reject cyclic graphs with a 400 status.");
        assertTrue(exception.getMessage().toLowerCase().contains("cycles"), "Exception message should indicate a cycle was found.");
    }
}