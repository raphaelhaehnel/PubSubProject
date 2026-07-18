package server.enums;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The subset of HTTP status codes used by this server, each pairing a numeric code with its
 * standard reason phrase. Used when building {@link server.dtos.HTTPResponse} objects and
 * translating {@link server.exceptions.HTTPException}s into responses.
 */
public enum HTTPStatus {
    OK(200, "OK"),
    BAD_REQUEST(400, "Bad Request"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    private final int code;
    private final String message;

    private static final Map<Integer, String> STATUS_LOOKUP = Stream.of(values())
            .collect(Collectors.toMap(HTTPStatus::code, HTTPStatus::message));

    /**
     * @param code    the numeric HTTP status code
     * @param message the standard reason phrase for that code
     */
    HTTPStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /** @return the numeric HTTP status code (e.g. {@code 200}) */
    public int code() { return code; }

    /** @return the standard reason phrase (e.g. {@code "OK"}) */
    public String message() { return message; }

    /**
     * Looks up the reason phrase for a numeric status code.
     *
     * @param statusCode the numeric HTTP status code
     * @return the matching reason phrase, or {@code "Unknown"} if the code is not defined here
     */
    public static String getMessageForCode(int statusCode) {
        return STATUS_LOOKUP.getOrDefault(statusCode, "Unknown");
    }
}