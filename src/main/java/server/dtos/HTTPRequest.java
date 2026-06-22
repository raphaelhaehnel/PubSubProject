package server.dtos;

import java.util.Map;

/**
 * Immutable Data Transfer Object representing an incoming HTTP request.
 * Implements alias getters for legacy JavaBean compatibility.
 */
public record HTTPRequest(
    String httpCommand,
    String uri,
    String resourceUri,
    String[] uriSegments,
    Map<String, String> parameters,
    byte[] content
) {
    // Legacy Alias Getters
    public String getHttpCommand() { return httpCommand(); }
    public String getUri() { return uri(); }
    public String getResourceUri() { return resourceUri(); }
    public String[] getUriSegments() { return uriSegments(); }
    public Map<String, String> getParameters() { return parameters(); }
    public byte[] getContent() { return content(); }
}