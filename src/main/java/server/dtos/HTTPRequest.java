package server.dtos;

import java.util.Map;

/**
 * Immutable Data Transfer Object representing an incoming HTTP request.
 * Also exposes JavaBean-style alias getters for compatibility.
 *
 * @param httpCommand the HTTP method (e.g. {@code "GET"}, {@code "POST"})
 * @param uri         the full request URI including any query string
 * @param resourceUri the URI without its query string (e.g. {@code "/publish"})
 * @param uriSegments the non-empty path segments (e.g. {@code ["app", "api.js"]})
 * @param parameters  the merged query-string and form-body parameters
 * @param content     the raw request body bytes (empty if none)
 */
public record HTTPRequest(
    String httpCommand,
    String uri,
    String resourceUri,
    String[] uriSegments,
    Map<String, String> parameters,
    byte[] content
) {
    /** @return the HTTP method */
    public String getHttpCommand() { return httpCommand(); }

    /** @return the full request URI including any query string */
    public String getUri() { return uri(); }

    /** @return the URI without its query string */
    public String getResourceUri() { return resourceUri(); }

    /** @return the non-empty path segments */
    public String[] getUriSegments() { return uriSegments(); }

    /** @return the merged query-string and form-body parameters */
    public Map<String, String> getParameters() { return parameters(); }

    /** @return the raw request body bytes */
    public byte[] getContent() { return content(); }
}