package com.hieu.auth_service.infrastructure.persistence.jpa.entities;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserJpaEntity extends BaseManualIdEntity {

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private boolean enabled = true;
    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;
    private Instant lastLogin;
    private Integer tokenVersion = 1;

    @Column(name = "google_sub", length = 255)
    private String googleSub;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleJpaEntity> roles = new HashSet<>();

    private Instant updatedAt;

    @Builder
    public UserJpaEntity(String id, String username, String email, String password,
                         String firstName, String lastName, boolean enabled,
                         boolean accountNonExpired, boolean accountNonLocked,
                         boolean credentialsNonExpired, Instant lastLogin,
                         Integer tokenVersion, String googleSub, Set<RoleJpaEntity> roles,
                         Instant createdAt, Instant updatedAt, boolean isNew) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.lastLogin = lastLogin;
        this.tokenVersion = tokenVersion;
        this.googleSub = googleSub;
        this.roles = roles != null ? roles : new HashSet<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isNew = isNew;
    }
}