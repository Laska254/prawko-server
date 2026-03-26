package pl.prawko.prawko_server.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
 * and system-related attributes like whether the account is enabled, the creation timestamp and the last update timestamp.
 * <p>
 * Relationships:
 * <ul>
 *     <li>{@link Role}: A user can have multiple roles, defining their permissions in the system.</li>
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

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "role_id", referencedColumnName = "id")}
    )
    private List<Role> roles;

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

    public List<Role> getRoles() {
        return roles;
    }

    public User setRoles(final List<Role> roles) {
        this.roles = roles;
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
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final var user = (User) object;
        return id == user.id
                && Objects.equals(firstName, user.firstName)
                && Objects.equals(lastName, user.lastName)
                && Objects.equals(userName, user.userName)
                && Objects.equals(email, user.email)
                && enabled == user.enabled
                && created.equals(user.created)
                && updated.equals(user.updated)
                && roles.equals(user.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, userName, email, enabled, created, updated, roles);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", enabled=" + enabled +
                ", created=" + created +
                ", updated=" + updated +
                ", roles=" + roles +
                ", exams=" + exams +
                '}';
    }

}
