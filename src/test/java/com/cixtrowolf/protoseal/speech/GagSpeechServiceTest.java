package com.cixtrowolf.protoseal.speech;

import com.cixtrowolf.protoseal.model.restraint.GagIntensity;
import com.cixtrowolf.protoseal.model.restraint.RestraintDefinition;
import com.cixtrowolf.protoseal.model.restraint.RestraintZone;
import com.cixtrowolf.protoseal.persistence.consent.ConsentService;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintState;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GagSpeechServiceTest {
    private final RestraintStateRepository repository = mock(RestraintStateRepository.class);
    private final ConsentService consent = mock(ConsentService.class);
    private final GagSpeechTransformer transformer = new GagSpeechTransformer();
    private final GagSpeechService service = new GagSpeechService(repository, consent, transformer);

    @Test
    void defaultMappingsCoverEveryNormalGag() {
        GagIntensity[] expected = {GagIntensity.MEDIUM, GagIntensity.LIGHT, GagIntensity.EXTREME,
                GagIntensity.MEDIUM, GagIntensity.EXTREME, GagIntensity.LIGHT,
                GagIntensity.STRONG, GagIntensity.MEDIUM, GagIntensity.STRONG, GagIntensity.STRONG};
        for (int level = 1; level <= expected.length; level++) {
            assertEquals(expected[level - 1], GagIntensity.DEFAULT.resolve(level), "level " + level);
        }
    }

    @Test
    void reindeerOverridesEvenNoneAndRetainsNoOriginalText() {
        assertEquals("reindeer muzzle", RestraintDefinition.GAG
                .getLevel(RestraintDefinition.REINDEER_MUZZLE_LEVEL).name());
        gag(RestraintDefinition.REINDEER_MUZZLE_LEVEL);
        assertEquals(Optional.of("Hrrn! Hrrn!"), service.transform("guild", "user", "Secret words"));
        verifyNoInteractions(consent);
    }

    @Test
    void noGagOrNonePreferenceDoesNotReplaceMessage() {
        assertTrue(service.transform("guild", "user", "hello").isEmpty());
        gag(1);
        when(consent.getGagIntensity("guild", "user")).thenReturn(GagIntensity.NONE);
        assertTrue(service.transform("guild", "user", "hello").isEmpty());
    }

    @Test
    void customIntensityOverridesDefault() {
        gag(2);
        when(consent.getGagIntensity("guild", "user")).thenReturn(GagIntensity.EXTREME);
        String text = "hello there";
        assertEquals(Optional.of(transformer.transform(text, GagIntensity.EXTREME)),
                service.transform("guild", "user", text));
    }

    @Test
    void transformerProtectsDiscordTokensUrlsCodeAndUnicode() {
        String text = "Hello <@123> @everyone <:emoji:123> https://example.com/a `code` ```block``` 🦌";
        String result = transformer.transform(text, GagIntensity.EXTREME);
        assertTrue(result.contains("<@123> @everyone <:emoji:123> https://example.com/a `code` ```block``` 🦌"));
        assertEquals(result, transformer.transform(text, GagIntensity.EXTREME));
        assertEquals(text, transformer.transform(text, GagIntensity.NONE));
        assertTrue(result.length() <= 2000);
    }

    @Test
    void strongerIntensitiesTransformTextPredictably() {
        String text = "The quick brown fox jumps over the lazy dog";
        String prev = text;
        for (GagIntensity intensity : new GagIntensity[]{GagIntensity.LIGHT, GagIntensity.MEDIUM,
                GagIntensity.STRONG, GagIntensity.EXTREME}) {
            String result = transformer.transform(text, intensity);
            assertNotEquals(prev, result);
            prev = result;
        }
    }

    private void gag(int level) {
        when(repository.findByGuildIdAndUserIdAndZone("guild", "user", RestraintZone.GAG))
                .thenReturn(Optional.of(new RestraintState("guild", "user", RestraintZone.GAG, level, "gag")));
    }
}
