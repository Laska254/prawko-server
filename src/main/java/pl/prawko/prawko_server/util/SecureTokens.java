package pl.prawko.prawko_server.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;

import java.util.Base64;

/**
 * Generates and hashes single-use tokens sent in emailed links.
 * <p>
 * Tokens are 256-bit random values, URL-safe Base64 encoded. Only their SHA-256 hash should be stored, so a database
 * leak doesn't allow using them. A fast hash is sufficient, since the tokens can't be guessed.
 */
public final class SecureTokens {

    private static final int TOKEN_BYTES = 32;
    private static final StringKeyGenerator GENERATOR =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), TOKEN_BYTES);

    private SecureTokens() {
    }

    public static String generate() {
        return GENERATOR.generateKey();
    }

    public static String hash(final String token) {
        return DigestUtils.sha256Hex(token);
    }

}
