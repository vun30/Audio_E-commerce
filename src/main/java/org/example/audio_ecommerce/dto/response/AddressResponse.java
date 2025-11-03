package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.AddressLabel;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {
    private UUID id;
    private UUID customerId;
    private String receiverName;
    private String phoneNumber;
    private AddressLabel label;
    private String country;
    private String province;
    private String district;
    private String ward;
    private String street;
    private String addressLine;
    private String postalCode;
    private String note;
    private boolean isDefault;
    private String provinceCode;
    private Integer districtId;
    private String wardCode;
}