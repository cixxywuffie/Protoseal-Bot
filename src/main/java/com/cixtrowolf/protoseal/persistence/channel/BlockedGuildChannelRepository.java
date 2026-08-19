package com.cixtrowolf.protoseal.persistence.channel;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BlockedGuildChannelRepository extends JpaRepository<BlockedGuildChannel, Long> {
    boolean existsByGuildIdAndChannelId(String guildId, String channelId);
    List<BlockedGuildChannel> findAllByGuildIdOrderByChannelId(String guildId);
    void deleteByGuildIdAndChannelId(String guildId, String channelId);
    void deleteAllByGuildId(String guildId);
}
