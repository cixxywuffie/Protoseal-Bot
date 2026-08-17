package com.cixtrowolf.protoseal;

import com.cixtrowolf.protoseal.listeners.ConsentOwnerButtonListener;
import com.cixtrowolf.protoseal.listeners.GlobalCommandRegistrar;
import com.cixtrowolf.protoseal.listeners.PermalockConfirmationListener;
import com.cixtrowolf.protoseal.listeners.SlashCommandListener;
import discord4j.core.GatewayDiscordClient;
import discord4j.rest.RestClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"bot.token=context-test-token",
		"spring.datasource.url=jdbc:h2:mem:context-test;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.open-in-view=false"
})
class ProtoSealApplicationTests {

	@MockitoBean
	private GatewayDiscordClient gatewayDiscordClient;

	@MockitoBean
	private RestClient restClient;

	@MockitoBean
	private SlashCommandListener slashCommandListener;

	@MockitoBean
	private ConsentOwnerButtonListener consentOwnerButtonListener;

	@MockitoBean
	private PermalockConfirmationListener permalockConfirmationListener;

	@MockitoBean
	private GlobalCommandRegistrar globalCommandRegistrar;

	@Test
	void contextLoads() {
	}

}
