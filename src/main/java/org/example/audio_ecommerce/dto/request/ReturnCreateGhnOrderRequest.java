// dto/request/ReturnCreateGhnOrderRequest.java
package org.example.audio_ecommerce.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnCreateGhnOrderRequest {
    /**
     * ID ca lấy hàng GHN, ví dụ:
     * 2, 3, 4 ... (FE lấy từ API pick-shift của GHN)
     */
    private Integer pickShiftId;
}
