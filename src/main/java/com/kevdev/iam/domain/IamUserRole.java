package com.kevdev.iam.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "iam_user_role")
public class IamUserRole {

    @EmbeddedId
    private IamUserRoleId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private IamUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IamUserRole() {}

    static IamUserRole link(IamUser user, Role role) {
        IamUserRole ur = new IamUserRole();
        ur.user = user;
        ur.role = role;
        ur.id = new IamUserRoleId(user.getId(), role.getId());
        ur.tenantId = user.getTenant().getId();
        ur.createdAt = Instant.now();
        return ur;
    }

    public Role getRole() {
        return role;
    }
}

