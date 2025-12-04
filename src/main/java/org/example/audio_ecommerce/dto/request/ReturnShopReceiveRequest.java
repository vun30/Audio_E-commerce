package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReturnShopReceiveRequest {

    @NotNull
    private Boolean receivedCorrect;  // true = nhận đúng → REFUNDED, false = DISPUTE

    private String shopVideoUrl;

    private List<String> shopImageUrls;

    private String shopDisputeReason;
}
