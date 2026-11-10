package pl.prawko.prawko_server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.Optional;

/**
 * Represents a {@code RefreshToken} entity, a long-lived credential exchanged for new access tokens without signing in
 * again.
 * <p>
 * Stored as the SHA-256 hash of the token (never the token itself) with an expiry time. A token is used once: using it
 * revokes it and links it to its successor, and the revoked row is kept until it expires, so its reuse can be detected
 * and a retry told apart from it.
 * <p>
 * Relationships:
 * <ul>
 *     <li>{@link User}: Each {@code User} can have multiple refresh tokens, one per signed-in client.</li>
 *     <li>{@code RefreshToken}: A used token points to the token issued in its place. Deleting the successor clears
 *     the link.</li>
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

    private Instant revokedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private RefreshToken replacedBy;

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

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public RefreshToken setRevokedAt(final Instant revokedAt) {
        this.revokedAt = revokedAt;
        return this;
    }

    public RefreshToken getReplacedBy() {
        return replacedBy;
    }

    public RefreshToken setReplacedBy(final RefreshToken replacedBy) {
        this.replacedBy = replacedBy;
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
                ", revokedAt=" + revokedAt +
                ", replacedBy=" + Optional.ofNullable(replacedBy)
                        .map(RefreshToken::getId)
                        .orElse(null) +
                ", user=" + Optional.ofNullable(user)
                        .map(User::getId)
                        .orElse(null) +
                '}';
    }

}
