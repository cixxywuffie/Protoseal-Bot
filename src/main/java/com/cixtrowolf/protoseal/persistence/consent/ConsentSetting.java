package com.cixtrowolf.protoseal.persistence.consent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "consent_settings", uniqueConstraints = @UniqueConstraint(
        name = "uk_consent_setting_guild_user", columnNames = {"guild_id", "user_id"}))
public class ConsentSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guild_id", nullable = false, length = 32)
    private String guildId;

    @Column(name = "user_id", nullable = false, length = 32)
    private String userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private ConsentMode mode;

    @Column(name = "owner_user_id", length = 32)
    private String ownerUserId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ConsentSetting() {
    }

    public ConsentSetting(String guildId, String userId, ConsentMode mode, String ownerUserId) {
        this.guildId = guildId;
        this.userId = userId;
        update(mode, ownerUserId);
    }

    public void update(ConsentMode mode, String ownerUserId) {
        this.mode = mode;
        this.ownerUserId = mode == ConsentMode.OWNER ? ownerUserId : null;
        this.updatedAt = Instant.now();
    }

    public ConsentMode getMode() {
        return mode;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }
}
