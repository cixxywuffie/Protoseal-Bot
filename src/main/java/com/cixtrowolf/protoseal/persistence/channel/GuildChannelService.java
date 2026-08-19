package com.cixtrowolf.protoseal.persistence.channel;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class GuildChannelService {
    private final GuildChannelRepository repository;
    private final BlockedGuildChannelRepository blockedRepository;

    public GuildChannelService(GuildChannelRepository repository,
                               BlockedGuildChannelRepository blockedRepository) {
        this.repository = repository;
        this.blockedRepository = blockedRepository;
    }

    public boolean isAllowed(String guildId, String channelId) {
        return repository.existsByGuildIdAndChannelId(guildId, channelId);
    }

    public boolean isBlocked(String guildId, String channelId) {
        return blockedRepository.existsByGuildIdAndChannelId(guildId, channelId);
    }

    @Transactional
    public boolean add(String guildId, String channelId) {
        boolean wasBlocked = blockedRepository.existsByGuildIdAndChannelId(guildId, channelId);
        if (wasBlocked) blockedRepository.deleteByGuildIdAndChannelId(guildId, channelId);
        if (repository.existsByGuildIdAndChannelId(guildId, channelId)) return wasBlocked;
        repository.save(new GuildChannel(guildId, channelId));
        return true;
    }

    @Transactional
    public boolean remove(String guildId, String channelId) {
        if (!repository.existsByGuildIdAndChannelId(guildId, channelId)) return false;
        repository.deleteByGuildIdAndChannelId(guildId, channelId);
        return true;
    }

    public List<String> list(String guildId) {
        return repository.findAllByGuildIdOrderByChannelId(guildId).stream()
                .map(GuildChannel::getChannelId).toList();
    }

    @Transactional
    public boolean block(String guildId, String channelId) {
        boolean wasAllowed = repository.existsByGuildIdAndChannelId(guildId, channelId);
        if (wasAllowed) repository.deleteByGuildIdAndChannelId(guildId, channelId);
        if (blockedRepository.existsByGuildIdAndChannelId(guildId, channelId)) return wasAllowed;
        blockedRepository.save(new BlockedGuildChannel(guildId, channelId));
        return true;
    }

    @Transactional
    public boolean unblock(String guildId, String channelId) {
        if (!blockedRepository.existsByGuildIdAndChannelId(guildId, channelId)) return false;
        blockedRepository.deleteByGuildIdAndChannelId(guildId, channelId);
        return true;
    }

    public List<String> listBlocked(String guildId) {
        return blockedRepository.findAllByGuildIdOrderByChannelId(guildId).stream()
                .map(BlockedGuildChannel::getChannelId).toList();
    }

    @Transactional
    public void clear(String guildId) {
        repository.deleteAllByGuildId(guildId);
        blockedRepository.deleteAllByGuildId(guildId);
    }
}
