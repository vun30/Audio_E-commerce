// GhnOrderDetail.java
package org.example.audio_ecommerce.integration.ghn.dto;

import lombok.Data;

@Data
public class GhnOrderDetail {
    private String order_code;
    private String status;
    private String leadtime;      // "2025-11-22T16:59:59Z"
    private String finish_date;   // "2025-11-18T22:47:51.991Z" (có thể null)
}
