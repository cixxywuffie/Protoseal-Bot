package com.cixtrowolf.protoseal.commands.help;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class HelpCommand implements SlashCommandInterface {

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.reply("**Available commands**\n"
                + "`/about` — shows information about ProtoSeal and its source code.\n"
                + "`/consent mode [owner]` — configures who can manage your restraints.\n"
                + "`/adminhelp` — shows administrator commands.\n"
                + "`/armcuffs target level` — changes arm restraints.\n"
                + "`/legcuffs target level` — changes leg restraints.\n"
                + "`/gag target level` — changes gag restraints.\n"
                + "`/hood target level` — changes hood restraints.\n"
                + "`/straitjacket target level` — changes straitjacket restraints.\n"
                + "`/suits target level` — changes restraint suits.\n"
                + "`/mitts target level` — changes restraint mitts.\n"
                + "`/chastity target level` — changes a chastity restraint.\n"
                + "`/blindfold target level` — changes a blindfold restraint.\n"
                + "`/lock target [type]` — locks all active restraints.\n"
                + "`/rdstatus [target]` — shows a user's active restraints.\n"
                + "`/safeword [target]` — resets restraint states; resetting another user requires administrator permission.\n\n"
                + "Use these commands only with the other person's consent.")
                .withEphemeral(true);
    }
}
