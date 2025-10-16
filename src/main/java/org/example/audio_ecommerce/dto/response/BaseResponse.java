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
     // ✅ Helper tạo response thành công
    public static <T> BaseResponse<T> success(String message, T data) {
        return BaseResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .build();
    }

    // ✅ Helper thành công (không có data)
    public static BaseResponse<Void> success(String message) {
        return BaseResponse.<Void>builder()
                .status(200)
                .message(message)
                .build();
    }

    // ✅ Helper lỗi
    public static BaseResponse<Void> error(String message) {
        return BaseResponse.<Void>builder()
                .status(400)
                .message(message)
                .build();
    }
}
