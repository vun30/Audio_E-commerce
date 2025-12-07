package org.example.audio_ecommerce.util;

import java.math.BigDecimal;

public final class WeightUtils {
    private WeightUtils() {}

    /** kg -> gram (ceil) */
    public static int kgToGram(BigDecimal kg) {
        if (kg == null) return 0;
        return kg.multiply(BigDecimal.valueOf(1000))
                .setScale(0, java.math.RoundingMode.CEILING)
                .intValue();
    }

}
