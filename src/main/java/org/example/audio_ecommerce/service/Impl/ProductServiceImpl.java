package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ProductRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.ProductResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.ProductService;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;

    /**
     * ➕ Tạo sản phẩm mới
     */
    @Override
    public ResponseEntity<BaseResponse> createProduct(ProductRequest request) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new RuntimeException("Store not found"));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (request.getSku() == null || request.getSku().isBlank()) {
            throw new RuntimeException("SKU must not be empty");
        }
        if (request.getBrandName() == null) {
            throw new RuntimeException("Brand Name must not be null");
        }

        Product product = new Product();
        product.setStore(store);
        product.setCategory(category);
        product.setBrandName(request.getBrandName());
        product.setName(request.getName());
        product.setSlug(request.getSlug());
        product.setShortDescription(request.getShortDescription());
        product.setDescription(request.getDescription());
        product.setModel(request.getModel());
        product.setColor(request.getColor());
        product.setMaterial(request.getMaterial());
        product.setDimensions(request.getDimensions());
        product.setWeight(request.getWeight());
        product.setImages(request.getImages());
        product.setVideoUrl(request.getVideoUrl());
        product.setSku(request.getSku());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setCurrency(request.getCurrency());
        product.setStockQuantity(request.getStockQuantity());
        product.setWarehouseLocation(request.getWarehouseLocation());
        product.setShippingAddress(request.getShippingAddress());
        product.setStatus(ProductStatus.ACTIVE);
        product.setIsFeatured(false);
        product.setCreatedAt(LocalDateTime.now());

        productRepository.save(product);
        return ResponseEntity.ok(new BaseResponse<>(201, "✅ Product created successfully", toResponse(product)));
    }

    /**
     * 📜 Lấy tất cả sản phẩm (có bộ lọc & phân trang)
     */
    @Override
    public ResponseEntity<BaseResponse> getAllProducts(
            UUID categoryId,
            UUID storeId,
            String keyword,
            int page,
            int size,
            ProductStatus status
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products = productRepository.findAll(pageable);

        List<ProductResponse> filtered = products.stream()
                .filter(p -> categoryId == null || p.getCategory().getCategoryId().equals(categoryId))
                .filter(p -> storeId == null || p.getStore().getStoreId().equals(storeId))
                .filter(p -> keyword == null || p.getName().toLowerCase().contains(keyword.toLowerCase()))
                .filter(p -> status == null || p.getStatus() == status)
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(new BaseResponse<>(200, "📦 List of products", filtered));
    }

    /**
     * 🔍 Lấy chi tiết sản phẩm theo ID (đồng bộ DTO)
     */
    @Override
    public ResponseEntity<BaseResponse> getProductById(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return ResponseEntity.ok(new BaseResponse<>(200, "🔎 Product detail", toResponse(product)));
    }

    /**
     * ✏️ Cập nhật sản phẩm (đồng bộ DTO)
     */
    @Override
    public ResponseEntity<BaseResponse> updateProduct(UUID productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (request.getName() != null) product.setName(request.getName());
        if (request.getSlug() != null) product.setSlug(request.getSlug());
        if (request.getShortDescription() != null) product.setShortDescription(request.getShortDescription());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getDiscountPrice() != null) product.setDiscountPrice(request.getDiscountPrice());
        if (request.getImages() != null) product.setImages(request.getImages());
        if (request.getVideoUrl() != null) product.setVideoUrl(request.getVideoUrl());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        if (request.getBrandName() != null) product.setBrandName(request.getBrandName());


        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        return ResponseEntity.ok(new BaseResponse<>(200, "✏️ Product updated successfully", toResponse(product)));
    }

    /**
     * 🔄 Thay đổi trạng thái sản phẩm (ACTIVE <-> DISCONTINUED)
     */
    @Override
    public ResponseEntity<BaseResponse> disableProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getStatus() == ProductStatus.ACTIVE) {
            product.setStatus(ProductStatus.DISCONTINUED);
        } else {
            product.setStatus(ProductStatus.ACTIVE);
        }

        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        return ResponseEntity.ok(new BaseResponse<>(200, "🔄 Product status changed: " + product.getStatus(), toResponse(product)));
    }

    /**
     * ✅ Convert Entity -> DTO
     */
    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .productId(p.getProductId())
                .storeId(p.getStore() != null ? p.getStore().getStoreId() : null)
                .storeName(p.getStore() != null ? p.getStore().getStoreName() : null)
                .categoryId(p.getCategory() != null ? p.getCategory().getCategoryId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .brandName(p.getBrandName())
                .name(p.getName())
                .slug(p.getSlug())
                .shortDescription(p.getShortDescription())
                .description(p.getDescription())
                .model(p.getModel())
                .color(p.getColor())
                .material(p.getMaterial())
                .dimensions(p.getDimensions())
                .weight(p.getWeight())
                .images(p.getImages())
                .videoUrl(p.getVideoUrl())
                .sku(p.getSku())
                .price(p.getPrice())
                .discountPrice(p.getDiscountPrice())
                .currency(p.getCurrency())
                .stockQuantity(p.getStockQuantity())
                .warehouseLocation(p.getWarehouseLocation())
                .shippingAddress(p.getShippingAddress())
                .status(p.getStatus())
                .isFeatured(p.getIsFeatured())
                .ratingAverage(p.getRatingAverage())
                .reviewCount(p.getReviewCount())
                .viewCount(p.getViewCount())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .driverConfiguration(p.getDriverConfiguration())
                .driverSize(p.getDriverSize())
                .frequencyResponse(p.getFrequencyResponse())
                .sensitivity(p.getSensitivity())
                .impedance(p.getImpedance())
                .powerHandling(p.getPowerHandling())
                .enclosureType(p.getEnclosureType())
                .coveragePattern(p.getCoveragePattern())
                .crossoverFrequency(p.getCrossoverFrequency())
                .placementType(p.getPlacementType())
                .connectionType(p.getConnectionType())
                .micType(p.getMicType())
                .polarPattern(p.getPolarPattern())
                .maxSPL(p.getMaxSPL())
                .micOutputImpedance(p.getMicOutputImpedance())
                .micSensitivity(p.getMicSensitivity())
                .amplifierType(p.getAmplifierType())
                .totalPowerOutput(p.getTotalPowerOutput())
                .thd(p.getThd())
                .snr(p.getSnr())
                .inputChannels(p.getInputChannels())
                .outputChannels(p.getOutputChannels())
                .supportBluetooth(p.getSupportBluetooth())
                .supportWifi(p.getSupportWifi())
                .supportAirplay(p.getSupportAirplay())
                .platterMaterial(p.getPlatterMaterial())
                .motorType(p.getMotorType())
                .tonearmType(p.getTonearmType())
                .autoReturn(p.getAutoReturn())
                .dacChipset(p.getDacChipset())
                .sampleRate(p.getSampleRate())
                .bitDepth(p.getBitDepth())
                .balancedOutput(p.getBalancedOutput())
                .inputInterface(p.getInputInterface())
                .outputInterface(p.getOutputInterface())
                .channelCount(p.getChannelCount())
                .hasPhantomPower(p.getHasPhantomPower())
                .eqBands(p.getEqBands())
                .faderType(p.getFaderType())
                .builtInEffects(p.getBuiltInEffects())
                .usbAudioInterface(p.getUsbAudioInterface())
                .midiSupport(p.getMidiSupport())
                .build();
    }
}
