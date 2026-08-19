package com.cixtrowolf.protoseal.commands.help;

import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Set;

@Component
public class DonateCommand implements SlashCommandInterface {

    private final String donation_url;

    public DonateCommand( @Value("${bot.donation-url:}") String donation_url) {
        this.donation_url = validateDonationUrl(donation_url);
    }

    @Override
    public String getName() {
        return "donate";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        if (donation_url.isBlank()) {
            return event.reply(
                    "Donations are not currently configured."
            ).withEphemeral(true);
        }

        return event.reply("**Support ProtoSeal**\n"
                        + "Donations are optional and if you enjoy the project, "
                        + "you can help cover its hosting and development costs here:\n"
                        + donation_url)
                .withEphemeral(true);
    }

    private String validateDonationUrl(String value) {
        if (value.isBlank()) {
            return "";
        }
        URI uri = URI.create(value);

        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException(
                    "DONATION_URL must be a valid HTTPS URL"
            );
        }

        return uri.toString();
    }
}
