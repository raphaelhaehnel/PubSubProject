package server;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import servlets.Servlet;
import server.RequestParser.RequestInfo;


public class MainTrain { // RequestParser
    

    private static void testParseRequest() {
        // Test data
        String request = "GET /api/resource?id=123&name=test HTTP/1.1\n" +
                            "Host: example.com\n" +
                            "Content-Length: 5\n"+
                            "\n" +
                            "filename=\"hello_world.txt\"\n"+
                            "\n" +
                            "hello world!\n"+
                            "\n" ;

        BufferedReader input=new BufferedReader(new InputStreamReader(new ByteArrayInputStream(request.getBytes())));
        try {
            RequestParser.RequestInfo requestInfo = RequestParser.parseRequest(input);

            // Test HTTP command
            if (!requestInfo.getHttpCommand().equals("GET")) {
                System.out.println("HTTP command test failed (-5)");
            }

            // Test URI
            if (!requestInfo.getUri().equals("/api/resource?id=123&name=test")) {
                System.out.println("URI test failed (-5)");
            }

            // Test URI segments
            String[] expectedUriSegments = {"api", "resource"};
            if (!Arrays.equals(requestInfo.getUriSegments(), expectedUriSegments)) {
                System.out.println("URI segments test failed (-5)");
                for(String s : requestInfo.getUriSegments()){
                    System.out.println(s);
                }
            } 
            // Test parameters
            Map<String, String> expectedParams = new HashMap<>();
            expectedParams.put("id", "123");
            expectedParams.put("name", "server");
            expectedParams.put("filename","\"hello_world.txt\"");
            if (!requestInfo.getParameters().equals(expectedParams)) {
                System.out.println("Parameters test failed (-5)");
            }

            // Test content
            byte[] expectedContent = "hello world!\n".getBytes();
            if (!Arrays.equals(requestInfo.getContent(), expectedContent)) {
                System.out.println("Content test failed (-5)");
            } 
            input.close();
        } catch (IOException e) {
            System.out.println("Exception occurred during parsing: " + e.getMessage() + " (-5)");
        }        
    }


    public static void testServer() throws Exception{


        int startingThreads = Thread.activeCount();

        Servlet concatenateServlet = new Servlet() {
            @Override
            public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
                System.out.println("Starting handling");
                Map<String, String> parameters = ri.getParameters();
                StringBuilder result = new StringBuilder();
                for (String value : parameters.values()) {
                    result.append(value);
                }

                String body = result.toString();

                String response =
                        "HTTP/1.1 200 OK\n" +
                                "Content-Type: text/plain\n" +
                                "Content-Length: " + body.length() + "\n" +
                                "\n" +
                                body +
                                "\n\n";

                toClient.write(response.getBytes());
                toClient.flush();

                System.out.println("Finished handling");
            }

            @Override
            public void close() throws IOException {

            }
        };

        HTTPServer httpServer = new MyHTTPServer(1234, 10);
        httpServer.addServlet("GET", "/api", concatenateServlet);
        httpServer.start();

        Socket client = new Socket("localhost", 1234);
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(client.getOutputStream())
        );

        // 6. Send GET request
        writer.write("GET /api?id=123&raphael=raphael HTTP/1.1\n");
        writer.write("Host: localhost\n");
        writer.write("\n");
        writer.write("Connection=close\n");
        writer.write("Forward=true\n");
        writer.write("\n");
        writer.flush();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream())
        );

        String line;
        System.out.println("---- Response ----");
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        System.out.println("End response");

        client.close();

        // 8. Close server
        httpServer.close();

        // 9. Wait and verify threads closed
        Thread.sleep(2000);

        System.out.println("Current threads: " + (startingThreads - Thread.activeCount()));
        System.out.println("Test finished.");
    }
    
    public static void main(String[] args) {
        testParseRequest(); // 40 points
        try{
            testServer(); // 60
        }catch(Exception e){
            System.out.println("your server throwed an exception (-60)");
        }
        System.out.println("done");
    }

}
