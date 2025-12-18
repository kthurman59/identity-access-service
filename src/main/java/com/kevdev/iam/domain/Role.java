package com.kevdev.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "iam_role")
public class Role {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    private Tenant tenant;

    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @ManyToMany
    @JoinTable(
        name = "iam_role_permission",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    protected Role() {}

    public static Role create(Tenant tenant, String name, String description) {
        Role r = new Role();
        r.id = UUID.randomUUID();
        r.tenant = tenant;
        r.name = name;
        r.description = description;
        return r;
    }

    public static Role create(Tenant tenant, String name) {
        return create(tenant, name, null);
    }

    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Set<Permission> getPermissions() { return permissions; }
}

