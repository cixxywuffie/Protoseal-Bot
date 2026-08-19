package com.cixtrowolf.protoseal.commands.help;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DonateCommandTest {

    @Test
    void exposesTheDonateCommandName() {
        assertEquals("donate", new DonateCommand("").getName());
    }

    @Test
    void rejectsNonHttpsDonationUrls() {
        assertThrows(IllegalArgumentException.class,
                () -> new DonateCommand("http://example.com/donate"));
        assertThrows(IllegalArgumentException.class,
                () -> new DonateCommand("not-a-url"));
    }

    @Test
    void repliesWithTheConfiguredDonationUrlEphemerally() {
        var event = mock(ChatInputInteractionEvent.class);
        var reply = mock(InteractionApplicationCommandCallbackReplyMono.class);
        when(event.reply(anyString())).thenReturn(reply);
        when(reply.withEphemeral(true)).thenReturn(reply);

        new DonateCommand("https://example.com/donate").handle(event);

        var message = ArgumentCaptor.forClass(String.class);
        verify(event).reply(message.capture());
        verify(reply).withEphemeral(true);
        assertTrue(message.getValue().contains("https://example.com/donate"));
    }

    @Test
    void explainsWhenDonationsAreNotConfigured() {
        var event = mock(ChatInputInteractionEvent.class);
        var reply = mock(InteractionApplicationCommandCallbackReplyMono.class);
        when(event.reply(anyString())).thenReturn(reply);
        when(reply.withEphemeral(true)).thenReturn(reply);

        new DonateCommand("").handle(event);

        verify(event).reply("Donations are not currently configured.");
        verify(reply).withEphemeral(true);
    }
}
