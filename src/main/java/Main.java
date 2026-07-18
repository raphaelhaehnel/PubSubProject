import server.HTTPServer;
import server.MyHTTPServer;
import servlets.ConfLoader;
import servlets.GraphDisplayer;
import servlets.StaticResourceServlet;
import servlets.ResetServlet;
import servlets.ClearServlet;
import servlets.TopicDisplayer;


/**
 * Entry point: starts the HTTP server with all endpoints registered,
 * then blocks until the user presses Enter to shut it down.
 */
public class Main {

    /**
     * Starts the HTTP server on port 8080, registers every endpoint, and blocks until the
     * user presses Enter, then shuts the server down.
     *
     * @param args command-line arguments (unused)
     * @throws Exception if the server fails to start or blocking on {@code System.in} fails
     */
    public static void main(String[] args) throws Exception {

        HTTPServer server = new MyHTTPServer(8080, 5);

        // GET  /publish  -> publish a message on a topic
        // POST /upload   -> upload a JSON configuration
        // GET  /graph    -> get the current graph as JSON
        // POST /reset    -> reset every agent and set every topic to 0
        // POST /clear    -> delete all topics and agents
        // GET  /app      -> serve the front-end (HTML/CSS/JS)
        server.addServlet("GET", "/publish", new TopicDisplayer());
        server.addServlet("POST", "/upload", new ConfLoader());
        server.addServlet("GET", "/graph", new GraphDisplayer());
        server.addServlet("POST", "/reset", new ResetServlet());
        server.addServlet("POST", "/clear", new ClearServlet());
        server.addServlet("GET", "/app", new StaticResourceServlet("web/dist"));

        server.start();

        // Block here until the user presses Enter in the terminal.
        System.in.read();

        server.close();
        System.out.println("Server stopped.");
    }
}
