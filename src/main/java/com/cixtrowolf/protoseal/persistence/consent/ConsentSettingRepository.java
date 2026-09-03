package com.cixtrowolf.protoseal.persistence.consent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ConsentSettingRepository extends JpaRepository<ConsentSetting, Long> {

    Optional<ConsentSetting> findByGuildIdAndUserId(String guildId, String userId);

    List<ConsentSetting> findByGuildIdAndOwnerUserId(String guildId, String ownerUserId);

    void deleteByGuildIdAndUserId(String guildId, String userId);

    void deleteAllByGuildId(String guildId);
}
