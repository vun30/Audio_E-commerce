package org.example.audio_ecommerce.dto.response;

import lombok.Data;
import java.util.UUID;

@Data
public class StaffResponse {
    private UUID id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String storeId;
}
