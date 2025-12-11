package org.example.audio_ecommerce.entity.Enum;

public enum ProductStatus {
    DRAFT,          // Soạn thảo, chưa publish
    ACTIVE,         // Đang bán bình thường (live/published)
    INACTIVE,       // Tạm dừng (có thể tương đương unlist hoặc suspended)
    OUT_OF_STOCK,   // Hết hàng (sold out, nhưng vẫn hiển thị)
    DISCONTINUED,   // Ngừng bán vĩnh viễn
    UNLISTED,       // Ẩn tạm thời khỏi listing
    SUSPENDED,      // Tạm ngưng do vi phạm
    DELETED,        // Xóa khỏi hệ thống
    BANNED,         // Bị cấm (nếu cần phân biệt với suspended)
    REJECT,      // Bị từ chối (ví dụ khi duyệt sản phẩm không đạt)
    PENDING_APPROVAL // Chờ duyệt (ví dụ khi sản phẩm mới tạo cần admin duyệt)
}