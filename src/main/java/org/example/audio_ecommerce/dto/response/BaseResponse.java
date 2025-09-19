package org.example.audio_ecommerce.dto.response;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Data
public class BaseResponse<T> {
    private int status;
    private String message;
    private T data;
}