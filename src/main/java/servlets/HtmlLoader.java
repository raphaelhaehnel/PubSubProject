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
public class HtmlLoader extends BaseServlet {

    private final String baseDir;

    public HtmlLoader(String baseDir) {
        this.baseDir = baseDir;
    }

@Override
    public HTTPResponse handle(HTTPRequest request) throws HTTPException {
        String uri = request.getResourceUri(); 
        String fileName = uri.replaceFirst("^/app/?", "");

        if (fileName.isEmpty() || fileName.equals("/")) {
            fileName = "index.html";
        }

        String fullPath = baseDir + "/" + fileName;
        Path path = Paths.get(fullPath);
        
        if (!Files.exists(path)) {
            throw new HTTPException(HTTPStatus.NOT_FOUND, "File not found");
        }

        // --- The Fix: Wrap the dangerous file system call in a try/catch ---
        try {
            byte[] content = Files.readAllBytes(path);
            String contentType = getContentType(fileName);
            return new HTTPResponse(HTTPStatus.OK, contentType, content);
            
        } catch (IOException e) {
            // Catch the low-level Java error and throw your custom HTTP domain error.
            // Passing 'e' as the third parameter chains them together for debugging!
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
