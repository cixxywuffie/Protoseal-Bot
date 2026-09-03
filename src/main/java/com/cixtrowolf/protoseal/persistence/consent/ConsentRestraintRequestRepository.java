package com.cixtrowolf.protoseal.persistence.consent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsentRestraintRequestRepository extends JpaRepository<ConsentRestraintRequest, Long> {
    Optional<ConsentRestraintRequest> findByToken(String token);
    void deleteByGuildIdAndTargetUserId(String guildId, String targetUserId);
    void deleteByGuildIdAndActorUserId(String guildId, String actorUserId);
    void deleteAllByGuildId(String guildId);
}
