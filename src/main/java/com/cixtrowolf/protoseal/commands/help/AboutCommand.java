package com.cixtrowolf.protoseal.commands.help;

import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AboutCommand implements SlashCommandInterface {

    private static final int EMBED_COLOR = 0x9B59B6;
    private static final String SOURCE_URL = "https://github.com/cixxywuffie/kinksterBot";
    private static final String AUTHOR_URL = "https://bsky.app/profile/cixtrowolf.com";

    @Override
    public String getName() {
        return "about";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        var embed = EmbedCreateSpec.builder()
                .color(Color.of(EMBED_COLOR))
                .title("About ProtoSeal")
                .description("A consent-focused Discord bot for adult restraint roleplay. "
                        + "ProtoSeal provides consent controls, owner invitations, restraints, locks and a safeword.")
                .addField("Source code", "[View the repository](" + SOURCE_URL + ")", false)
                .addField("Author", "[CixtroWolf](" + AUTHOR_URL + ")", true)
                .addField("License", "GNU AGPL-3.0", true)
                .footer("Use ProtoSeal only between consenting adults.", null)
                .build();

        return event.reply().withEmbeds(embed).withEphemeral(true);
    }
}
