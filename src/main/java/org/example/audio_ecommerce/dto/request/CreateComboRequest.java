package org.example.audio_ecommerce.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateComboRequest {
    private UUID comboProductId; // product đại diện
    private List<UUID> includedProductIds; // danh sách sản phẩm con
    private String comboImageUrl;
    private String categoryName;
    private String categoryIconUrl;
    private String comboDescription;
    private BigDecimal  comboPrice;
}