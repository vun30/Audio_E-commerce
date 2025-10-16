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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;

    // ============================================================
    // 🔧 Helper: Sinh slug duy nhất
    // ============================================================
    private String generateUniqueSlug(String productName) {
        if (productName == null || productName.isBlank())
            return UUID.randomUUID().toString().substring(0, 8);

        String base = productName.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        String slug = base;
        int counter = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    // ============================================================
    // ➕ CREATE PRODUCT
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> createProduct(ProductRequest req) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Store not found for logged-in account"));

        // 🔍 Tìm Category theo tên
        if (req.getCategoryName() == null || req.getCategoryName().isBlank())
            throw new RuntimeException("❌ Category Name must not be null");

        Category category = categoryRepository.findByNameIgnoreCase(req.getCategoryName())
                .orElseThrow(() -> new RuntimeException("❌ Category not found: " + req.getCategoryName()));

        if (req.getSku() == null || req.getSku().isBlank())
            throw new RuntimeException("❌ SKU must not be empty");

        if (productRepository.existsByStore_StoreIdAndSku(store.getStoreId(), req.getSku()))
            throw new RuntimeException("❌ SKU already exists in this store");

        // ✅ Khởi tạo Product
        Product p = new Product();
        p.setStore(store);
        p.setCategory(category);
        p.setBrandName(req.getBrandName());
        p.setName(req.getName());
        p.setSlug(generateUniqueSlug(req.getName()));
        p.setSku(req.getSku());
        p.setStatus(ProductStatus.ACTIVE);
        p.setIsFeatured(false);
        p.setCreatedAt(LocalDateTime.now());
        p.setCreatedBy(store.getAccount().getId());

        // Áp dữ liệu từ request
        applyRequestToProduct(p, req);

        // Tính toán giá sau khuyến mãi
        calculatePrice(p);

        productRepository.save(p);
        return ResponseEntity.ok(new BaseResponse<>(201, "✅ Product created successfully", toResponse(p)));
    }

    // ============================================================
    // ✏️ UPDATE PRODUCT
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> updateProduct(UUID id, ProductRequest req) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Store not found"));

        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Product not found"));

        if (!p.getStore().getStoreId().equals(store.getStoreId()))
            throw new RuntimeException("❌ Cannot update another store's product");

        if (req.getCategoryName() != null && !req.getCategoryName().isBlank()) {
            Category category = categoryRepository.findByNameIgnoreCase(req.getCategoryName())
                    .orElseThrow(() -> new RuntimeException("❌ Category not found: " + req.getCategoryName()));
            p.setCategory(category);
        }

        if (req.getName() != null) {
            p.setName(req.getName());
            p.setSlug(generateUniqueSlug(req.getName()));
        }

        if (req.getSku() != null && !req.getSku().equals(p.getSku())) {
            if (productRepository.existsByStore_StoreIdAndSku(store.getStoreId(), req.getSku()))
                throw new RuntimeException("❌ SKU already exists in this store");
            p.setSku(req.getSku());
        }

        applyRequestToProduct(p, req);
        calculatePrice(p);

        p.setUpdatedAt(LocalDateTime.now());
        p.setUpdatedBy(store.getAccount().getId());

        productRepository.save(p);
        return ResponseEntity.ok(new BaseResponse<>(200, "✏️ Product updated successfully", toResponse(p)));
    }

    // ============================================================
    // 🔄 ENABLE / DISABLE PRODUCT
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> disableProduct(UUID id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Product not found"));
        p.setStatus(p.getStatus() == ProductStatus.ACTIVE ? ProductStatus.DISCONTINUED : ProductStatus.ACTIVE);
        p.setUpdatedAt(LocalDateTime.now());
        productRepository.save(p);
        return ResponseEntity.ok(new BaseResponse<>(200, "🔄 Product status changed", toResponse(p)));
    }

    // ============================================================
    // 📜 GET ALL / 🔍 BY ID
    // ============================================================
    @Override
