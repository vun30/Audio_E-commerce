package org.example.audio_ecommerce.dto.request;

import lombok.Data;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class UpdateCartItemQtyWithVoucherRequest {

    private UUID cartItemId;
    private Integer quantity;

    // giống như body checkout
    private List<StoreVoucherUse> storeVouchers;       // optional
    private List<PlatformVoucherUse> platformVouchers; // optional

    // serviceType cho từng store nếu cần tính phí GHN (optional)
    private Map<UUID, Integer> serviceTypeIds; // storeId -> serviceTypeId
}
