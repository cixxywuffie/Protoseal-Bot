package com.cixtrowolf.protoseal.speech;

import com.cixtrowolf.protoseal.model.restraint.GagIntensity;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GagSpeechTransformerTest {
    private final GagSpeechTransformer transformer = new GagSpeechTransformer();

    @Test
    void intensitiesUseTheirFixedCharacterMappings() {
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        Map<GagIntensity, String> expected = Map.of(
                GagIntensity.LIGHT, "abcdefghijkhnnopqhnfuvwxyn",
                GagIntensity.MEDIUM, "afhfefghimmhnnoffhnfunmfnn",
                GagIntensity.STRONG, "mfhfefghimmhnnmffnnfunmfnn",
                GagIntensity.EXTREME, "mfhfmfghnmmhnnmffnnfmnmfnn");
        expected.forEach((intensity, output) -> {
            assertEquals(output, transformer.transform(alphabet, intensity), intensity.name());
        });
        assertEquals("ABCDEFGHIJKHNNOPQHNFUVWXYN", transformer.transform(alphabet.toUpperCase(Locale.ROOT), GagIntensity.LIGHT));
        assertEquals("AFHFEFGHIMMHNNOFFHNFUNMFNN", transformer.transform(alphabet.toUpperCase(Locale.ROOT), GagIntensity.MEDIUM));
        assertEquals("MFHFEFGHIMMHNNMFFNNFUNMFNN", transformer.transform(alphabet.toUpperCase(Locale.ROOT), GagIntensity.STRONG));
        assertEquals("MFHFMFGHNMMHNNMFFNNFMNMFNN", transformer.transform(alphabet.toUpperCase(Locale.ROOT), GagIntensity.EXTREME));
    }

    @Test
    void eachIntensityHasPredictableReadableFixtures() {
        String text = "Hello world!";
        assertEquals(text, transformer.transform(text, GagIntensity.NONE));
        assertEquals("Hehho wohhd!", transformer.transform(text, GagIntensity.LIGHT));
        assertEquals("Hehho mohhf!", transformer.transform(text, GagIntensity.MEDIUM));
        assertEquals("Hehhm mmnhf!", transformer.transform(text, GagIntensity.STRONG));
        assertEquals("Hmhhm mmnhf!", transformer.transform(text, GagIntensity.EXTREME));
        assertThrows(IllegalArgumentException.class, () -> transformer.transform(text, GagIntensity.DEFAULT));
    }

    @Test
    void strongAndExtremePreserveCaseAndDoNotCascadeReplacements() {
        for (var intensity : new GagIntensity[]{GagIntensity.STRONG, GagIntensity.EXTREME}) {
            assertEquals("MmMm FfNn!", transformer.transform("AaWw BbZz!", intensity));
            assertEquals(intensity == GagIntensity.STRONG ? "hmhm nunfm" : "hmhm nmnfm",
                    transformer.transform("hola mundo", intensity));
        }
    }

    @Test
    void preservesProtectedSegmentsWhitespaceAndUnicodeAtEveryLevel() {
        String protectedContent = "<@123> <@&456> <#789> <:wave:123> @everyone @here "
                + "https://example.com/hello `literal` ```code\nLine``` é 🦌";
        for (GagIntensity intensity : GagIntensity.values()) {
            if (intensity == GagIntensity.DEFAULT) continue;
            String result = transformer.transform("Hello\n" + protectedContent, intensity);
            assertTrue(result.endsWith("\n" + protectedContent), intensity.name());
            assertEquals(result, transformer.transform("Hello\n" + protectedContent, intensity));
        }
    }

    @Test
    void outputLengthNeverExceedsDiscordLimitForMaximumLengthInput() {
        String content = "A".repeat(2000);
        for (GagIntensity intensity : GagIntensity.values()) {
            if (intensity == GagIntensity.DEFAULT) continue;
            assertEquals(content.length(), transformer.transform(content, intensity).length());
        }
    }
}
