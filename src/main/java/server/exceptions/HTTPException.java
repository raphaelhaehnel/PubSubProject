package server.exceptions;

import server.enums.HTTPStatus;

/**
 * Runtime exception carrying an {@link HTTPStatus} so servlets can signal an HTTP error
 * (e.g. {@code 400 Bad Request}, {@code 404 Not Found}) by throwing it. The server's request
 * loop catches it and turns the status and message into a JSON error response.
 */
public class HTTPException extends RuntimeException {
    private final HTTPStatus status;

    /**
     * Creates an exception for the given status, message, and underlying cause.
     *
     * @param status  the HTTP status to report to the client
     * @param message the human-readable error message
     * @param cause   the underlying cause of this error
     */
    public HTTPException(HTTPStatus status, String message, Throwable cause) {
        super(message);
        this.status = status;
    }

    /**
     * Creates an exception for the given status and message.
     *
     * @param status  the HTTP status to report to the client
     * @param message the human-readable error message
     */
    public HTTPException(HTTPStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** @return the HTTP status associated with this error */
    public HTTPStatus getStatus() {
        return status;
    }

    /** @return the numeric HTTP status code associated with this error */
    public int getStatusCode() {
        return status.code();
    }

}