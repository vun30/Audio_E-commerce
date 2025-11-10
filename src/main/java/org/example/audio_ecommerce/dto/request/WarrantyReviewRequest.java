// dto/request/WarrantyReviewRequest.java
package org.example.audio_ecommerce.dto.request;

import lombok.Data;
@Data
public class WarrantyReviewRequest {
    private int rating;    // 1..5
    private String comment;
}
