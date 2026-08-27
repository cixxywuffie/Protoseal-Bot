package com.cixtrowolf.protoseal.commands.channel;

import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import com.cixtrowolf.protoseal.persistence.channel.GuildChannelService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.rest.util.Permission;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Callable;

@Component
public class ChannelConfigCommand implements SlashCommandInterface {
    private final GuildChannelService channelService;
    private final GatewayDiscordClient client;

    public ChannelConfigCommand(GuildChannelService channelService, GatewayDiscordClient client) {
        this.channelService = channelService;
        this.client = client;
    }

    @Override
    public String getName() { return "channelconfig"; }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        var guildId = event.getInteraction().getGuildId();
        if (guildId.isEmpty()) return event.reply("This command can only be used in a server.").withEphemeral(true);

        return client.getMemberById(guildId.get(), event.getInteraction().getUser().getId())
                .flatMap(member -> member.getBasePermissions())
                .map(permissions -> permissions.contains(Permission.ADMINISTRATOR))
                .onErrorReturn(false)
                .flatMap(isAdmin -> isAdmin ? execute(event, guildId.get().asString())
                        : event.reply("Only server administrators can configure bot channels.").withEphemeral(true));
    }

    private Mono<Void> execute(ChatInputInteractionEvent event, String guildId) {
        var option = event.getOptions().stream().findFirst();
        if (option.isEmpty()) return event.reply("Choose `block`, `unblock`, `list`, or `clear`.")
                .withEphemeral(true);
        String action = option.get().getName();

        if ("list".equals(action)) {
            return blocking(() -> channelService.listBlocked(guildId))
                    .flatMap(blockedChannels -> {
                String blacklist = blockedChannels.isEmpty() ? "None"
                        : blockedChannels.stream().map(id -> "<#" + id + ">").reduce((a, b) -> a + "\n" + b).orElse("");
                String message = "ProtoSeal is available only in NSFW text channels unless they are blacklisted.\n"
                        + "**Blacklist**\n" + blacklist;
                return event.reply(message).withEphemeral(true);
            });
        }
        if ("clear".equals(action)) {
            return Mono.fromRunnable(() -> channelService.clear(guildId)).subscribeOn(Schedulers.boundedElastic())
                    .then(event.reply("Blacklist cleared. The bot is available in NSFW text channels.")
                            .withEphemeral(true));
        }
        if (!java.util.Set.of("block", "unblock").contains(action)) {
            return event.reply("Unknown channel configuration action.").withEphemeral(true);
        }

        var channelId = option.get().getOption("channel")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> value.asSnowflake().asString());
        if (channelId.isEmpty()) return event.reply("You must specify a channel.").withEphemeral(true);

        return blocking(() -> change(action, guildId, channelId.get()))
                .flatMap(changed -> event.reply(response(action, channelId.get(), changed)).withEphemeral(true));
    }

    private boolean change(String action, String guildId, String channelId) {
        return switch (action) {
            case "block" -> channelService.block(guildId, channelId);
            case "unblock" -> channelService.unblock(guildId, channelId);
            default -> throw new IllegalArgumentException("Unsupported channel action: " + action);
        };
    }

    private String response(String action, String channelId, boolean changed) {
        String mention = "<#" + channelId + ">";
        return switch (action) {
            case "block" -> changed ? mention + " added to the blacklist." : mention + " was already blacklisted.";
            case "unblock" -> changed ? mention + " removed from the blacklist." : mention + " was not blacklisted.";
            default -> "Unknown channel configuration action.";
        };
    }

    private <T> Mono<T> blocking(Callable<T> operation) {
        return Mono.fromCallable(operation).subscribeOn(Schedulers.boundedElastic());
    }
}
