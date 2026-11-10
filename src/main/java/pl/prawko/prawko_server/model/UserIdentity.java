package pl.prawko.prawko_server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Represents a {@code UserIdentity} entity, an account at an {@link IdentityProvider} linked to a {@link User}, so the
 * user can sign in with it.
 * <p>
 * The account is identified by the provider's stable user ID, never by its email, so a later email change on either
 * side doesn't break the link. A provider account is linked to at most one user, and a user has at most one account
 * per provider.
 * <p>
 * Relationships:
 * <ul>
 *     <li>{@link User}: Each {@code User} can have one identity per provider, deleted with the user.</li>
 * </ul>
 * All setters are returning {@code UserIdentity} itself, enabling method chaining.
 * {@link #toString()} leaves out the provider user ID, so it's safe to log.
 */
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "provider_user_id"}),
        @UniqueConstraint(columnNames = {"user_id", "provider"})
})
public class UserIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private IdentityProvider provider;

    @Column(nullable = false)
    private String providerUserId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    public long getId() {
        return id;
    }

    public UserIdentity setId(final long id) {
        this.id = id;
        return this;
    }

    public IdentityProvider getProvider() {
        return provider;
    }

    public UserIdentity setProvider(final IdentityProvider provider) {
        this.provider = provider;
        return this;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public UserIdentity setProviderUserId(final String providerUserId) {
        this.providerUserId = providerUserId;
        return this;
    }

    public User getUser() {
        return user;
    }

    public UserIdentity setUser(final User user) {
        this.user = user;
        return this;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof UserIdentity other)) {
            return false;
        }
        return id != 0 && id == other.getId();
    }

    @Override
    public int hashCode() {
        return UserIdentity.class.hashCode();
    }

    @Override
    public String toString() {
        final long userId;
        if (user == null) {
            userId = 0;
        } else {
            userId = user.getId();
        }
        return "UserIdentity{" +
                "id=" + id +
                ", provider=" + provider +
                ", user=" + userId +
                '}';
    }

}
