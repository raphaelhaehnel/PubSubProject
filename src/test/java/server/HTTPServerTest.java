package server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.dtos.HTTPRequest;
import server.dtos.HTTPResponse;
import server.enums.HTTPStatus;
import server.exceptions.HTTPException;
import servlets.Servlet;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class HTTPServerTest {

    private MyHTTPServer server;
    private static final int TEST_PORT = 8081;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize server with 2 threads for testing
        server = new MyHTTPServer(TEST_PORT, 2);
        server.start();
        // Making sure server is fully loaded before tests
        Thread.sleep(1000);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.close();
        // Making sure server closing completes
        Thread.sleep(1000);
    }

    @Test
    void testServerReturns404ForUnmappedUri() throws IOException {
        URL url = new URL("http://localhost:" + TEST_PORT + "/does-not-exist");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        assertEquals(404, connection.getResponseCode(), "Server should return 404 NOT FOUND for unmapped routes.");
        connection.disconnect();
    }

    @Test
    void testServerRoutesToMappedServletAndHandlesPrefixes() throws Exception {
        // Mock a servlet to return a basic 200 OK response
        Servlet mockServlet = Mockito.mock(Servlet.class);
        HTTPResponse mockResponse = new HTTPResponse(HTTPStatus.valueOf("OK"), "text/plain", "MockResponse".getBytes());
        when(mockServlet.handle(any(HTTPRequest.class))).thenReturn(mockResponse);

        // Register the mock to a base URI
        server.addServlet("GET", "/api", mockServlet);

        // Test 1: Exact prefix match
        URL urlExact = new URL("http://localhost:" + TEST_PORT + "/api");
        HttpURLConnection connExact = (HttpURLConnection) urlExact.openConnection();
        connExact.setRequestMethod("GET");
        assertEquals(200, connExact.getResponseCode());

        // Test 2: Longest-prefix match (should still hit the "/api" servlet)
        URL urlExtended = new URL("http://localhost:" + TEST_PORT + "/api/users?id=1");
        HttpURLConnection connExtended = (HttpURLConnection) urlExtended.openConnection();
        connExtended.setRequestMethod("GET");
        assertEquals(200, connExtended.getResponseCode(), "Server should route to the longest prefix matching servlet.");

        connExact.disconnect();
        connExtended.disconnect();
    }

    @Test
    void testServerRemovesServletCorrectly() throws Exception {
        Servlet mockServlet = Mockito.mock(Servlet.class);
        HTTPResponse mockResponse = new HTTPResponse(HTTPStatus.valueOf("OK"), "text/plain", "MockResponse".getBytes());
        when(mockServlet.handle(any(HTTPRequest.class))).thenReturn(mockResponse);

        server.addServlet("GET", "/temp", mockServlet);
        
        // Verify it exists
        URL url = new URL("http://localhost:" + TEST_PORT + "/temp");
        HttpURLConnection conn1 = (HttpURLConnection) url.openConnection();
        assertEquals(200, conn1.getResponseCode());
        conn1.disconnect();

        // Remove it and verify 404
        server.removeServlet("GET", "/temp");
        HttpURLConnection conn2 = (HttpURLConnection) url.openConnection();
        assertEquals(404, conn2.getResponseCode(), "Server should return 404 after servlet is removed.");
        conn2.disconnect();
    }

    @Test
    void testServerHandlesBadRequest400() throws Exception {
        Servlet mockServlet = Mockito.mock(Servlet.class);
        when(mockServlet.handle(any(HTTPRequest.class)))
                .thenThrow(new HTTPException(HTTPStatus.BAD_REQUEST, "Missing parameter"));

        server.addServlet("GET", "/test-400", mockServlet);

        URL url = new URL("http://localhost:" + TEST_PORT + "/test-400");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        assertEquals(400, conn.getResponseCode(), "Server should translate HTTPException(400) to 400 BAD REQUEST");
        conn.disconnect();
    }

    @Test
    void testServerHandlesForbidden403() throws Exception {
        Servlet mockServlet = Mockito.mock(Servlet.class);
        when(mockServlet.handle(any(HTTPRequest.class)))
                .thenThrow(new HTTPException(HTTPStatus.FORBIDDEN, "Access Denied"));

        server.addServlet("GET", "/test-403", mockServlet);

        URL url = new URL("http://localhost:" + TEST_PORT + "/test-403");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        assertEquals(403, conn.getResponseCode(), "Server should translate HTTPException(403) to 403 FORBIDDEN");
        conn.disconnect();
    }

    @Test
    void testServerHandlesInternalServerError500() throws Exception {
        Servlet mockServlet = Mockito.mock(Servlet.class);
        // Throwing a generic RuntimeException to test the server's fallback error handling
        when(mockServlet.handle(any(HTTPRequest.class)))
                .thenThrow(new RuntimeException("Unexpected system failure"));

        server.addServlet("GET", "/test-500", mockServlet);

        URL url = new URL("http://localhost:" + TEST_PORT + "/test-500");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        assertEquals(500, conn.getResponseCode(), "Server should catch unhandled exceptions and return 500 INTERNAL SERVER ERROR");
        conn.disconnect();
    }
}