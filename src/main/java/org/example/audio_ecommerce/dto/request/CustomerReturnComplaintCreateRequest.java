package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CustomerReturnComplaintCreateRequest {

    @NotNull
    private UUID returnRequestId;

    @Size(max = 1000)
    private String reason;

    private String customerVideoUrl;

    private List<String> customerImageUrls;
}
