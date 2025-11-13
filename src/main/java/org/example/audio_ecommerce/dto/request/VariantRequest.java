package org.example.audio_ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariantRequest {
    @Schema (description = "Tên thuộc tính biến thể (Color / Size / Dung lượng)", example = "Color")
    private String optionName;

    @Schema(description = "Giá trị của biến thể", example = "Black")
    private String optionValue;

    @Schema(description = "Giá riêng của biến thể", example = "1000000")
    private BigDecimal variantPrice;

    @Schema(description = "Tồn kho của biến thể", example = "50")
    private Integer variantStock;

    @Schema(description = "SKU của biến thể", example = "SKU-50-SN001")
    private String variantSku;
}
