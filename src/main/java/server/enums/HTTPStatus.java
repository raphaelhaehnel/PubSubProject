package server.enums;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    HTTPStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() { return code; }
    public String message() { return message; }

    public static String getMessageForCode(int statusCode) {
        return STATUS_LOOKUP.getOrDefault(statusCode, "Unknown");
    }
}