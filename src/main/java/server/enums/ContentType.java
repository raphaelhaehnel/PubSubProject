package server.enums;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maps common file extensions to their HTTP {@code Content-Type} (MIME) values, used by the
 * static resource servlet to label responses. {@code application/json} is the default fallback.
 */
public enum ContentType {
    HTML(".html", "text/html"),
    CSS(".css", "text/css"),
    JS(".js", "application/javascript"),
    SVG(".svg", "image/svg+xml"),
    JSON(".json", "application/json"),
    PNG(".png", "image/png");

    private final String extension;
    private final String mimeType;

    private static final Map<String, String> EXTENSION_LOOKUP = Stream.of(values())
            .collect(Collectors.toMap(ContentType::extension, ContentType::mimeType));

    /**
     * @param extension the file extension including the leading dot (e.g. {@code ".html"})
     * @param mimeType   the MIME type served for that extension
     */
    ContentType(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    /** @return the file extension for this content type, including the leading dot */
    public String extension() { return extension; }

    /** @return the MIME type string for this content type */
    public String mimeType() { return mimeType; }

    /**
     * Looks up the MIME type for a file extension, case-insensitively.
     *
     * @param extension the file extension including the leading dot, or {@code null}
     * @return the matching MIME type, or {@code application/json} if unknown or {@code null}
     */
    public static String getMimeTypeForExtension(String extension) {
        String defaultMime = ContentType.JSON.mimeType();
        
        if (extension == null) {
            return defaultMime;
        }
        
        return EXTENSION_LOOKUP.getOrDefault(extension.toLowerCase(), defaultMime);
    }
}