package server;

import servlets.Servlet;

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

    @Override
    public void start() {
        if (!running) {
            initializeServer();
            super.start();
            logger.log(Level.INFO, "MyHTTPServer started on port {0} with {1} threads.", new Object[]{port, nThreads});
        } else {
            logger.log(Level.WARNING, "Server start() called when already running.");
        }
    }

    @Override
    public void run() {
        try (ServerSocket ss = createServerSocket()) {
            logger.log(Level.INFO, "Server socket listening on port {0}", port);
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

    @Override
    public void close() {
        logger.info("Server shutdown requested.");
        stopServer();
        closeAllServlets();
        closeExecutor();
        logger.info("Server shutdown complete.");
    }

    private void handleClient(Socket client) {
        logger.log(Level.INFO, "Connection received from: {0}", client.getRemoteSocketAddress());
        try (Socket c = client;
             BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
             OutputStream out = c.getOutputStream()) {
            processClientRequest(br, out);
        } catch (IOException e) {
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
        switch (httpCommand) {
            case "GET":
                return getToServletMap;
            case "POST":
                return postToServletMap;
            case "DELETE":
                return deleteToServletMap;
            default:
                return null;
        }
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
        logger.fine("Server initialized, thread pool created.");
    }


    private ServerSocket createServerSocket() throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(1000);
        return serverSocket;
    }

    private void acceptClientConnections(ServerSocket ss) {
        logger.info("Server ready to accept connections...");
        while (running) {
            try {
                Socket client = ss.accept();
                executor.submit(() -> handleClient(client));
            } catch (SocketTimeoutException e) {
                // Timeout occurred - just loop again to check if we're still running
            } catch (IOException e) {
                if (running) {
                    logger.log(Level.WARNING, "IOException accepting client connection", e);
                }
            }
        }
        logger.info("Stopped accepting connections.");
    }

    private void processClientRequest(BufferedReader br, OutputStream out) throws IOException {
        RequestParser.RequestInfo ri = null;
        String requestIdentifier = "Unknown Request";
        try {
            ri = RequestParser.parseRequest(br);
            requestIdentifier = ri.getHttpCommand() + " " + ri.getUri();
            logger.log(Level.FINE, "Processing request: {0}", requestIdentifier);
        } catch (IOException e) {
            if (e.getMessage() == null || !e.getMessage().contains("Empty or null request line")) {
                logger.log(Level.WARNING, "IOException during request parsing: {0}", e.getMessage());
                try {
                    writeBadRequest(out, "Malformed request reading failed");
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, "Failed to send 400 error response.", ioe);
                }
                return;
            } else {
                throw e;
            }
        }

        Servlet servlet = findServlet(ri.getHttpCommand(), ri.getResourceUri());

        if (servlet == null) {
            logger.log(Level.INFO, "No servlet found for request: {0}", requestIdentifier);
            try {
                writeNotFound(out, "No servlet for " + ri.getHttpCommand() + " " + ri.getResourceUri());
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Failed to send 404 error response.", ioe);
            }
            return;
        }

        try {
            logger.log(Level.FINE, "Dispatching request {0} to servlet {1}",
                    new Object[]{requestIdentifier, servlet.getClass().getName()});
            servlet.handle(ri, out);
            logger.log(Level.FINE, "Servlet {0} finished handling request {1}",
                    new Object[]{servlet.getClass().getName(), requestIdentifier});
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error executing servlet " + servlet.getClass().getName() + " for request " + requestIdentifier, e);
            try {
                if (out != null) {
                    writeInternalError(out, "Servlet execution failed");
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Failed to send 500 error response after servlet error.", ioe);
            }
        }
    }

    private Servlet findServlet(String httpCommand, String uri) {
        Map<String, Servlet> servletMap = getServletMap(httpCommand);
        return matchServletToUri(uri, servletMap);
    }


    private Servlet matchServletToUri(String uri, Map<String, Servlet> uriToServlet) {
        Servlet matchingServlet = null;
        int longestPrefixLength = -1;
        String matchedUri = null;

        if (uriToServlet == null) return null;

        for (String currentUri : uriToServlet.keySet()) {
            if (uri.startsWith(currentUri) && currentUri.length() > longestPrefixLength) {
                longestPrefixLength = currentUri.length();
                matchingServlet = uriToServlet.get(currentUri);
                matchedUri = currentUri;
            }
        }
        if (matchingServlet != null) {
            logger.log(Level.FINER, "Matched URI {0} to servlet pattern {1}", new Object[]{uri, matchedUri});
        }
        return matchingServlet;
    }

    private void writeBadRequest(OutputStream out, String msg) throws IOException {
        String resp = "HTTP/1.1 400 Bad Request\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + msg.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + "\r\n" +
                "Connection: close\r\n\r\n" +
                msg;
        out.write(resp.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.flush();
    }


    private void writeNotFound(OutputStream out, String msg) throws IOException {
        String resp = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + msg.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + "\r\n" +
                "Connection: close\r\n\r\n" +
                msg;
        out.write(resp.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.flush();
    }


    private void writeInternalError(OutputStream out, String msg) throws IOException {
        String resp = "HTTP/1.1 500 Internal Server Error\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + msg.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + "\r\n" +
                "Connection: close\r\n\r\n" +
                msg;
        out.write(resp.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.flush();
    }


    private void stopServer() {
        running = false;
        logger.fine("Setting running flag to false.");
        if (serverSocket != null) {
            try {
                serverSocket.close();
                logger.info("Server socket closed.");
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error closing server socket", e);
            }
        }
    }


    private void closeAllServlets() {
        logger.fine("Closing all registered servlets...");
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
        logger.fine("Finished closing servlets.");
    }


    private void closeExecutor() {
        if (executor != null) {
            logger.fine("Shutting down executor service...");
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warning("Executor did not terminate gracefully after 5 seconds, forcing shutdown.");
                    executor.shutdownNow();
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.severe("Executor did not terminate even after forced shutdown.");
                    }
                }
                logger.info("Executor service shut down.");
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Interrupted while waiting for executor shutdown", e);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}