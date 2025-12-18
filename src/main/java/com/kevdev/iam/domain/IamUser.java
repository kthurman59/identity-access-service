package com.kevdev.iam.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import java.util.UUID;

@Entity
@Table(name = "iam_user")
public class IamUser {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(optional = false)
    private Tenant tenant;

    private String username;
    private String email;
    private String passwordHash;

    protected IamUser() {}

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private final java.util.Set<IamUserRole> userRoles = new java.util.HashSet<>();

    public void assignRole(Role role) {
        userRoles.add(IamUserRole.link(this, role));
    }

    public java.util.Set<Role> getRoles() {
        return userRoles.stream().map(IamUserRole::getRole).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }


    public static IamUser create(Tenant tenant, String username, String email, String passwordHash) {
        IamUser u = new IamUser();
        u.tenant = tenant;
        u.username = username;
        u.email = email;
        u.passwordHash = passwordHash;
        return u;
    }

    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
}

