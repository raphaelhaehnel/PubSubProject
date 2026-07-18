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

    /**
     * @param baseDir the directory from which static files are served
     */
    public StaticResourceServlet(String baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Serves the requested file from the base directory, mapping a bare {@code /app} to
     * {@code index.html}.
     *
     * @throws HTTPException {@code 403} if the resolved path escapes the base directory
     *         (path-traversal attempt), {@code 404} if the file does not exist, or
     *         {@code 500} if the file cannot be read
     */
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

    /**
     * Picks a MIME type by file extension. Defaults to JSON when there is no extension.
     *
     * @param fileName the requested file name
     * @return the matching MIME type
     */
    private String getContentType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ContentType.JSON.mimeType();
        }

        String extension = fileName.substring(fileName.lastIndexOf("."));
        return ContentType.getMimeTypeForExtension(extension);
    }

    /** {@inheritDoc} */
    @Override
    public void close() {}
}