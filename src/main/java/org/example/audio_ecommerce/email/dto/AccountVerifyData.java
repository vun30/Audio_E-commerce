
package org.example.audio_ecommerce.email.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountVerifyData {
    private String email;
    private String name;
    private String role;
    private String verifyLink; // link xác nhận email
    private String siteUrl;    // link trang web (optional)
}
