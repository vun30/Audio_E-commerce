package org.example.audio_ecommerce.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class VoucherCodeGeneratorTest {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{4}$");

    @Test
    @DisplayName("Sinh ra mã voucher đúng định dạng 4 ký tự A-Z0-9")
    void testGenerateCodeFormat() {
        for (int i = 0; i < 100; i++) {
            String code = VoucherCodeGenerator.generateCode();
            assertNotNull(code);
            assertEquals(4, code.length());
            assertTrue(CODE_PATTERN.matcher(code).matches(), "Mã không đúng định dạng: " + code);
        }
    }

    @Test
    @DisplayName("Sinh nhiều mã đảm bảo xác suất trùng rất thấp (không trùng trong 10.000 lần)")
    void testGenerateCodeUniquenessApproximate() {
        Set<String> codes = new HashSet<>();
        int total = 10_000; // Không đảm bảo tuyệt đối nhưng kỳ vọng không collision trong tập nhỏ này
        for (int i = 0; i < total; i++) {
            String code = VoucherCodeGenerator.generateCode();
            assertTrue(codes.add(code), "Trùng mã: " + code);
        }
        assertEquals(total, codes.size());
    }

    @Test
    @DisplayName("Sinh mã với prefix")
    void testGenerateCodeWithPrefix() {
        String code = VoucherCodeGenerator.generateCodeWithPrefix("sale");
        assertNotNull(code);
        assertTrue(code.startsWith("SALE-"));
        String[] parts = code.split("-");
        assertEquals(2, parts.length);
        assertTrue(CODE_PATTERN.matcher(parts[1]).matches());
    }
}

