package org.example.audio_ecommerce.dto.request;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopRegisterAudioXtraRequest {
    private UUID campaignId;
}