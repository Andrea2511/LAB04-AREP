package co.eci.edu.arep;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import static org.junit.jupiter.api.Assertions.*;


class HttpServerTest {

    @BeforeEach
    void setUp() {
        HttpServer.staticfiles("src/main/resources");
    }

    @Test
    void testProcessRequest_WithRegisteredService() {
        HttpServer.get("/test", (req, resp) -> "Hello, World!");
        HttpRequest request = new HttpRequest("/app/test", "");
        HttpResponse response = new HttpResponse();

        String result = HttpServer.processRequest(request, response);

        assertTrue(result.contains("HTTP/1.1 200 OK"));
        assertTrue(result.contains("\"response\":\"Hello, World!\""));
    }

    @Test
    void testProcessRequest_WithUnregisteredService() {
        HttpRequest request = new HttpRequest("/app/unknown", "");
        HttpResponse response = new HttpResponse();

        String result = HttpServer.processRequest(request, response);

        assertTrue(result.contains("HTTP/1.1 404 Not Found"));
        assertTrue(result.contains("404 Not Found"));
    }

    @Test
    void testServeStaticFile_FileExists() throws Exception {
        OutputStream out = new ByteArrayOutputStream();

        HttpServer.serveStaticFile("/index.html", out);

        String result = out.toString();
        assertTrue(result.contains("HTTP/1.1 200 OK"));
        assertTrue(result.contains("Content-Type: text/html"));
    }

    @Test
    void testServeStaticFile_FileNotExists() throws Exception {
        OutputStream out = new ByteArrayOutputStream();

        HttpServer.serveStaticFile("/nonexistent.html", out);

        String result = out.toString();
        assertTrue(result.contains("HTTP/1.1 404 Not Found"));
    }

}
