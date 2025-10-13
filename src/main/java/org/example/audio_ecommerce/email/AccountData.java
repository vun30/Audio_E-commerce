package org.example.audio_ecommerce.email;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountData {
    private String email;     // người nhận
    private String name;      // tên hiển thị
    private String role;      // CUSTOMER / STOREOWNER / ADMIN
    private String siteUrl;   // vd: https://yourplatform.com
}
