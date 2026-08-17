package com.cixtrowolf.protoseal.persistence.consent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "consent_owner_requests", uniqueConstraints = {
        @UniqueConstraint(name = "uk_consent_owner_request_token", columnNames = "token"),
        @UniqueConstraint(name = "uk_consent_owner_request_guild_user", columnNames = {"guild_id", "requester_user_id"})
})
public class ConsentOwnerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String token;

    @Column(name = "guild_id", nullable = false, length = 32)
    private String guildId;

    @Column(name = "requester_user_id", nullable = false, length = 32)
    private String requesterUserId;

    @Column(name = "owner_user_id", nullable = false, length = 32)
    private String ownerUserId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected ConsentOwnerRequest() {
    }

    public ConsentOwnerRequest(String token, String guildId, String requesterUserId,
                               String ownerUserId, Instant expiresAt) {
        update(token, ownerUserId, expiresAt);
        this.guildId = guildId;
        this.requesterUserId = requesterUserId;
    }

    public void update(String token, String ownerUserId, Instant expiresAt) {
        this.token = token;
        this.ownerUserId = ownerUserId;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getRequesterUserId() {
        return requesterUserId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
