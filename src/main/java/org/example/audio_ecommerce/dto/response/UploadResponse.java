package org.example.audio_ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UploadResponse {
    private String url;           // secure_url
    private String resourceType;  // "image"
    private String publicId;      // tùy: trả thêm nếu muốn
}

