package pl.prawko.prawko_server.util;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class SecureTokensTest {

    @Test
    void generate_returnUrlSafe256BitToken_whenCalled() {
        final var token = SecureTokens.generate();

        assertThat(token).matches("[\\w-]{43}");
        assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
    }

    @Test
    void generate_returnDifferentToken_whenCalledAgain() {
        assertThat(SecureTokens.generate()).isNotEqualTo(SecureTokens.generate());
    }

    @Test
    void hash_returnSha256Hex_whenCalled() {
        assertThat(SecureTokens.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

}
