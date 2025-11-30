package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class OrdersByDayPoint {
    private LocalDate date;
    private long count;
}

