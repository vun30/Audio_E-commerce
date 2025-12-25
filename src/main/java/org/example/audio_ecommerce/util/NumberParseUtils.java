package org.example.audio_ecommerce.util;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NumberParseUtils {
    private NumberParseUtils() {}

    public static BigDecimal extractNumber(String raw) {
        if (raw == null) return null;
        String s = raw.trim().replace(",", ".");
        Matcher m = Pattern.compile("(-?\\d+(?:\\.\\d+)?)").matcher(s);
        if (!m.find()) return null;
        return new BigDecimal(m.group(1));
    }
}
