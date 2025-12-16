package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AdminWithdrawMarkPaidRequest {
    private String payoutRef;
    private String note;
    @NotEmpty
    private List<@NotBlank String> proofUrls;
}

