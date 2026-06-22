package server.dtos;

import server.enums.HTTPStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable Data Transfer Object representing an outgoing HTTP response.
 * Implements a custom constructor for header generation and alias getters for compatibility.
 */
public record HTTPResponse(
    HTTPStatus status, 
    Map<String, String> headers, 
    byte[] body
) {

    public HTTPResponse(HTTPStatus status, String contentType, byte[] body) {
        this(status, buildHeaders(contentType, body), body);
    }

    private static Map<String, String> buildHeaders(String contentType, byte[] body) {
        Map<String, String> map = new LinkedHashMap<>();
        String charset = (contentType.startsWith("text/") || contentType.startsWith("application/json")) ? "; charset=UTF-8" : "";
        
        map.put("Content-Type", contentType + charset);
        map.put("Content-Length", String.valueOf(body.length));
        
        return map;
    }

    // Legacy Alias Getters
    public HTTPStatus getStatus() { return status(); }
    public int getStatusCode() { return this.status.code(); }
    public Map<String, String> getHeaders() { return headers(); }
    public byte[] getBody() { return body(); }
}