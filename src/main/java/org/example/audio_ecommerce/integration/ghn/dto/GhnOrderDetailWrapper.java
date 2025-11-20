package org.example.audio_ecommerce.integration.ghn.dto;

import lombok.Data;

@Data
public class GhnOrderDetailWrapper {
    private int code;
    private String message;
    private GhnOrderDetail data;
}