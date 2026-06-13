package server.exceptions;

import server.enums.HTTPStatus;

public class HTTPException extends RuntimeException {
    private final HTTPStatus status; // Now using the Enum!

    public HTTPException(HTTPStatus status, String message, Throwable cause) {
        super(message);
        this.status = status;
    }

    public HTTPException(HTTPStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HTTPStatus getStatus() { 
        return status; 
    }

    public int getStatusCode() {
        return status.code();
    }

}