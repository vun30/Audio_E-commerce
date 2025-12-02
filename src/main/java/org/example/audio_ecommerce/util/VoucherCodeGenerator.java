package org.example.audio_ecommerce.util;

import java.security.SecureRandom;

/**
 * Utility class để sinh mã voucher ngẫu nhiên 4 ký tự (A-Z, 0-9)
 */
public class VoucherCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 4;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Sinh mã voucher ngẫu nhiên 4 ký tự
     * @return Mã voucher (ví dụ: "A3K9", "X7Y2")
     */
    public static String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(index));
        }
        return code.toString();
    }

    /**
     * Sinh mã voucher unique với prefix (tùy chọn)
     * @param prefix Prefix cho mã voucher (ví dụ: "SALE", "NEW")
     * @return Mã voucher với prefix (ví dụ: "SALE-A3K9")
     */
    public static String generateCodeWithPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return generateCode();
        }
        return prefix.toUpperCase() + "-" + generateCode();
    }
}

