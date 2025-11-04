package org.example.audio_ecommerce.dto.request;

import lombok.Data;

@Data
public class PushLocationRequest {
    private Double latitude;
    private Double longitude;
    private Double speedKmh;     // optional
    private String addressText;  // optional (client reverse geocode)
}
