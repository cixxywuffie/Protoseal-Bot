package com.cixtrowolf.protoseal.speech;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Webhook;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.WebhookExecuteSpec;
import discord4j.rest.util.AllowedMentions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class WebhookProxyService {
    private static final String WEBHOOK_NAME = "ProtoSeal speech";

    public Mono<Message> publish(TextChannel channel, Snowflake botId, Member author, String content) {
        return channel.getWebhooks()
                .filter(webhook -> webhook.getName().filter(WEBHOOK_NAME::equals).isPresent()
                        && webhook.getCreator().map(user -> user.getId().equals(botId)).orElse(false)
                        && webhook.getToken().isPresent())
                .next()
                .switchIfEmpty(Mono.defer(() -> channel.createWebhook(WEBHOOK_NAME)))
                .flatMap(webhook -> execute(webhook, author, content));
    }

    private Mono<Message> execute(Webhook webhook, Member author, String content) {
        var spec = WebhookExecuteSpec.create()
                .withContent(content)
                .withUsername(author.getDisplayName())
                .withAvatarUrl(author.getEffectiveAvatarUrl())
                .withAllowedMentions(AllowedMentions.suppressAll())
                .withFlags(Message.Flag.SUPPRESS_EMBEDS);
        return webhook.executeAndWait(spec);
    }
}