public ResponseEntity<BaseResponse> getAllProducts(
        String categoryName, UUID storeId, String keyword, int page, int size, ProductStatus status) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    Page<Product> products = productRepository.findAll(pageable);

    // Danh sách tên hợp lệ để FE select filter (có thể customize thêm sau)
    List<String> validCategoryNames = List.of(
            "Tai Nghe", "Loa", "Micro", "DAC", "Mixer", "Amp",
            "Turntable", "Sound Card", "DJ Controller", "Combo"
    );

    // Chuẩn hóa tên danh mục được gửi từ FE
    final String normalizedCategory =
        (categoryName != null && !categoryName.isBlank())
                ? validCategoryNames.stream()
                    .filter(c -> c.equalsIgnoreCase(categoryName))
                    .findFirst()
                    .orElse(null)
                : null;

List<ProductResponse> filtered = products.getContent().stream()
        .filter(p -> normalizedCategory == null ||
                (p.getCategory() != null &&
                 p.getCategory().getName() != null &&
                 p.getCategory().getName().equalsIgnoreCase(normalizedCategory)))
        .filter(p -> storeId == null || p.getStore().getStoreId().equals(storeId))
        .filter(p -> keyword == null || p.getName().toLowerCase().contains(keyword.toLowerCase()))
        .filter(p -> status == null || p.getStatus() == status)
        .map(this::toResponse)
        .toList();

    return ResponseEntity.ok(new BaseResponse<>(200, "📦 Product list filtered successfully", filtered));
}

    @Override
    public ResponseEntity<BaseResponse> getProductById(UUID id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Product not found"));
        return ResponseEntity.ok(new BaseResponse<>(200, "🔎 Product detail", toResponse(p)));
    }

    // ============================================================
    // 💡 Helper: Convert Entity → DTO
    // ============================================================
    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .productId(p.getProductId())
                .storeId(p.getStore().getStoreId())
                .storeName(p.getStore().getStoreName())
                .categoryId(p.getCategory().getCategoryId())
                .categoryName(p.getCategory().getName())
                .brandName(p.getBrandName())
                .name(p.getName())
                .slug(p.getSlug())
                .sku(p.getSku())
                .price(p.getPrice())
                .discountPrice(p.getDiscountPrice())
                .promotionPercent(p.getPromotionPercent())
                .priceAfterPromotion(p.getPriceAfterPromotion())
                .priceBeforeVoucher(p.getPriceBeforeVoucher())
                .finalPrice(p.getFinalPrice())
                .platformFeePercent(p.getPlatformFeePercent())
                .currency(p.getCurrency())
                .stockQuantity(p.getStockQuantity())
                .warehouseLocation(p.getWarehouseLocation())
                .shippingAddress(p.getShippingAddress())
                .images(p.getImages())
                .videoUrl(p.getVideoUrl())
                .status(p.getStatus())
                .isFeatured(p.getIsFeatured())
                .ratingAverage(p.getRatingAverage())
                .reviewCount(p.getReviewCount())
                .viewCount(p.getViewCount())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    // ============================================================
    // 💡 Helper: Apply Request → Product Entity (Đồng bộ 100%)
    // ============================================================
    private void applyRequestToProduct(Product p, ProductRequest r) {
        // === Thông tin chung ===
        p.setBrandName(r.getBrandName());
        p.setShortDescription(r.getShortDescription());
        p.setDescription(r.getDescription());
        p.setModel(r.getModel());
        p.setColor(r.getColor());
        p.setMaterial(r.getMaterial());
        p.setDimensions(r.getDimensions());
        p.setWeight(r.getWeight());
        p.setImages(r.getImages());
        p.setVideoUrl(r.getVideoUrl());

        // === Giá & kho ===
        p.setPrice(r.getPrice());
        p.setDiscountPrice(r.getDiscountPrice());
        p.setPromotionPercent(r.getPromotionPercent());
        p.setCurrency(r.getCurrency());
        p.setStockQuantity(r.getStockQuantity());
        p.setWarehouseLocation(r.getWarehouseLocation());
        p.setShippingAddress(r.getShippingAddress());
        p.setPlatformFeePercent(r.getPlatformFeePercent());

        // === Bảo hành / Nhà sản xuất ===
        p.setVoltageInput(r.getVoltageInput());
        p.setWarrantyPeriod(r.getWarrantyPeriod());
        p.setWarrantyType(r.getWarrantyType());
        p.setManufacturerName(r.getManufacturerName());
        p.setManufacturerAddress(r.getManufacturerAddress());
        p.setProductCondition(r.getProductCondition());
        p.setIsCustomMade(r.getIsCustomMade());

        // === Tai nghe ===
        p.setHeadphoneType(r.getHeadphoneType());
        p.setCompatibleDevices(r.getCompatibleDevices());
        p.setIsSportsModel(r.getIsSportsModel());
        p.setHeadphoneFeatures(r.getHeadphoneFeatures());
        p.setBatteryCapacity(r.getBatteryCapacity());
        p.setHasBuiltInBattery(r.getHasBuiltInBattery());
        p.setIsGamingHeadset(r.getIsGamingHeadset());
        p.setHeadphoneAccessoryType(r.getHeadphoneAccessoryType());
        p.setHeadphoneConnectionType(r.getHeadphoneConnectionType());
        p.setPlugType(r.getPlugType());
        p.setSirimApproved(r.getSirimApproved());
        p.setSirimCertified(r.getSirimCertified());
        p.setMcmcApproved(r.getMcmcApproved());

        // === Loa ===
        p.setDriverConfiguration(r.getDriverConfiguration());
        p.setDriverSize(r.getDriverSize());
        p.setFrequencyResponse(r.getFrequencyResponse());
        p.setSensitivity(r.getSensitivity());
        p.setImpedance(r.getImpedance());
        p.setPowerHandling(r.getPowerHandling());
        p.setEnclosureType(r.getEnclosureType());
        p.setCoveragePattern(r.getCoveragePattern());
        p.setCrossoverFrequency(r.getCrossoverFrequency());
        p.setPlacementType(r.getPlacementType());
        p.setConnectionType(r.getConnectionType());

        // === Ampli / Receiver ===
        p.setAmplifierType(r.getAmplifierType());
        p.setTotalPowerOutput(r.getTotalPowerOutput());
        p.setThd(r.getThd());
        p.setSnr(r.getSnr());
        p.setInputChannels(r.getInputChannels());
        p.setOutputChannels(r.getOutputChannels());
        p.setSupportBluetooth(r.getSupportBluetooth());
        p.setSupportWifi(r.getSupportWifi());
        p.setSupportAirplay(r.getSupportAirplay());

        // === Micro ===
        p.setMicType(r.getMicType());
        p.setPolarPattern(r.getPolarPattern());
        p.setMaxSPL(r.getMaxSPL());
        p.setMicOutputImpedance(r.getMicOutputImpedance());
        p.setMicSensitivity(r.getMicSensitivity());

        // === Turntable ===
        p.setPlatterMaterial(r.getPlatterMaterial());
        p.setMotorType(r.getMotorType());
        p.setTonearmType(r.getTonearmType());
        p.setAutoReturn(r.getAutoReturn());

        // === DAC / Mixer / SoundCard ===
        p.setDacChipset(r.getDacChipset());
        p.setSampleRate(r.getSampleRate());
        p.setBitDepth(r.getBitDepth());
        p.setBalancedOutput(r.getBalancedOutput());
        p.setInputInterface(r.getInputInterface());
        p.setOutputInterface(r.getOutputInterface());
        p.setChannelCount(r.getChannelCount());
        p.setHasPhantomPower(r.getHasPhantomPower());
        p.setEqBands(r.getEqBands());
        p.setFaderType(r.getFaderType());
        p.setBuiltInEffects(r.getBuiltInEffects());
        p.setUsbAudioInterface(r.getUsbAudioInterface());
        p.setMidiSupport(r.getMidiSupport());

        // === Bulk discount ===
        if (r.getBulkDiscounts() != null) {
            List<Product.BulkDiscount> discounts = r.getBulkDiscounts().stream()
                    .map(b -> new Product.BulkDiscount(
                            b.getFromQuantity(),
                            b.getToQuantity(),
                            b.getUnitPrice()))
                    .collect(Collectors.toList());
            p.setBulkDiscounts(discounts);
        }
    }

    // ============================================================
    // 💡 Helper: Tính giá động
    // ============================================================
    private void calculatePrice(Product p) {
        if (p.getPromotionPercent() != null && p.getPromotionPercent().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = p.getPrice()
                    .multiply(p.getPromotionPercent())
                    .divide(BigDecimal.valueOf(100));
            p.setPriceAfterPromotion(p.getPrice().subtract(discount));
        } else {
            p.setPriceAfterPromotion(p.getPrice());
        }
        p.setPriceBeforeVoucher(p.getPriceAfterPromotion());
        p.setFinalPrice(p.getPriceAfterPromotion());
    }
}
