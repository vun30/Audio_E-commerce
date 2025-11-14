package org.example.audio_ecommerce.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiQueryRequest {
    private String UserName;
    private String userId;
    private String message;
}