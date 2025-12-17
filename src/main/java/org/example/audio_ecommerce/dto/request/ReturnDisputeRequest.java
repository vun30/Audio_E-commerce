package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ReturnDisputeRequest {

    @NotBlank
    private String reason;

    private String videoUrl;

    private List<String> imageUrls;
}
