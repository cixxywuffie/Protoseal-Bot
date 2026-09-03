package com.cixtrowolf.protoseal.persistence.consent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsentOwnerRequestRepository extends JpaRepository<ConsentOwnerRequest, Long> {

    Optional<ConsentOwnerRequest> findByToken(String token);

    Optional<ConsentOwnerRequest> findByGuildIdAndRequesterUserId(String guildId, String requesterUserId);

    void deleteByGuildIdAndRequesterUserId(String guildId, String requesterUserId);

    void deleteByGuildIdAndOwnerUserId(String guildId, String ownerUserId);

    void deleteAllByGuildId(String guildId);
}
