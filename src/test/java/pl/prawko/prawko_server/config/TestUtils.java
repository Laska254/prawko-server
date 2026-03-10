package pl.prawko.prawko_server.config;

import org.springframework.http.HttpHeaders;

public class TestUtils {

    public static final String BASE_URL = "http://localhost:";

    public static void authUser(final HttpHeaders headers) {
        headers.setBasicAuth("pippin", "lembasy");
    }

    public static void authAdmin(final HttpHeaders headers) {
        headers.setBasicAuth("gimli", "krasnoludka");
    }

}
