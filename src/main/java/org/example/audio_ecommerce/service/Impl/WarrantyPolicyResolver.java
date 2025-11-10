package org.example.audio_ecommerce.service.Impl;

import org.example.audio_ecommerce.entity.Product;

public class WarrantyPolicyResolver {
    /** Ưu tiên đọc từ Product.warrantyPeriod (chuỗi “24 tháng”) -> số tháng; default 12 */
    public static int resolveMonths(Product p) {
        if (p == null) return 12;
        String s = p.getWarrantyPeriod();
        if (s != null) {
            String digits = s.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                try { return Math.max(1, Integer.parseInt(digits)); } catch (Exception ignored) {}
            }
        }
        return 12;
    }
}
