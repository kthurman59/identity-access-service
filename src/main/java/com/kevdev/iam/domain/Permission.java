package com.kevdev.iam.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "iam_permission")
public class Permission {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @Column(length = 255)
    private String description;

    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected Permission() {}

    public Permission(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
}

