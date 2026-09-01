package com.cixtrowolf.protoseal.persistence.consent;

import com.cixtrowolf.protoseal.model.restraint.RestraintZone;
import com.cixtrowolf.protoseal.model.restraint.RestraintLockType;
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
    public enum RequestType { RESTRAINT, LOCK, TIMELOCK }
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
    @Column(name = "channel_id", length = 32)
    private String channelId;
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", length = 16)
    private RequestType requestType;
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private RestraintZone zone;
    @Column
    private int level;
    @Column(length = 128)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "lock_type", length = 16)
    private RestraintLockType lockType;
    @Column(name = "duration_minutes")
    private Long durationMinutes;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected ConsentRestraintRequest() {}

    public ConsentRestraintRequest(String token, String guildId, String targetUserId, String actorUserId,
                                   String channelId,
                                   RestraintZone zone, int level, String name, Instant expiresAt) {
        this.token = token;
        this.guildId = guildId;
        this.targetUserId = targetUserId;
        this.actorUserId = actorUserId;
        this.channelId = channelId;
        this.requestType = RequestType.RESTRAINT;
        this.zone = zone;
        this.level = level;
        this.name = name;
        this.expiresAt = expiresAt;
    }

    public ConsentRestraintRequest(String token, String guildId, String targetUserId, String actorUserId,
                                   String channelId, RequestType requestType, RestraintLockType lockType,
                                   Long durationMinutes, Instant expiresAt) {
        this.token = token;
        this.guildId = guildId;
        this.targetUserId = targetUserId;
        this.actorUserId = actorUserId;
        this.channelId = channelId;
        this.requestType = requestType;
        this.lockType = lockType;
        this.durationMinutes = durationMinutes;
        this.expiresAt = expiresAt;
    }

    public String getToken() { return token; }
    public String getGuildId() { return guildId; }
    public String getTargetUserId() { return targetUserId; }
    public String getActorUserId() { return actorUserId; }
    public String getChannelId() { return channelId; }
    public RequestType getRequestType() { return requestType == null ? RequestType.RESTRAINT : requestType; }
    public RestraintZone getZone() { return zone; }
    public int getLevel() { return level; }
    public String getName() { return name; }
    public RestraintLockType getLockType() { return lockType; }
    public Long getDurationMinutes() { return durationMinutes; }
    public Instant getExpiresAt() { return expiresAt; }
}
