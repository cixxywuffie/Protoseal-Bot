package com.cixtrowolf.protoseal.persistence.restraint;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cixtrowolf.protoseal.model.restraint.RestraintZone;

import java.util.List;
import java.util.Optional;

public interface RestraintStateRepository extends JpaRepository<RestraintState, Long> {

    Optional<RestraintState> findByGuildIdAndUserIdAndZone(String guildId, String userId, RestraintZone zone);

    List<RestraintState> findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc(
            String guildId, String userId, int level);

    void deleteByGuildIdAndUserId(String guildId, String userId);

    void deleteAllByGuildId(String guildId);

    List<RestraintState> findByGuildIdAndLockedByUserId(String guildId, String lockedByUserId);
}
