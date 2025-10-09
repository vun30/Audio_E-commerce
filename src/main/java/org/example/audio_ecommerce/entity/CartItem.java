//package org.example.audio_ecommerce.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.example.audio_ecommerce.entity.Enum.CartItemType;
//import org.hibernate.annotations.GenericGenerator;
//
//import java.math.BigDecimal;
//import java.util.UUID;
//
//@Getter @Setter
//@NoArgsConstructor @AllArgsConstructor @Builder
//@Entity @Table(name = "cart_items")
//public class CartItem {
//
//    @Id
//    @GeneratedValue(generator = "uuid2")
//    @GenericGenerator(name="uuid2", strategy="uuid2")
//    @Column(name = "cart_item_id", columnDefinition = "CHAR(36)")
//    private UUID cartItemId;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "cart_id", nullable = false)
//    private Cart cart;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private CartItemType itemType; // PRODUCT / COMBO
//
//    // Một trong hai trường dưới sẽ có giá trị, tuỳ thuộc itemType
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "product_id")
//    private Product product; // khi itemType = PRODUCT
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "combo_id")
//    private ProductCombo combo; // khi itemType = COMBO
//
//    // Lưu thêm thông tin để hiển thị/khóa giá tại thời điểm add
//    @Column(nullable = false)
//    private String displayName; // tên sản phẩm/combo để show giỏ
//
//    @Column(name = "store_id", columnDefinition = "CHAR(36)")
//    private UUID storeId; // để group theo shop giống Shopee
//
//    @Column(nullable = false)
//    private Integer quantity;
//
//    @Column(nullable = false, precision = 19, scale = 2)
//    private BigDecimal unitPrice; // đơn giá (đã chọn price/discountPrice hoặc comboPrice)
//
//    @Column(nullable = false, precision = 19, scale = 2)
//    private BigDecimal subtotal;
//
//    @Column(nullable = false)
//    private Boolean selected = true; // tick chọn hay không
//
//    public void recomputeSubtotal() {
//        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
//    }
//}
