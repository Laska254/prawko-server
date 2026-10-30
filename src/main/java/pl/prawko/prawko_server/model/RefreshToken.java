package pl.prawko.prawko_server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/**
 * Represents a {@code RefreshToken} entity, a long-lived credential exchanged for new access tokens without signing in
 * again.
 * <p>
 * Stored as the SHA-256 hash of the token (never the token itself) with an expiry time. A token is used once: using it
 * revokes it, and the revoked row is kept until it expires, so its reuse can be detected.
 * <p>
 * Relationships:
 * <ul>
 *     <li>{@link User}: Each {@code User} can have multiple refresh tokens, one per signed-in client.</li>
 * </ul>
 * All setters are returning {@code RefreshToken} itself, enabling method chaining.
 * {@link #toString()} leaves out the token hash, so it's safe to log.
 */
@Entity
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(length = 64, unique = true, nullable = false)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Instant expires;

    private boolean revoked;

    public long getId() {
        return id;
    }

    public RefreshToken setId(final long id) {
        this.id = id;
        return this;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public RefreshToken setTokenHash(final String tokenHash) {
        this.tokenHash = tokenHash;
        return this;
    }

    public User getUser() {
        return user;
    }

    public RefreshToken setUser(final User user) {
        this.user = user;
        return this;
    }

    public Instant getExpires() {
        return expires;
    }

    public RefreshToken setExpires(final Instant expires) {
        this.expires = expires;
        return this;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public RefreshToken setRevoked(final boolean revoked) {
        this.revoked = revoked;
        return this;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof RefreshToken other)) {
            return false;
        }
        return id != 0 && id == other.getId();
    }

    @Override
    public int hashCode() {
        return RefreshToken.class.hashCode();
    }

    @Override
    public String toString() {
        return "RefreshToken{" +
                "id=" + id +
                ", expires=" + expires +
                ", revoked=" + revoked +
                ", user=" + (user == null ? null : user.getId()) +
                '}';
    }

}
