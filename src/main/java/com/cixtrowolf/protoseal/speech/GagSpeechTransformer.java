package com.cixtrowolf.protoseal.speech;

import com.cixtrowolf.protoseal.model.restraint.GagIntensity;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class GagSpeechTransformer {
    private static final Pattern PROTECTED = Pattern.compile(
            "```[\\s\\S]*?```|`[^`\\r\\n]*`|https?://[^\\s<>]+|<[^>\\r\\n]+>|@everyone|@here");
    private static final Pattern LETTERS = Pattern.compile("[a-zA-Z]");
    private static final String LIGHT_MAPPING =  "abcdefghijkhnnopqhnfuvwxyn";
    private static final String MEDIUM_MAPPING = "afhfefghimmhnnoffhnfunmfnn";
    private static final String STRONG_MAPPING = "mfhfefghimmhnnmffnnfunmfnn";
    private static final String EXTREME_MAPPING = "mfhfmfghnmmhnnmffnnfmnmfnn";

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
            case LIGHT -> applyMapping(segment, LIGHT_MAPPING);
            case MEDIUM -> applyMapping(segment, MEDIUM_MAPPING);
            case STRONG -> applyMapping(segment, STRONG_MAPPING);
            case EXTREME -> applyMapping(segment, EXTREME_MAPPING);
            default -> segment;
        };
    }

    private String applyMapping(String text, String mapping) {
        return LETTERS.matcher(text).replaceAll(mr -> {
            char c = mr.group().charAt(0);
            char mapped = mapping.charAt(Character.toLowerCase(c) - 'a');
            return String.valueOf(Character.isUpperCase(c) ? Character.toUpperCase(mapped) : mapped);
        });
    }
}
