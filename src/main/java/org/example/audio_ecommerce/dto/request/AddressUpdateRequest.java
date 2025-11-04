package org.example.audio_ecommerce.dto.request;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.AddressLabel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressUpdateRequest {
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
    private Boolean isDefault;
    private String provinceCode;
    private Integer districtId;
    private String wardCode;
    private String lat;
    private String lng;
}