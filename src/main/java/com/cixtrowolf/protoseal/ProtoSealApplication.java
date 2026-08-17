package com.cixtrowolf.protoseal;

import discord4j.core.GatewayDiscordClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProtoSealApplication {

	public static void main(String[] args) {
		var context = SpringApplication.run(ProtoSealApplication.class, args);
		context.getBean(GatewayDiscordClient.class).onDisconnect().block();
	}

}
