package org.example.audio_ecommerce.dto.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.AddressLabel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressCreateRequest {
    @NotBlank private String receiverName;
    private String phoneNumber;
    private AddressLabel label;       // nullable
    private String country;
    private String province;
    private String district;
    private String ward;
    private String street;
    private String addressLine;
    private String postalCode;
    private String note;
    private Boolean isDefault;        // nếu true -> đặt mặc định
    private String provinceCode;
    private Integer districtId;
    private String wardCode;
}
