package pl.prawko.prawko_server.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.sun.net.httpserver.HttpServer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Serves a JWKS on a random local port in place of Google's, so ID tokens signed by a test key are verified the same
 * way as real ones.
 */
public class GoogleJwksStub implements AutoCloseable {

    private static final String PATH = "/oauth2/v3/certs";

    private final HttpServer server;

    public GoogleJwksStub(final JWKSet jwks) throws IOException {
        final var body = jwks.toString()
                .getBytes(StandardCharsets.UTF_8);
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext(PATH, exchange -> {
            exchange.getResponseHeaders()
                    .set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(HttpStatus.OK.value(), body.length);
            try (var response = exchange.getResponseBody()) {
                response.write(body);
            }
        });
        server.start();
    }

    public String url() {
        final var address = server.getAddress();
        return "http://" + address.getHostString() + ":" + address.getPort() + PATH;
    }

    @Override
    public void close() {
        server.stop(0);
    }

}
