package com.todoschedule;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AuthFilter 擋下的 401 也要帶 CORS 標頭，前端才讀得到 401 並回登入頁。不需要 MongoDB。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorsTest {

    private static final String FRONT = "http://localhost:5173";

    @LocalServerPort
    int port;

    private HttpResponse<String> get(String origin) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/tasks"))
                .header("Origin", origin).GET().build();
        return HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static String allowOrigin(HttpResponse<String> res) {
        return res.headers().firstValue("Access-Control-Allow-Origin").orElse(null);
    }

    @Test
    void unauthorizedResponseCarriesCorsHeader() throws Exception {
        HttpResponse<String> res = get(FRONT);
        assertEquals(401, res.statusCode());
        assertEquals(FRONT, allowOrigin(res));
        assertTrue(res.body().contains("detail"));
    }

    @Test
    void preflightAllowsAuthorizationHeader() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/tasks"))
                .header("Origin", FRONT)
                .header("Access-Control-Request-Method", "PUT")
                .header("Access-Control-Request-Headers", "authorization")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        assertEquals(FRONT, allowOrigin(res));
        assertTrue(res.headers().firstValue("Access-Control-Allow-Methods").orElse("").contains("PUT"));
    }

    @Test
    void otherOriginIsRejected() throws Exception {
        HttpResponse<String> res = get("https://evil.example");
        assertEquals(403, res.statusCode());
        assertEquals(null, allowOrigin(res));
    }
}
