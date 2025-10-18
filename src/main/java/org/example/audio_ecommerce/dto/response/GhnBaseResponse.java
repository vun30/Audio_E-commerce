package org.example.audio_ecommerce.dto.response;

import lombok.Data;

@Data
public class GhnBaseResponse<T> {
    private int code;
    private String message;
    private T data;
}
