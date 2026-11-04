package pl.prawko.prawko_server.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class EmailStatusTest {

    @ParameterizedTest
    @CsvSource({"UNVERIFIED, false", "VERIFIED, true", "AUTHORITATIVE, true"})
    void isVerified_returnTrue_unlessStatusIsUnverified(final EmailStatus status, final boolean expected) {
        assertThat(status.isVerified()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"UNVERIFIED, false", "VERIFIED, false", "AUTHORITATIVE, true"})
    void isAuthoritative_returnTrue_onlyWhenStatusIsAuthoritative(final EmailStatus status, final boolean expected) {
        assertThat(status.isAuthoritative()).isEqualTo(expected);
    }

}
