package org.example.audio_ecommerce.entity.Enum;

public enum StoreStatus {
    CREATED,
    INACTIVE,
    PENDING,
    ACTIVE,
    REJECTED,
    SUSPENDED,
    PAUSED,// store tạm dừng hoạt động, stroe chỉ đổi sang trạng thái này khi chủ store tự chọn từ active  thành paused
    SUSPENDED_DEBT, // store bị khóa do nợ phí dịch vụ
    SUSPENDED_POLICY,  // store bị khóa do vi phạm chính sách
    ABANDONED, // store bị khóa do chủ store không hoạt động trong thời gian dài
    CLOSED // store đóng cửa vĩnh viễn

}
