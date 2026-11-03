package pl.prawko.prawko_server.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Represents a {@code User} entity within the application.
 * <p>
 * A {@code User} contains personal details such as:
 *  <ul>
 *     <li>first name - limited to 31 characters</li>
 *     <li>last name - limited to 31 characters</li>
 *     <li>username - limited to 31 characters, unique ignoring case</li>
 *     <li>email - limited to 254 characters, stored lowercased and unique</li>
 *     <li>password - hashed and limited to 63 characters, {@code null} for users who only sign in with a linked
 *     {@link UserIdentity}</li>
 * </ul>
 * and system-related attributes like the {@link Role}, whether the account is enabled, the creation timestamp and the last
 * update timestamp.
 * <p>
 * A pending password reset is stored as the SHA-256 hash of its token (never the token itself) with an expiry time.
 * Both are {@code null} when no reset is pending.
 * <p>
 * A new account is disabled until its email address is verified. The pending verification is stored the same way,
 * as the SHA-256 hash of its token with an expiry time, both {@code null} once the account is verified.
 * <p>
 * A changed email address is kept as the pending email until it's verified with the same kind of token, only then it
 * replaces the current email. The current address must confirm it too, with another token sharing the expiry.
 * A {@code null} token hash means that address confirmed.
 * Changing or resetting the password cancels the change.
 * <p>
 * Relationships:
 * <ul>
 *     <li>{@link Exam}: A user can be assigned to multiple exams.</li>
 *     <li>{@link RefreshToken}: A user can have multiple refresh tokens, deleted with the user.</li>
 *     <li>{@link UserIdentity}: A user can have one linked account per {@link IdentityProvider}, ordered by provider
 *     and deleted with the user.</li>
 * </ul>
 * The entity is mapped to the database table {@code user}. The unique constraints on the username and email are named
 * {@value #USER_NAME_CONSTRAINT} and {@value #EMAIL_CONSTRAINT}. The database's collation decides whether the username
 * constraint ignores case (MariaDB's default does, H2 doesn't); the services check it ignoring case either way.
 * Emails, including the pending one, are lowercased by their setters, so their constraint behaves the same everywhere.
 * Timestamps are {@link Instant}s, so they don't depend on the JVM's time zone; {@code created} and {@code updated} are
 * set automatically.
 * All setters are returning {@code User} itself, enabling method chaining.
 * {@link #toString()} leaves out personal data and credentials, so it's safe to log.
 */
@Entity
@Table(name = "`user`", uniqueConstraints = {
        @UniqueConstraint(name = User.USER_NAME_CONSTRAINT, columnNames = "user_name"),
        @UniqueConstraint(name = User.EMAIL_CONSTRAINT, columnNames = "email")
})
public class User {

    public static final String USER_NAME_CONSTRAINT = "uk_user_user_name";
    public static final String EMAIL_CONSTRAINT = "uk_user_email";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(length = 31)
    private String firstName;

    @Column(length = 31)
    private String lastName;

    @Column(length = 31)
    private String userName;

    @Column(length = 254)
    private String email;

    @Column(length = 254)
    private String pendingEmail;

    @Column(length = 63)
    private String password;

    private boolean enabled;

    @CreationTimestamp
    private Instant created;

    @UpdateTimestamp
    private Instant updated;

    @Column(length = 64, unique = true)
    private String passwordResetTokenHash;

    private Instant passwordResetTokenExpires;

    @Column(length = 64, unique = true)
    private String verificationTokenHash;

    private Instant verificationTokenExpires;

    @Column(length = 64, unique = true)
    private String currentEmailTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Exam> exams;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefreshToken> refreshTokens;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("provider")
    @BatchSize(size = 50)
    private List<UserIdentity> identities = new ArrayList<>();

    public long getId() {
        return id;
    }

    public User setId(final long id) {
        this.id = id;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public User setFirstName(final String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public User setLastName(final String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public User setUserName(final String userName) {
        this.userName = userName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public User setEmail(final String email) {
        this.email = lowercase(email);
        return this;
    }

    public String getPendingEmail() {
        return pendingEmail;
    }

    public User setPendingEmail(final String pendingEmail) {
        this.pendingEmail = lowercase(pendingEmail);
        return this;
    }

    /**
     * Drops a pending email change and invalidates its verification links.
     * <p>
     * The token expiry is kept, so the verification email cooldown still applies.
     *
     * @return {@code true} if a change was pending
     */
    public boolean cancelEmailChange() {
        if (pendingEmail == null) {
            return false;
        }
        pendingEmail = null;
        verificationTokenHash = null;
        currentEmailTokenHash = null;
        return true;
    }

    public String getPassword() {
        return password;
    }

    public User setPassword(final String password) {
        this.password = password;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public User setEnabled(final boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public User setCreated(final Instant created) {
        this.created = created;
        return this;
    }

    public Instant getUpdated() {
        return updated;
    }

    public User setUpdated(final Instant updated) {
        this.updated = updated;
        return this;
    }

    public String getPasswordResetTokenHash() {
        return passwordResetTokenHash;
    }

    public User setPasswordResetTokenHash(final String passwordResetTokenHash) {
        this.passwordResetTokenHash = passwordResetTokenHash;
        return this;
    }

    public Instant getPasswordResetTokenExpires() {
        return passwordResetTokenExpires;
    }

    public User setPasswordResetTokenExpires(final Instant passwordResetTokenExpires) {
        this.passwordResetTokenExpires = passwordResetTokenExpires;
        return this;
    }

    public String getVerificationTokenHash() {
        return verificationTokenHash;
    }

    public User setVerificationTokenHash(final String verificationTokenHash) {
        this.verificationTokenHash = verificationTokenHash;
        return this;
    }

    public Instant getVerificationTokenExpires() {
        return verificationTokenExpires;
    }

    public User setVerificationTokenExpires(final Instant verificationTokenExpires) {
        this.verificationTokenExpires = verificationTokenExpires;
        return this;
    }

    public String getCurrentEmailTokenHash() {
        return currentEmailTokenHash;
    }

    public User setCurrentEmailTokenHash(final String currentEmailTokenHash) {
        this.currentEmailTokenHash = currentEmailTokenHash;
        return this;
    }

    public Role getRole() {
        return role;
    }

    public User setRole(final Role role) {
        this.role = role;
        return this;
    }

    public List<Exam> getExams() {
        return exams;
    }

    public User setExams(final List<Exam> exams) {
        this.exams = exams;
        return this;
    }

    public List<UserIdentity> getIdentities() {
        return identities;
    }

    /**
     * Links an account at the provider to this user.
     *
     * @param provider       the provider of the account
     * @param providerUserId the provider's ID of the account
     * @return this user
     */
    public User addIdentity(final IdentityProvider provider, final String providerUserId) {
        identities.add(new UserIdentity()
                .setProvider(provider)
                .setProviderUserId(providerUserId)
                .setUser(this));
        return this;
    }

    public boolean hasIdentity(final IdentityProvider provider) {
        return identities.stream()
                .map(UserIdentity::getProvider)
                .anyMatch(provider::equals);
    }

    private static @Nullable String lowercase(@Nullable final String email) {
        if (email == null) {
            return null;
        }
        return email.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof User other)) {
            return false;
        }
        return id != 0 && id == other.getId();
    }

    @Override
    public int hashCode() {
        return User.class.hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", role=" + role +
                ", enabled=" + enabled +
                ", created=" + created +
                ", updated=" + updated +
                '}';
    }

}
