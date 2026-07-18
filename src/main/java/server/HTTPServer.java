package server;


import servlets.Servlet;

/**
 * Public API of the HTTP server. {@link MyHTTPServer} is the only
 * implementation; the interface lets the rest of the code be unaware
 * of the implementation details.
 */
public interface HTTPServer extends Runnable {
    /**
     * Registers a servlet to handle requests matching an HTTP method and URI prefix.
     *
     * @param httpCommand the HTTP method (e.g. {@code "GET"}, {@code "POST"})
     * @param uri         the URI prefix the servlet handles (e.g. {@code "/publish"})
     * @param s           the servlet to register
     */
    void addServlet(String httpCommand, String uri, Servlet s);

    /**
     * Removes a previously registered servlet.
     *
     * @param httpCommand the HTTP method the servlet was registered under
     * @param uri         the URI prefix the servlet was registered under
     */
    void removeServlet(String httpCommand, String uri);

    /** Starts the server so it begins accepting connections. */
    void start();

    /** Stops the server and releases its resources (sockets, worker pool, servlets). */
    void close();
}
