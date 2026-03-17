package pl.prawko.prawko_server.config;

import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.RestTestClient;

public class TestUtils {

    public static final String BASE_URL = "http://localhost:";

    public static void authUser(final HttpHeaders headers) {
        headers.setBasicAuth("pippin", "lembasy");
    }

    public static void authAdmin(final HttpHeaders headers) {
        headers.setBasicAuth("gimli", "krasnoludka");
    }

    public static RestTestClient createRestTestClient(final int port, final String controllerBasePath) {
        return RestTestClient
                .bindToServer()
                .baseUrl(TestUtils.BASE_URL + port + controllerBasePath)
                .build();
    }

}
