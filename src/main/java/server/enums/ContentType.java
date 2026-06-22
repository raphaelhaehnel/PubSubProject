package server.enums;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    ContentType(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    public String extension() { return extension; }
    public String mimeType() { return mimeType; }

    public static String getMimeTypeForExtension(String extension) {
        String defaultMime = ContentType.JSON.mimeType();
        
        if (extension == null) {
            return defaultMime;
        }
        
        return EXTENSION_LOOKUP.getOrDefault(extension.toLowerCase(), defaultMime);
    }
}