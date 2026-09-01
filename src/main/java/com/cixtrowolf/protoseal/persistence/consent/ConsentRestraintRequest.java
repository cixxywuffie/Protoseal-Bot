package com.cixtrowolf.protoseal.persistence.consent;

import com.cixtrowolf.protoseal.model.restraint.RestraintZone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "consent_restraint_requests", uniqueConstraints =
        @UniqueConstraint(name = "uk_consent_restraint_request_token", columnNames = "token"))
public class ConsentRestraintRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 36)
    private String token;
    @Column(name = "guild_id", nullable = false, length = 32)
    private String guildId;
    @Column(name = "target_user_id", nullable = false, length = 32)
    private String targetUserId;
    @Column(name = "actor_user_id", nullable = false, length = 32)
    private String actorUserId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RestraintZone zone;
    @Column(nullable = false)
    private int level;
    @Column(nullable = false, length = 128)
    private String name;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected ConsentRestraintRequest() {}

    public ConsentRestraintRequest(String token, String guildId, String targetUserId, String actorUserId,
                                   RestraintZone zone, int level, String name, Instant expiresAt) {
        this.token = token;
        this.guildId = guildId;
        this.targetUserId = targetUserId;
        this.actorUserId = actorUserId;
        this.zone = zone;
        this.level = level;
        this.name = name;
        this.expiresAt = expiresAt;
    }

    public String getToken() { return token; }
    public String getGuildId() { return guildId; }
    public String getTargetUserId() { return targetUserId; }
    public String getActorUserId() { return actorUserId; }
    public RestraintZone getZone() { return zone; }
    public int getLevel() { return level; }
    public String getName() { return name; }
    public Instant getExpiresAt() { return expiresAt; }
}
