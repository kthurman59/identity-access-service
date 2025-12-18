package com.kevdev.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "iam_tenant")
public class Tenant {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Property name matches repository method: findBySlug
    // Column name matches what the table should have
    @Column(name = "key", nullable = false, unique = true)
    private String key;

    @Column(name = "name", nullable = false)
    private String name;

    protected Tenant() {
        // for JPA
    }

    public static Tenant create(String key, String name) {
        Tenant t = new Tenant();
        t.id = UUID.randomUUID();
        t.key = key;
        t.name = name;
        return t;
    }

    public UUID getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }
}

