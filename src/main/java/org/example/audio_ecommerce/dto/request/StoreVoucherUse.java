package org.example.audio_ecommerce.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class StoreVoucherUse {
    private UUID storeId;        // shop cần áp mã
    private List<String> codes;  // ["SALE10K","P10",...]
}
