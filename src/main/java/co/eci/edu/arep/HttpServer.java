package co.eci.edu.arep;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

public class HttpServer {
    private static final int PORT = 8080;
    private static String WEB_ROOT = "src/main/resources/static";
    private static Map<String, BiFunction<HttpRequest, HttpResponse, String>> servicios = new HashMap<>();
    private static ExecutorService executor = Executors.newFixedThreadPool(10);
    private static volatile boolean running = true;
    private static ServerSocket serverSocket;

    /**
     * Starts the HTTP server.
     *
     * @param args the command line arguments
     */
    public static void start(String[] args) {

        Runtime.getRuntime().addShutdownHook(new Thread(() ->{
            System.out.println("Shutting down server...");
            stop();
        }));

        running = true;


        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Server started on port " + PORT);
                serverSocket.setSoTimeout(1000);

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        executor.submit(() -> {
                            try{
                                handleRequest(clientSocket);
                            }
                            catch (IOException | URISyntaxException e){
                                e.printStackTrace();
                            }
                            finally {
                                try {
                                    clientSocket.close();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });

                    }
                    catch (SocketTimeoutException e){
                        continue;
                    }
                    catch (SocketException e){
                        if(!running){
                            System.out.println("Server stopped.");
                        }
                        else {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Type 'exit' to stop the server.");
            while (scanner.hasNextLine()){
                String command = scanner.nextLine();
                if("exit".equalsIgnoreCase(command.trim())){
                    stop();
                    break;
                }
            }
            scanner.close();
        }).start();

    }

    /**
     * Stop the HTTP server.
     */
    public static void stop(){
        running = false;
        if(serverSocket != null && !serverSocket.isClosed()){
            try{
                serverSocket.close();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Handles an incoming client request.
     *
     * @param clientSocket the socket connected to the client
     * @throws IOException if an I/O error occurs when reading from the input stream or writing to the output stream
     * @throws URISyntaxException if the URI syntax is incorrect
     */
    static void handleRequest(Socket clientSocket) throws IOException, URISyntaxException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        OutputStream out = clientSocket.getOutputStream();

        String requestLine = in.readLine();
        if (requestLine == null) return;
        System.out.println("Request: " + requestLine);

        String[] tokens = requestLine.split(" ");
        if (tokens.length < 2) return;

        String method = tokens[0];
        String uri = tokens[1];

        // Parsear la URI
        URI resourceUri = new URI(uri);
        String path = resourceUri.getPath();
        String query = resourceUri.getQuery();

        System.out.println("Path: " + path);
        System.out.println("Query: " + query);


        if (servicios.containsKey(path)) {
            HttpRequest req = new HttpRequest(path, query);
            HttpResponse resp = new HttpResponse();
            String outputLine = processRequest(req, resp);
            out.write(outputLine.getBytes());
        } else {
            serveStaticFile(path, out);
        }
    }

    /**
     * Processes an HTTP request and returns an HTTP response.
     *
     * @param req the HTTP request
     * @param resp the HTTP response
     * @return the HTTP response
     */
    static String processRequest(HttpRequest req, HttpResponse resp) {
        BiFunction<HttpRequest, HttpResponse, String> service = servicios.get(req.getPath());
        System.out.println("service: " + service);
        System.out.println("req: " + req.getPath());

        if (service != null) {
            String responseBody = service.apply(req, resp);
            return "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: application/json\r\n"
                    + "\r\n"
                    + "{\"response\":\"" + responseBody + "\"}";
        } else {
            return "HTTP/1.1 404 Not Found\r\n"
                    + "Content-Type: text/plain\r\n"
                    + "\r\n"
                    + "404 Not Found";
        }
    }

    /**
     * Registers a service for a given route.
     *
     * @param route the route
     * @param service the service
     */
    public static void get(String route, BiFunction<HttpRequest, HttpResponse, String> service) {
        System.out.println("route: " + route);
        servicios.put("/app" + route, service);
    }

    /**
     * Sets the directory for static files.
     *
     * @param path the directory path
     */
    public static void staticfiles(String path) {
        WEB_ROOT = path;
        System.out.println("Static files directory set to: " + WEB_ROOT);
    }

    /**
     * Serves a static file.
     *
     * @param path the file path
     * @param out the output stream
     * @throws IOException if an I/O error occurs when reading from the input stream or writing to the output stream
     */
    static void serveStaticFile(String path, OutputStream out) throws IOException {

        if (path.equals("/")) path = "/index.html";
        System.out.println("Serving static file: " + path);
        File file = new File(WEB_ROOT, path);
        System.out.println(file.getAbsolutePath());
        File notFoundFile = new File(WEB_ROOT, "/error.html");

        if (!file.exists() || file.isDirectory()) {

            if (notFoundFile.exists()) {

                BufferedReader reader = new BufferedReader(new FileReader(notFoundFile));
                StringBuilder responseContent = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    responseContent.append(line).append("\n");
                }

                reader.close();

                String response = "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + responseContent.length() + "\r\n" +
                        "\r\n" +
                        responseContent.toString();

                out.write(response.getBytes());
            } else {

                String response = "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "\r\n" +
                        "404 Not Found";
                out.write(response.getBytes());
            }
        } else {
            String contentType = getContentType(file);
            byte[] fileData = Files.readAllBytes(file.toPath());

            String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "Content-Length: " + fileData.length + "\r\n" +
                    "\r\n";
            out.write(responseHeaders.getBytes());
            out.write(fileData);
        }
        out.flush();
    }

    /**
     * Returns the content type of a file based on its extension.
     *
     * @param file the file
     * @return the content type
     */
    private static String getContentType(File file) {
        Map<String, String> mimeTypes = new HashMap<>();
        mimeTypes.put("html", "text/html");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("js", "application/javascript");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("png", "image/png");

        String ext = file.getName().substring(file.getName().lastIndexOf('.') + 1);
        return mimeTypes.getOrDefault(ext, "application/octet-stream");
    }
}