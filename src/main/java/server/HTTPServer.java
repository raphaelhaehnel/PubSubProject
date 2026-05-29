package server;


import servlets.Servlet;

/**
 * Public API of the HTTP server. {@link MyHTTPServer} is the only
 * implementation; the interface lets the rest of the code be unaware
 * of the implementation details.
 */
public interface HTTPServer extends Runnable {
    void addServlet(String httpCommand, String uri, Servlet s);
    void removeServlet(String httpCommand, String uri);
    void start();
    void close();
}
