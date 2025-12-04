package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.audio_ecommerce.entity.Enum.ReturnReasonType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ReturnRequestCreateRequest {

    @NotNull
    private UUID orderItemId;

    @NotNull
    private UUID productId;

    @NotNull
    private BigDecimal itemPrice;

    @NotNull
    private ReturnReasonType reasonType;

    @Size(max = 1000)
    private String reason;

    private String customerVideoUrl;

    private List<String> customerImageUrls;
}
