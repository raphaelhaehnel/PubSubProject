package server.dtos;

import server.enums.HTTPStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable Data Transfer Object representing an outgoing HTTP response.
 * Offers a convenience constructor that derives the headers, plus JavaBean-style alias getters.
 *
 * @param status  the HTTP status
 * @param headers the response headers
 * @param body    the response body bytes
 */
public record HTTPResponse(
    HTTPStatus status,
    Map<String, String> headers,
    byte[] body
) {

    /**
     * Convenience constructor that builds {@code Content-Type} (adding a UTF-8 charset for text
     * and JSON) and {@code Content-Length} headers automatically.
     *
     * @param status      the HTTP status
     * @param contentType the response MIME type
     * @param body        the response body bytes
     */
    public HTTPResponse(HTTPStatus status, String contentType, byte[] body) {
        this(status, buildHeaders(contentType, body), body);
    }

    /**
     * Builds the {@code Content-Type} and {@code Content-Length} headers for a response.
     *
     * @param contentType the response MIME type
     * @param body        the response body bytes
     * @return the assembled header map
     */
    private static Map<String, String> buildHeaders(String contentType, byte[] body) {
        Map<String, String> map = new LinkedHashMap<>();
        String charset = (contentType.startsWith("text/") || contentType.startsWith("application/json")) ? "; charset=UTF-8" : "";
        
        map.put("Content-Type", contentType + charset);
        map.put("Content-Length", String.valueOf(body.length));
        
        return map;
    }

    /** @return the HTTP status */
    public HTTPStatus getStatus() { return status(); }

    /** @return the numeric HTTP status code */
    public int getStatusCode() { return this.status.code(); }

    /** @return the response headers */
    public Map<String, String> getHeaders() { return headers(); }

    /** @return the response body bytes */
    public byte[] getBody() { return body(); }
}