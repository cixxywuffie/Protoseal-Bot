package com.cixtrowolf.protoseal.config;

import discord4j.gateway.intent.Intent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    void enablesGuildMembersIntentForMemberLeaveEvents() {
        assertTrue(AppConfig.ENABLED_INTENTS.contains(Intent.GUILD_MEMBERS));
    }
}
