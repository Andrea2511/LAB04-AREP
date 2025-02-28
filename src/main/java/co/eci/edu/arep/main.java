package co.eci.edu.arep;

import static co.eci.edu.arep.HttpServer.start;
import static co.eci.edu.arep.HttpServer.get;
import static co.eci.edu.arep.HttpServer.staticfiles;

public class main {

    public static void main(String[] args) {

        staticfiles("src/main/resources");
        get("/hello", (req, resp) -> "Hello " + req.getValues("name"));
        get("/pi", (req, resp) -> {
            return String.valueOf(Math.PI);
        });

        start(args);

    }
}
