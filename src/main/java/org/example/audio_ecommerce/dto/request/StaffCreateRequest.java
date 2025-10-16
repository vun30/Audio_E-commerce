package org.example.audio_ecommerce.dto.request;

import lombok.Data;

@Data
public class StaffCreateRequest {
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String phone;
}

