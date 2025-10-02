package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CreateProductRequest;
import org.example.audio_ecommerce.dto.request.UpdateProductRequest;
import org.example.audio_ecommerce.entity.Enum.CategoryEnum;
import org.example.audio_ecommerce.entity.Product;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.repository.ProductRepository;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.ProductService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository; // 👈 thêm để load Store

    @Override
    public Product createProduct(CreateProductRequest request) {
        validateCommonFields(request);

        if (request.getCategory() == CategoryEnum.SPEAKER) {
            validateSpeakerFields(request);
        }

        // Lấy store từ DB
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new RuntimeException("Store not found"));

        Product product = mapToProduct(request, store);
        return productRepository.save(product);
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product getProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    @Override
    public Product updateProduct(UUID id, UpdateProductRequest request) {
        Product product = getProductById(id);

        if (request.getCategory() == CategoryEnum.SPEAKER) {
            validateSpeakerFieldsUpdate(request);
        }

        // Update các field
        if (request.getName() != null) product.setName(request.getName());
        if (request.getSlug() != null) product.setSlug(request.getSlug());
        if (request.getShortDescription() != null) product.setShortDescription(request.getShortDescription());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getImages() != null) product.setImages(request.getImages());
        if (request.getVideoUrl() != null) product.setVideoUrl(request.getVideoUrl());
        if (request.getModel() != null) product.setModel(request.getModel());
        if (request.getColor() != null) product.setColor(request.getColor());
        if (request.getMaterial() != null) product.setMaterial(request.getMaterial());
        if (request.getDimensions() != null) product.setDimensions(request.getDimensions());
        if (request.getWeight() != null) product.setWeight(request.getWeight());
        if (request.getPowerOutput() != null) product.setPowerOutput(request.getPowerOutput());
        if (request.getConnectorType() != null) product.setConnectorType(request.getConnectorType());
        if (request.getCompatibility() != null) product.setCompatibility(request.getCompatibility());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getDiscountPrice() != null) product.setDiscountPrice(request.getDiscountPrice());
        if (request.getCurrency() != null) product.setCurrency(request.getCurrency());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        if (request.getWarehouseLocation() != null) product.setWarehouseLocation(request.getWarehouseLocation());
        if (request.getShippingAddress() != null) product.setShippingAddress(request.getShippingAddress());
        if (request.getStatus() != null) product.setStatus(request.getStatus());
        if (request.getIsFeatured() != null) product.setIsFeatured(request.getIsFeatured());

        // Loa
        if (request.getDriverConfiguration() != null) product.setDriverConfiguration(request.getDriverConfiguration());
        if (request.getDriverSize() != null) product.setDriverSize(request.getDriverSize());
        if (request.getFrequencyResponse() != null) product.setFrequencyResponse(request.getFrequencyResponse());
        if (request.getSensitivity() != null) product.setSensitivity(request.getSensitivity());
        if (request.getImpedance() != null) product.setImpedance(request.getImpedance());
        if (request.getPowerHandling() != null) product.setPowerHandling(request.getPowerHandling());
        if (request.getEnclosureType() != null) product.setEnclosureType(request.getEnclosureType());
        if (request.getCoveragePattern() != null) product.setCoveragePattern(request.getCoveragePattern());
        if (request.getCrossoverFrequency() != null) product.setCrossoverFrequency(request.getCrossoverFrequency());
        if (request.getPlacementType() != null) product.setPlacementType(request.getPlacementType());

        product.setUpdatedAt(LocalDateTime.now());

        return productRepository.save(product);
    }

    @Override
    public void deleteProduct(UUID id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }

    // ================== HÀM PHỤ ==================

    private void validateCommonFields(CreateProductRequest req) {
        if (req.getSku() == null || req.getSku().isBlank()) {
            throw new RuntimeException("SKU là bắt buộc");
        }
        if (productRepository.existsBySku(req.getSku())) {
            throw new RuntimeException("SKU đã tồn tại");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new RuntimeException("Tên sản phẩm là bắt buộc");
        }
    }

    private void validateSpeakerFields(CreateProductRequest req) {
        if (req.getDriverConfiguration() == null || req.getFrequencyResponse() == null || req.getImpedance() == null) {
            throw new RuntimeException("Loa bắt buộc phải có driverConfiguration, frequencyResponse, impedance");
        }
    }

    private void validateSpeakerFieldsUpdate(UpdateProductRequest req) {
        if (req.getDriverConfiguration() == null || req.getFrequencyResponse() == null || req.getImpedance() == null) {
            throw new RuntimeException("Loa bắt buộc phải có driverConfiguration, frequencyResponse, impedance khi update");
        }
    }

    private Product mapToProduct(CreateProductRequest req, Store store) {
        return Product.builder()
                .store(store) // 👈 gắn Store entity
                .categoryId(req.getCategoryId())
                .brandId(req.getBrandId())
                .name(req.getName())
                .slug(req.getSlug())
                .shortDescription(req.getShortDescription())
                .description(req.getDescription())
                .images(req.getImages())
                .videoUrl(req.getVideoUrl())
                .model(req.getModel())
                .color(req.getColor())
                .material(req.getMaterial())
                .dimensions(req.getDimensions())
                .weight(req.getWeight())
                .powerOutput(req.getPowerOutput())
                .connectorType(req.getConnectorType())
                .compatibility(req.getCompatibility())
                .sku(req.getSku())
                .price(req.getPrice())
                .discountPrice(req.getDiscountPrice())
                .currency(req.getCurrency())
                .stockQuantity(req.getStockQuantity())
                .warehouseLocation(req.getWarehouseLocation())
                .shippingAddress(req.getShippingAddress()) // 👈 thêm shippingAddress
                .status(req.getStatus())
                .isFeatured(req.getIsFeatured())
                .ratingAverage(null)
                .reviewCount(0)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(req.getCreatedBy())
                .updatedBy(req.getCreatedBy())
                // loa
                .driverConfiguration(req.getDriverConfiguration())
                .driverSize(req.getDriverSize())
                .frequencyResponse(req.getFrequencyResponse())
                .sensitivity(req.getSensitivity())
                .impedance(req.getImpedance())
                .powerHandling(req.getPowerHandling())
                .enclosureType(req.getEnclosureType())
                .coveragePattern(req.getCoveragePattern())
                .crossoverFrequency(req.getCrossoverFrequency())
                .placementType(req.getPlacementType())
                .build();
    }
}
