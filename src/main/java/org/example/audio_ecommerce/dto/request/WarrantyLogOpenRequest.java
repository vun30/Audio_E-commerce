package org.example.audio_ecommerce.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class WarrantyLogOpenRequest {
    private String problemDescription;
    private Boolean covered; // null = theo default warranty
    private List<String> attachmentUrls;
}
