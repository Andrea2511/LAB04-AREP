package co.eci.edu.arep;


import java.util.HashMap;
import java.util.Map;

/**
 * Represents an HTTP request with method, path, and query parameters.
 */
public class HttpRequest {
    private String path;
    private String query;
    private Map<String, String> queryParams;

    public HttpRequest(String path, String query) {
        this.path = path;
        this.query = query;
        this.queryParams = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] keyValue = param.split("=");
                if (keyValue.length > 1) {
                    queryParams.put(keyValue[0], keyValue[1]);
                }
            }
        }
    }

    public String getPath() {
        return path;
    }

    public String getQuery() {
        return query;
    }

    public String getValues(String key) {
        return queryParams.get(key);
    }
}