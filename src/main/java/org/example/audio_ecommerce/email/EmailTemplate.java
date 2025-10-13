package org.example.audio_ecommerce.email;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplate {
    private String to;
    private String subject;
    private String content; // HTML
}
