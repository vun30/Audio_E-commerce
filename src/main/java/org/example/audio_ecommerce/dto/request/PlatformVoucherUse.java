package org.example.audio_ecommerce.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class PlatformVoucherUse {
    private UUID campaignProductId; // hoặc mã định danh voucher toàn sàn bạn đang dùng (PlatformCampaignProduct.id)
    private Integer quantity; // mặc định 1; để sau này có limit / multiples
}
