package org.example.audio_ecommerce.dto.request;

import lombok.Data;

@Data
public class ConfirmSuccessRequest {
    private String photoUrl; // đã up sẵn (Cloudinary, S3…)
    private Boolean installed; // có lắp đặt hay không
    private String note;
}
