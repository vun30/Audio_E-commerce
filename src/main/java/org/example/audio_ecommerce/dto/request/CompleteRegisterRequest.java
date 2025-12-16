package org.example.audio_ecommerce.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteRegisterRequest {
    private String name;
    private String email;
    private String password;
}
