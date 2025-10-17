package org.example.audio_ecommerce.entity.Enum;

public enum StoreStatus {
    INACTIVE,
    PENDING,
    ACTIVE,
    REJECTED,
    SUSPENDED,
    PAUSED // store tạm dừng hoạt động, stroe chỉ đổi sang trạng thái này khi chủ store tự chọn từ active  thành paused
}
