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
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a {@code User} entity within the application.
 * <p>
 * A {@code User} contains personal details such as:
 *  <ul>
 *     <li>first name - limited to 31 characters</li>
 *     <li>last name - limited to 31 characters</li>
 *     <li>username - limited to 31 characters</li>
 *     <li>email - limited to 63 characters</li>
 *     <li>password - hashed and limited to 63 characters</li>
 * </ul>
 * and system-related attributes like the {@link Role}, whether the account is enabled, the creation timestamp and the last
 * update timestamp.
 * <p>
 * A pending password reset is stored as the SHA-256 hash of its token (never the token itself) with an expiry time.
 * Both are {@code null} when no reset is pending.
 * <p>
 * Relationships:
 * <ul>
 *     <li>{@link Exam}: A user can be assigned to multiple exams.</li>
 * </ul>
 * The entity is mapped to the database table {@code user} and uses automatic timestamp handling.
 * All setters are returning {@code User} itself, enabling method chaining.
 */
@Entity
@Table(name = "`user`")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(length = 31)
    private String firstName;

    @Column(length = 31)
    private String lastName;

    @Column(length = 31)
    private String userName;

    @Column(length = 63)
    private String email;

    @Column(length = 63)
    private String password;

    private boolean enabled;

    @CreationTimestamp
    private LocalDateTime created;

    @UpdateTimestamp
    private LocalDateTime updated;

    @Column(length = 63, unique = true)
    private String passwordResetTokenHash;

    private LocalDateTime passwordResetTokenExpires;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Exam> exams;

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
        this.email = email;
        return this;
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

    public LocalDateTime getCreated() {
        return created;
    }

    public User setCreated(final LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public User setUpdated(final LocalDateTime updated) {
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

    public LocalDateTime getPasswordResetTokenExpires() {
        return passwordResetTokenExpires;
    }

    public User setPasswordResetTokenExpires(final LocalDateTime passwordResetTokenExpires) {
        this.passwordResetTokenExpires = passwordResetTokenExpires;
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
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", enabled=" + enabled +
                ", created=" + created +
                ", updated=" + updated +
                '}';
    }

}
