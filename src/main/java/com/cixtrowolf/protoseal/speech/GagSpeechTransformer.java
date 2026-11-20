package com.cixtrowolf.protoseal.speech;

import com.cixtrowolf.protoseal.model.restraint.GagIntensity;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class GagSpeechTransformer {
    private static final Pattern PROTECTED = Pattern.compile(
            "```[\\s\\S]*?```|`[^`\\r\\n]*`|https?://[^\\s<>]+|<[^>\\r\\n]+>|@everyone|@here");

    public String transform(String text, GagIntensity intensity) {
        if (intensity == GagIntensity.NONE) return text;
        if (intensity == GagIntensity.DEFAULT) {
            throw new IllegalArgumentException("Expected a resolved gag intensity");
        }
        var result = new StringBuilder(text.length());
        var matcher = PROTECTED.matcher(text);
        int last = 0;
        while (matcher.find()) {
            result.append(applyGag(text.substring(last, matcher.start()), intensity));
            result.append(matcher.group());
            last = matcher.end();
        }
        result.append(applyGag(text.substring(last), intensity));
        if (result.length() > 2000) {
            return result.substring(0, 2000);
        }
        return result.toString();
    }

    private String applyGag(String segment, GagIntensity intensity) {
        if (segment.isEmpty()) return segment;
        return switch (intensity) {
            case LIGHT -> applyLight(segment);
            case MEDIUM -> applyMedium(segment);
            case STRONG -> applyStrong(segment);
            case EXTREME -> applyExtreme(segment);
            default -> segment;
        };
    }

    private String applyLight(String text) {
        return text.replaceAll("l", "w")
                .replaceAll("r", "w")
                .replaceAll("L", "W")
                .replaceAll("R", "W")
                .replaceAll("s", "f")
                .replaceAll("S", "F")
                .replaceAll("t", "g")
                .replaceAll("T", "G");
    }

    private String applyMedium(String text) {
        return text.replaceAll("[lr]", "w")
                .replaceAll("[LR]", "W")
                .replaceAll("[bp]", "m")
                .replaceAll("[BP]", "M")
                .replaceAll("[dt]", "n")
                .replaceAll("[DT]", "N")
                .replaceAll("[fsvz]", "h")
                .replaceAll("[FSVZ]", "H");
    }

    private String applyStrong(String text) {
        return text.replaceAll("[abjsvz]", "r")
                .replaceAll("[ABJSVZ]", "R")
                .replaceAll("[dklv]", "f")
                .replaceAll("[DKLV]", "F")
                .replaceAll("[gx]", "n")
                .replaceAll("[GX]", "N")
                .replaceAll("[himu]", "d")
                .replaceAll("[HIMU]", "D")
                .replaceAll("q", "m")
                .replaceAll("Q", "M");
    }

    private String applyExtreme(String text) {
        return text.replaceAll("[BDKTV]", "Mph")
                .replaceAll("[bdktv]", "m")
                .replaceAll("[DJLQR]", "M")
                .replaceAll("[djlqr]", "ph")
                .replaceAll("[AEIOU]", "Mph")
                .replaceAll("[aeiou]", "m")
                .replaceAll("[CVNY]", "Mh")
                .replaceAll("[cvny]", "ph")
                .replaceAll("[WZX]", "Mf")
                .replaceAll("[wzx]", "f")
                .replaceAll("S", "h")
                .replaceAll("s", "m");
    }
}

