package org.example.audio_ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class StaffInfo {
    private UUID staffId;
    private UUID storeId;     // có thể null nếu staff chưa gán store
    private String fullName;
    private String email;
    private String phone;
}
