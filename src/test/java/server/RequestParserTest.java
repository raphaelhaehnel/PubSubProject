package server;

import org.junit.jupiter.api.Test;
import server.dtos.HTTPRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class RequestParserTest {

    @Test
    void testParseValidGetRequest() throws IOException {
        String rawRequest = "GET /api/data?id=123&name=test HTTP/1.1\r\n" +
                            "Host: localhost:8080\r\n" +
                            "User-Agent: curl/7.68.0\r\n" +
                            "\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(rawRequest));
        HTTPRequest request = RequestParser.parseRequest(reader);

        assertEquals("GET", request.getHttpCommand());
        assertEquals("/api/data?id=123&name=test", request.getUri());
        assertEquals("/api/data", request.getResourceUri());
        assertArrayEquals(new String[]{"api", "data"}, request.getUriSegments(), "URI segments should be split correctly");

        assertEquals("123", request.getParameters().get("id"));
        assertEquals("test", request.getParameters().get("name"));
    }

    @Test
    void testParseValidPostRequestWithBody() throws IOException {
        String body = "key1=value1&key2=value2";
        String rawRequest = "POST /submit HTTP/1.1\r\n" +
                            "Host: localhost:8080\r\n" +
                            "Content-Length: " + body.length() + "\r\n" +
                            "\r\n" +
                            body;

        BufferedReader reader = new BufferedReader(new StringReader(rawRequest));
        HTTPRequest request = RequestParser.parseRequest(reader);

        assertEquals("POST", request.getHttpCommand());
        assertEquals("/submit", request.getResourceUri());

        // Body parameters should be merged seamlessly into the parameters map
        assertEquals("value1", request.getParameters().get("key1"));
        assertEquals("value2", request.getParameters().get("key2"));
        assertArrayEquals(body.getBytes(java.nio.charset.StandardCharsets.UTF_8), request.getContent());
    }

    @Test
    void testParseEmptyRequestThrowsException() {
        String rawRequest = "";
        BufferedReader reader = new BufferedReader(new StringReader(rawRequest));

        IOException exception = assertThrows(IOException.class, () -> RequestParser.parseRequest(reader));
        assertTrue(exception.getMessage().contains("Empty or null request line"));
    }

    @Test
    void testParseInvalidContentLengthThrowsException() {
        String rawRequest = "POST /upload HTTP/1.1\r\n" +
                            "Content-Length: -5\r\n" +
                            "\r\n";
        
        BufferedReader reader = new BufferedReader(new StringReader(rawRequest));

        IOException exception = assertThrows(IOException.class, () -> RequestParser.parseRequest(reader));
        assertTrue(exception.getMessage().contains("Invalid Content-Length"));
    }
}