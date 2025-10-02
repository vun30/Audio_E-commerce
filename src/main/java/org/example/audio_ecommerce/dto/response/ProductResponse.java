package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private UUID productId;
    private UUID storeId;
    private String storeName;        // Trả về thêm tên store cho FE

    private UUID categoryId;
    private UUID brandId;

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
    private String shippingAddress;

    private ProductStatus status;
    private Boolean isFeatured;

    private BigDecimal ratingAverage;
    private Integer reviewCount;
    private Integer viewCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UUID createdBy;
    private UUID updatedBy;

    // Loa
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
}
