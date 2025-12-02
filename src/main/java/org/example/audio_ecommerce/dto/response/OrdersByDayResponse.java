package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrdersByDayResponse {
    private List<OrdersByDayPoint> points;
    private long total;
}

