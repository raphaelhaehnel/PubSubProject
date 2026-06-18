package servlets;

import server.dtos.HTTPRequest;
import server.dtos.HTTPResponse;
import server.enums.ContentType;
import server.enums.HTTPStatus;
import server.exceptions.HTTPException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * GET /app/... : serves static files from the configured base directory.
 * A bare "/app" request is mapped to "index.html".
 */
public class StaticResourceServlet extends BaseServlet {

    private final String baseDir;

    public StaticResourceServlet(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public HTTPResponse handle(HTTPRequest request) throws HTTPException {
        String uri = request.getResourceUri(); 
        String fileName = uri.replaceFirst("^/app/?", "");

        if (fileName.isEmpty() || fileName.equals("/")) {
            fileName = "index.html";
        }

        Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();
        Path requestedPath = basePath.resolve(fileName).normalize();

        // Ensure the requested file strictly resides within the allowed base directory
        if (!requestedPath.startsWith(basePath)) {
            throw new HTTPException(HTTPStatus.FORBIDDEN, "Access denied: Invalid path");
        }

        // Check if file exists and is not a directory
        if (!Files.exists(requestedPath) || !Files.isRegularFile(requestedPath)) {
            throw new HTTPException(HTTPStatus.NOT_FOUND, "File not found");
        }

        try {
            byte[] content = Files.readAllBytes(requestedPath);
            String contentType = getContentType(fileName);
            return new HTTPResponse(HTTPStatus.OK, contentType, content);
            
        } catch (IOException e) {
            throw new HTTPException(HTTPStatus.INTERNAL_SERVER_ERROR, "Failed to read file: " + fileName, e);
        }
    }

    /** Picks a MIME type by file extension. Defaults to JSON. */
    private String getContentType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ContentType.JSON.mimeType();
        }

        String extension = fileName.substring(fileName.lastIndexOf("."));
        return ContentType.getMimeTypeForExtension(extension);
    }

    @Override
    public void close() {}
}