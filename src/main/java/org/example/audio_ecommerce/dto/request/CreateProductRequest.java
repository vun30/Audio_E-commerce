package org.example.audio_ecommerce.dto.request;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.CategoryEnum;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProductRequest {

    private UUID storeId;              // 🔹 Id của Store (FE gửi lên khi tạo sản phẩm)
    private UUID categoryId;           // 🔹 Danh mục
    private UUID brandId;              // 🔹 Thương hiệu

    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private List<String> images;
    private String videoUrl;
    private String model;
    private String color;
    private String material;
    private String dimensions;
    private BigDecimal weight;

    private String powerOutput;
    private String connectorType;
    private String compatibility;

    private String sku;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String currency;
    private Integer stockQuantity;
    private String warehouseLocation;
    private String shippingAddress;    // 🔹 Địa chỉ shop đưa hàng cho shipper

    private ProductStatus status;
    private Boolean isFeatured;

    private UUID createdBy;
    private UUID updatedBy;

    // Nếu là loa thì cần thêm
    private String driverConfiguration;
    private String driverSize;
    private String frequencyResponse;
    private String sensitivity;
    private String impedance;
    private String powerHandling;
    private String enclosureType;
    private String coveragePattern;
    private String crossoverFrequency;
    private String placementType;

    private CategoryEnum category; // 🔹 để phân biệt có validate theo SPEAKER không
}
