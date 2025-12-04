package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.audio_ecommerce.entity.Enum.ReturnFaultType;

@Data
public class ReturnDisputeResolveRequest {

    @NotNull
    private ReturnFaultType faultType; // CUSTOMER / SHOP

    @NotNull
    private Boolean refundCustomer;

    @Size(max = 1000)
    private String adminNote;
}
