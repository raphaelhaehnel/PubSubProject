package server;

import servlets.Servlet;
import server.dtos.HTTPRequest;
import server.dtos.HTTPResponse;
import server.enums.HTTPStatus;
import server.exceptions.HTTPException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Minimal multithreaded HTTP server built on top of plain Sockets.
 * Listens on a port, accepts connections on the main thread, and hands
 * each one to a fixed-size worker pool.
 * One servlet map per supported HTTP method (GET, POST, DELETE).
 */
public class MyHTTPServer extends Thread implements HTTPServer {

    private static final Logger logger = Logger.getLogger(MyHTTPServer.class.getName());

    private final int port;
    private final int nThreads;

    private final ConcurrentHashMap<String, Servlet> getToServletMap;
    private final ConcurrentHashMap<String, Servlet> postToServletMap;
    private final ConcurrentHashMap<String, Servlet> deleteToServletMap;

    private volatile boolean running;
    private ServerSocket serverSocket;
    private ExecutorService executor;


    /**
     * Creates a server (without starting it) bound to the given port and worker-pool size.
     *
     * @param port       the TCP port to listen on
     * @param maxThreads the number of worker threads used to handle requests
     */
    public MyHTTPServer(int port, int maxThreads) {
        super("MyHTTPServer-MainThread");
        this.port = port;
        this.nThreads = maxThreads;
        this.running = false;
        this.getToServletMap = new ConcurrentHashMap<>();
        this.postToServletMap = new ConcurrentHashMap<>();
        this.deleteToServletMap = new ConcurrentHashMap<>();
        this.serverSocket = null;
        this.executor = null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Unsupported HTTP methods are ignored (logged as a warning).
     */
    @Override
    public void addServlet(String httpCommand, String uri, Servlet s) {
        Map<String, Servlet> servletMap = getServletMap(httpCommand.toUpperCase());
        if (servletMap != null) {
            servletMap.put(uri, s);
            logger.log(Level.INFO, "Added servlet for {0} {1}: {2}", new Object[]{httpCommand, uri, s.getClass().getName()});
        } else {
            logger.log(Level.WARNING, "Attempted to add servlet for unsupported HTTP command: {0}", httpCommand);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The removed servlet is closed. Unsupported HTTP methods are ignored (logged as a warning).
     */
    @Override
    public void removeServlet(String httpCommand, String uri) {
        Map<String, Servlet> servletMap = getServletMap(httpCommand.toUpperCase());
        if (servletMap != null) {
            closeAndRemoveServlet(servletMap, uri);
            logger.log(Level.INFO, "Removed servlet for {0} {1}", new Object[]{httpCommand, uri});
        } else {
            logger.log(Level.WARNING, "Attempted to remove servlet for unsupported HTTP command: {0}", httpCommand);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Initializes the worker pool and starts the accept loop on a background thread, returning
     * immediately. Does nothing if the server is already running.
     */
    @Override
    public void start() {
        if (!running) {
            initializeServer();
            // MyHTTPServer extends Thread; super.start() runs run() on a
            // background thread so this method can return immediately.
            super.start();
            logger.log(Level.INFO, "MyHTTPServer started on port {0} with {1} threads.", new Object[]{String.valueOf(port), String.valueOf(nThreads)});
        } else {
            logger.log(Level.WARNING, "Server start() called when already running.");
        }
    }

    /**
     * The server's main loop: opens the server socket and accepts client connections until
     * the server is closed. Runs on the background thread started by {@link #start()}; not
     * meant to be called directly.
     */
    @Override
    public void run() {
        try (ServerSocket ss = createServerSocket()) {
            logger.log(Level.INFO, "Server socket listening on port {0}", String.valueOf(port));
            acceptClientConnections(ss);
        } catch (IOException e) {
            if (running) {
                logger.log(Level.SEVERE, "Server socket error in main loop, shutting down.", e);
            }
        } finally {
            logger.info("Server main loop exiting.");
            closeExecutor();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Stops the accept loop, closes every registered servlet, and shuts down the worker pool.
     */
    @Override
    public void close() {
        logger.info("Server shutdown requested.");
        stopServer();
        closeAllServlets();
        closeExecutor();
        logger.info("Server shutdown complete.");
    }

    /** Runs in a worker thread, once per accepted connection. */
    private void handleClient(Socket client) {
        logger.log(Level.INFO, "Connection received from: {0}", client.getRemoteSocketAddress());
        try (Socket c = client;
             BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
             OutputStream out = c.getOutputStream()) {
            processClientRequest(br, out);
        } catch (IOException e) {
            // Browsers often pre-open and close connections without sending
            // anything; we swallow those silently to keep the log readable.
            if (e.getMessage() != null && e.getMessage().contains("Empty or null request line")) {
                logger.log(Level.INFO, "Client connected but sent no data or closed connection early: {0}", client.getRemoteSocketAddress());
            } else {
                logger.log(Level.WARNING, "IOException during client handling for {0}: {1}",
                        new Object[]{client.getRemoteSocketAddress(), e.getMessage()});
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error during client handling for " + client.getRemoteSocketAddress(), e);
        }
        logger.log(Level.INFO, "Finished handling connection from: {0}", client.getRemoteSocketAddress());
    }

    private Map<String, Servlet> getServletMap(String httpCommand) {
        return switch (httpCommand) {
            case "GET" -> getToServletMap;
            case "POST" -> postToServletMap;
            case "DELETE" -> deleteToServletMap;
            default -> null;
        };
    }

    private void closeAndRemoveServlet(Map<String, Servlet> servletMap, String uri) {
        Servlet servlet = servletMap.remove(uri);
        if (servlet != null) {
            try {
                servlet.close();
                logger.log(Level.FINE, "Closed servlet for URI: {0}", uri);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error closing servlet during removal for URI: " + uri, e);
            }
        }
    }

    private void initializeServer() {
        running = true;
        executor = Executors.newFixedThreadPool(nThreads);
    }

    private ServerSocket createServerSocket() throws IOException {
        serverSocket = new ServerSocket(port);
        // Short accept() timeout so the loop can check the running flag
        // and exit cleanly when close() is called.
        serverSocket.setSoTimeout(1000);
        return serverSocket;
    }

    private void acceptClientConnections(ServerSocket ss) {
        while (running) {
            try {
                Socket client = ss.accept();
                executor.submit(() -> handleClient(client));
            } catch (SocketTimeoutException e) {
                // expected: just loop and re-check "running"
            } catch (IOException e) {
                if (running) {
                    logger.log(Level.WARNING, "IOException accepting client connection", e);
                }
            }
        }
    }

    /**
     * Parses one request, routes it to the matching servlet, and writes the response. Turns
     * missing routes into {@code 404}, {@link HTTPException}s into their status, and any other
     * failure into {@code 500}, so a well-formed JSON error is always returned.
     */
    private void processClientRequest(BufferedReader br, OutputStream out) throws IOException {
        HTTPRequest ri;
        String requestIdentifier;
        try {
            ri = RequestParser.parseRequest(br);
            requestIdentifier = ri.getHttpCommand() + " " + ri.getUri();
        } catch (IOException e) {
            if (e.getMessage() == null || !e.getMessage().contains("Empty or null request line")) {
                logger.log(Level.WARNING, "IOException during request parsing: {0}", e.getMessage());
                writeResponse(out, createJsonErrorResponse(HTTPStatus.BAD_REQUEST, "Malformed request reading failed"));
                return;
            } else {
                throw e;
            }
        }

        Servlet servlet = findServlet(ri.getHttpCommand(), ri.getResourceUri());

        if (servlet == null) {
            logger.log(Level.INFO, "No servlet found for request: {0}", requestIdentifier);
            writeResponse(out, createJsonErrorResponse(HTTPStatus.NOT_FOUND, "No servlet for " + ri.getHttpCommand() + " " + ri.getResourceUri()));
            return;
        }

        try {
            HTTPResponse response = servlet.handle(ri);
            writeResponse(out, response);
        } catch (HTTPException e) {
            writeResponse(out, createJsonErrorResponse(e.getStatus(), e.getMessage()));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error executing servlet " + servlet.getClass().getName() + " for request " + requestIdentifier, e);
            writeResponse(out, createJsonErrorResponse(HTTPStatus.INTERNAL_SERVER_ERROR, "Servlet execution failed"));
        }
    }

    private Servlet findServlet(String httpCommand, String uri) {
        Map<String, Servlet> servletMap = getServletMap(httpCommand);
        return matchServletToUri(uri, servletMap);
    }

    /** Longest-prefix URI match: more specific patterns win. */
    private Servlet matchServletToUri(String uri, Map<String, Servlet> uriToServlet) {
        Servlet matchingServlet = null;
        int longestPrefixLength = -1;

        if (uriToServlet == null) return null;

        for (String currentUri : uriToServlet.keySet()) {
            if (uri.startsWith(currentUri) && currentUri.length() > longestPrefixLength) {
                longestPrefixLength = currentUri.length();
                matchingServlet = uriToServlet.get(currentUri);
            }
        }
        return matchingServlet;
    }

    private void writeResponse(OutputStream out, HTTPResponse response) throws IOException {
        String statusText = HTTPStatus.getMessageForCode(response.getStatusCode());
        StringBuilder header = new StringBuilder("HTTP/1.1 " + response.getStatusCode() + " " + statusText + "\r\n");
        
        for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
            header.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        header.append("Connection: close\r\n\r\n");

        out.write(header.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.write(response.getBody());
        out.flush();
    }

    /** Builds a JSON error response of the form {@code {"error": "..."}} for the given status. */
    private HTTPResponse createJsonErrorResponse(HTTPStatus status, String message) {
        String escapedMsg = message.replace("\"", "\\\"");
        String jsonBody = "{\"error\": \"" + escapedMsg + "\"}";
        return new HTTPResponse(status, "application/json", jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void stopServer() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error closing server socket", e);
            }
        }
    }

    private void closeAllServlets() {
        for (Map<String, Servlet> servletMap : new Map[]{getToServletMap, postToServletMap, deleteToServletMap}) {
            if (servletMap != null) {
                for (Servlet servlet : servletMap.values()) {
                    try {
                        servlet.close();
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Error closing servlet " + servlet.getClass().getName(), e);
                    }
                }
            }
        }
    }

    /** Graceful shutdown of the worker pool, with a forced shutdown after 5s. */
    private void closeExecutor() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warning("Executor did not terminate gracefully after 5 seconds, forcing shutdown.");
                    executor.shutdownNow();
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.severe("Executor did not terminate even after forced shutdown.");
                    }
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Interrupted while waiting for executor shutdown", e);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
