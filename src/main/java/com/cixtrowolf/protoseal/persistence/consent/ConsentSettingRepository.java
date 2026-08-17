package com.cixtrowolf.protoseal.persistence.consent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsentSettingRepository extends JpaRepository<ConsentSetting, Long> {

    Optional<ConsentSetting> findByGuildIdAndUserId(String guildId, String userId);
}
