// util/DimensionsUtils.java
package org.example.audio_ecommerce.util;

public final class DimensionsUtils {
    private DimensionsUtils() {}

    // "22 x 9.6 x 9.3 cm" hoặc "22x9.6x9.3"
    public static int[] toLWHcm(String dimensions) {
        if (dimensions == null || dimensions.isBlank()) return new int[]{0,0,0};
        String s = dimensions.toLowerCase().replace("cm","").replace(" ", "");
        String[] parts = s.split("x");
        if (parts.length != 3) return new int[]{0,0,0};
        try {
            double l = Double.parseDouble(parts[0]);
            double w = Double.parseDouble(parts[1]);
            double h = Double.parseDouble(parts[2]);
            return new int[]{(int)Math.ceil(l), (int)Math.ceil(w), (int)Math.ceil(h)};
        } catch (NumberFormatException e) {
            return new int[]{0,0,0};
        }
    }

    /** Gom kiện: length=maxL, width=maxW, height=sumH (xếp chồng) */
    public static int[] aggregateStack(java.util.List<int[]> lwhList) {
        int maxL = 0, maxW = 0, sumH = 0;
        for (int[] lwh : lwhList) {
            maxL = Math.max(maxL, lwh[0]);
            maxW = Math.max(maxW, lwh[1]);
            sumH += lwh[2];
        }
        return new int[]{maxL, maxW, sumH};
    }
}
