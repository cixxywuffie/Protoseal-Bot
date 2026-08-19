package com.cixtrowolf.protoseal.persistence.restraint;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.cixtrowolf.protoseal.model.restraint.RestraintZone;
import com.cixtrowolf.protoseal.model.restraint.RestraintLockType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "restraint_states", uniqueConstraints = @UniqueConstraint(
        name = "uk_restraint_state_guild_user_zone",
        columnNames = {"guild_id", "user_id", "zone"}))
public class RestraintState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guild_id", nullable = false, length = 32)
    private String guildId;

    @Column(name = "user_id", nullable = false, length = 32)
    private String userId;

    @Convert(converter = RestraintZoneConverter.class)
    @Column(nullable = false)
    private RestraintZone zone;

    @Column(nullable = false)
    private int level;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "lock_type", length = 16)
    private RestraintLockType lockType;

    @Column(name = "locked_by_user_id", length = 32)
    private String lockedByUserId;

    @Column(nullable = false)
    private String name;

    protected RestraintState() {
    }

    public RestraintState(String guildId, String userId, RestraintZone zone, int level, String name) {
        this.guildId = guildId;
        this.userId = userId;
        this.zone = zone;
        this.level = level;
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public void updateLevel(int level, String name) {
        this.level = level;
        this.name = name;
        if (level == 0) {
            removeLock();
        }
        this.updatedAt = Instant.now();
    }

    public void applyLock(RestraintLockType lockType, String lockedByUserId) {
        this.lockType = lockType;
        this.lockedByUserId = lockedByUserId;
        this.updatedAt = Instant.now();
    }

    public void removeLock() {
        this.lockType = null;
        this.lockedByUserId = null;
        this.updatedAt = Instant.now();
    }

    public RestraintZone getZone() {
        return zone;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public RestraintLockType getLockType() {
        return lockType;
    }

    public String getLockedByUserId() {
        return lockedByUserId;
    }

    public boolean isLocked() {
        return lockType != null && lockedByUserId != null;
    }
}
