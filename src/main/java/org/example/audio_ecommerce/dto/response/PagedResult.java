// dto/response/PagedResult.java
package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PagedResult<T> {
    private List<T> items;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}
