package org.example.audio_ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.GhnStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGhnStatusRequest {

    @NotNull
    private GhnStatus status;
}
