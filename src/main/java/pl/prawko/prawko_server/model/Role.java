package pl.prawko.prawko_server.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import java.util.List;

/**
 * Represents a role entity within the application.
 * <p>
 * A {@code Role} contains a name limited to 7 characters.
 * <p>
 * Relationships:
 * <ul>
 *     <li>{@link User}: A role can be assigned to multiple users.</li>
 * </ul>
 * The entity is mapped to the database table {@code role}.
 * All setters are returning {@code Role} itself, enabling method chaining.
 */
@Entity
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(length = 7)
    private String name;

    @ManyToMany(mappedBy = "roles")
    @JsonBackReference
    private List<User> users;

    public long getId() {
        return id;
    }

    public Role setId(final long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Role setName(final String name) {
        this.name = name;
        return this;
    }

    public List<User> getUsers() {
        return users;
    }

    public Role setUsers(final List<User> users) {
        this.users = users;
        return this;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Role other)) {
            return false;
        }
        return id != 0 && id == other.getId();
    }

    @Override
    public int hashCode() {
        return Role.class.hashCode();
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

}
