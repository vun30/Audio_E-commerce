// CheckoutItemRequest vẫn giữ nguyên
// Thêm optional addressId cho checkout COD
package org.example.audio_ecommerce.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter @Setter
public class CheckoutCODRequest {
    private List<CheckoutItemRequest> items;
    private UUID addressId;
    private  String message; // optional; nếu null thì lấy default address
    private List<StoreVoucherUse> storeVouchers;
    private Integer serviceTypeId;
}
