package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ProductRequest;
import org.example.audio_ecommerce.dto.request.UpdateProductRequest;
import org.example.audio_ecommerce.dto.request.VariantRequest;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final PlatformFeeRepository platformFeeRepository;
    private final ProductVariantRepository productVariantRepository;

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
    // 🧮 Helper tính tổng stock biến thể
    // ============================================================
    private int calculateVariantStockTotal(UUID productId) {
        return productVariantRepository.findAllByProduct_ProductId(productId)
                .stream()
                .mapToInt(ProductVariantEntity::getVariantStock)
                .sum();
    }


    // 🔐 Helper: kiểm tra store đang đăng nhập có địa chỉ default hay chưa
// ============================================================
    private void ensureStoreHasDefaultAddress() {

        // 1️⃣ Lấy principal từ token
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID accountId = null;

        try {
            // Token dạng "email:ROLE:UUID"
            if (principal.contains(":")) {
                String[] parts = principal.split(":");
                for (String p : parts) {
                    try {
                        accountId = UUID.fromString(p);
                        break;
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Nếu không parse được thì fallback lấy email
        Store store;
        if (accountId == null) {
            String email = principal.contains(":") ? principal.split(":")[0] : principal;

            store = storeRepository.findByAccount_Email(email)
                    .orElseThrow(() -> new RuntimeException(
                            "❌ Store not found for current login (email=" + email + ")"
                    ));

        } else {
            UUID finalAccountId = accountId;

            store = storeRepository.findByAccount_Id(finalAccountId)
                    .orElseThrow(() -> new RuntimeException(
                            "❌ Store not found for current login (accountId=" + finalAccountId + ")"
                    ));
        }

        // 2️⃣ Kiểm tra có địa chỉ hay chưa
        if (store.getStoreAddresses() == null || store.getStoreAddresses().isEmpty()) {
            throw new RuntimeException("❌ Store has no addresses. Please add an address first.");
        }

        // 3️⃣ Kiểm tra có default hay chưa
        boolean hasDefault = store.getStoreAddresses().stream()
                .anyMatch(a -> Boolean.TRUE.equals(a.getDefaultAddress()));

        if (!hasDefault) {
            throw new RuntimeException("❌ Store has NO default address. Please set one default address before performing this action.");
        }
    }


    // ============================================================
    // ➕ CREATE PRODUCT
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> createProduct(ProductRequest req) {

        ensureStoreHasDefaultAddress();
        try {
            String principal = SecurityContextHolder.getContext().getAuthentication().getName();
            String email = principal.contains(":") ? principal.split(":")[0] : principal;

            Store store = storeRepository.findByAccount_Email(email)
                    .orElseThrow(() -> new RuntimeException("❌ Store not found for logged-in account"));

            if (req.getCategoryName() == null || req.getCategoryName().isBlank())
                throw new RuntimeException("❌ Category Name must not be null");

            Category category = categoryRepository.findByNameIgnoreCase(req.getCategoryName())
                    .orElseThrow(() -> new RuntimeException("❌ Category not found: " + req.getCategoryName()));

            if (req.getSku() == null || req.getSku().isBlank())
                throw new RuntimeException("❌ SKU must not be empty");

            if (productRepository.existsByStore_StoreIdAndSku(store.getStoreId(), req.getSku()))
                throw new RuntimeException("❌ SKU already exists in this store");

            LocalDateTime now = LocalDateTime.now();

            Product p = new Product();
            p.setStore(store);
            p.setCategory(category);
            p.setBrandName(req.getBrandName());
            p.setName(req.getName());
            p.setSlug(generateUniqueSlug(req.getName()));
            p.setSku(req.getSku());
            p.setStatus(ProductStatus.ACTIVE);
            p.setIsFeatured(false);
            p.setCreatedAt(now);
            p.setUpdatedAt(now);
            p.setLastUpdatedAt(now);
            p.setLastUpdateIntervalDays(0L);
            p.setCreatedBy(store.getAccount().getId());
            p.setUpdatedBy(store.getAccount().getId());

            // Ánh xạ dữ liệu kỹ thuật & chi tiết
            applyRequestToProduct(p, req);

            // Giá
            p.setPrice(req.getPrice());
            p.setCurrency(req.getCurrency());
            p.setDiscountPrice(null);
            p.setPromotionPercent(null);
            p.setPriceAfterPromotion(req.getPrice());
            p.setPriceBeforeVoucher(req.getPrice());
            p.setVoucherAmount(null);
            p.setFinalPrice(req.getPrice());
            p.setPlatformFeePercent(null);

            // 🎯 LOGIC STOCK — tạo sản phẩm
            if (req.getVariants() == null || req.getVariants().isEmpty()) {
                // Không có biến thể → FE được phép gửi stockQuantity
                p.setStockQuantity(req.getStockQuantity());
            } else {
                // Có biến thể → stock sẽ tính lại sau bằng tổng stock biến thể
                p.setStockQuantity(0);
            }

            productRepository.save(p);   // save lần 1 để có productId

            // 🔄 Lưu biến thể
            if (req.getVariants() != null && !req.getVariants().isEmpty()) {
                for (VariantRequest v : req.getVariants()) {
                    ProductVariantEntity variant = new ProductVariantEntity();
                    variant.setProduct(p);
                    variant.setOptionName(v.getOptionName());
                    variant.setOptionValue(v.getOptionValue());
                    variant.setVariantPrice(v.getVariantPrice());
                    variant.setVariantStock(v.getVariantStock());
                    variant.setVariantUrl(v.getVariantUrl());
                    variant.setVariantSku(v.getVariantSku());
                    productVariantRepository.save(variant);
                }

                // Sau khi có biến thể → tính lại tổng stock
                int totalStock = calculateVariantStockTotal(p.getProductId());
                p.setStockQuantity(totalStock);
                productRepository.save(p);
            }

            return ResponseEntity.ok(
                    new BaseResponse<>(201, "✅ Product created successfully", toResponse(p))
            );

        } catch (Exception e) {
            System.err.println("\n===== CREATE PRODUCT ERROR =====");
            System.err.println("ERROR TYPE: " + e.getClass().getName());
            System.err.println("ERROR MESSAGE: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    BaseResponse.error("❌ Create product failed: " + e.getMessage())
            );
        }
    }

    // ============================================================
    // ✏️ UPDATE PRODUCT
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> updateProduct(UUID id, UpdateProductRequest req) {
        try {
            String principal = SecurityContextHolder.getContext().getAuthentication().getName();
            String email = principal.contains(":") ? principal.split(":")[0] : principal;

            Store store = storeRepository.findByAccount_Email(email)
                    .orElseThrow(() -> new RuntimeException("❌ Store not found for current account"));

            Product p = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("❌ Product not found"));

            if (!p.getStore().getStoreId().equals(store.getStoreId())) {
                throw new RuntimeException("❌ You are not allowed to update another store's product");
            }

            // 🔗 Update Basic Fields
            if (req.getCategoryName() != null && !req.getCategoryName().isBlank()) {
                Category category = categoryRepository.findByNameIgnoreCase(req.getCategoryName())
                        .orElseThrow(() -> new RuntimeException("❌ Category not found: " + req.getCategoryName()));
                p.setCategory(category);
            }

            if (req.getName() != null && !req.getName().isBlank()) {
                p.setName(req.getName());
                p.setSlug(generateUniqueSlug(req.getName()));
            }

            if (req.getSku() != null && !req.getSku().equals(p.getSku())) {
                if (productRepository.existsByStore_StoreIdAndSku(store.getStoreId(), req.getSku()))
                    throw new RuntimeException("❌ SKU already exists in this store");
                p.setSku(req.getSku());
            }

            LocalDateTime now = LocalDateTime.now();
            long intervalDays = p.getLastUpdatedAt() != null
                    ? ChronoUnit.DAYS.between(p.getLastUpdatedAt(), now)
                    : 0L;

            p.setLastUpdateIntervalDays(intervalDays);
            p.setLastUpdatedAt(now);
            p.setUpdatedAt(now);
            p.setUpdatedBy(store.getAccount().getId());

            // Map các field còn lại (trừ variant)
            mapUpdateRequestToProduct(p, req);

            // Lấy biến thể hiện tại trong DB
            List<ProductVariantEntity> oldVariants =
                    productVariantRepository.findAllByProduct_ProductId(p.getProductId());

            // 🎯 UPDATE VARIANTS — KHÔNG CHO THÊM hoặc XÓA, chỉ update theo ID
            if (req.getVariants() != null) {

                if (req.getVariants().size() != oldVariants.size()) {
                    throw new RuntimeException("❌ Không được thêm hoặc xóa biến thể. Số lượng phải giống DB.");
                }

                for (UpdateProductRequest.VariantResponse vReq : req.getVariants()) {

                    if (vReq.getVariantId() == null)
                        throw new RuntimeException("❌ variantId is required for update");

                    ProductVariantEntity variant = oldVariants.stream()
                            .filter(x -> x.getId().equals(vReq.getVariantId()))
                            .findFirst()
                            .orElseThrow(() ->
                                    new RuntimeException("❌ Variant ID not found: " + vReq.getVariantId())
                            );

                    if (vReq.getOptionName() != null) variant.setOptionName(vReq.getOptionName());
                    if (vReq.getOptionValue() != null) variant.setOptionValue(vReq.getOptionValue());
                    if (vReq.getVariantPrice() != null) variant.setVariantPrice(vReq.getVariantPrice());
                    if (vReq.getVariantStock() != null) variant.setVariantStock(vReq.getVariantStock());
                    if (vReq.getVariantUrl() != null) variant.setVariantUrl(vReq.getVariantUrl());
                    if (vReq.getVariantSku() != null) variant.setVariantSku(vReq.getVariantSku());

                    productVariantRepository.save(variant);
                }

                // Có variants → stock = tổng stock biến thể
                int totalStock = calculateVariantStockTotal(p.getProductId());
                p.setStockQuantity(totalStock);
            } else {
                // FE không gửi variants → giữ nguyên list trong DB
                if (!oldVariants.isEmpty()) {
                    // Vẫn có biến thể trong DB → luôn sync stock theo biến thể
                    int totalStock = calculateVariantStockTotal(p.getProductId());
                    p.setStockQuantity(totalStock);
                }
            }

            // Nếu không có biến thể (cả trong DB)
            if (oldVariants.isEmpty()) {
                if (req.getStockQuantity() != null) {
                    p.setStockQuantity(req.getStockQuantity());
                }
            }

            productRepository.save(p);

            return ResponseEntity.ok(
                    new BaseResponse<>(200, "✏️ Product updated successfully", toResponse(p))
            );

        } catch (Exception e) {
            System.err.println("\n===== UPDATE PRODUCT ERROR =====");
            System.err.println("ERROR TYPE: " + e.getClass().getName());
            System.err.println("ERROR MESSAGE: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    BaseResponse.error("❌ Update product failed: " + e.getMessage())
            );
        }
    }

    // ============================================================
    // 💡 Gán dữ liệu từ ProductRequest → Entity
    //  (convert List -> ArrayList để tránh ImmutableCollections)
    // ============================================================
    private void applyRequestToProduct(Product p, ProductRequest r) {
        p.setShortDescription(r.getShortDescription());
        p.setDescription(r.getDescription());
        p.setModel(r.getModel());
        p.setColor(r.getColor());
        p.setMaterial(r.getMaterial());
        p.setDimensions(r.getDimensions());
        p.setWeight(r.getWeight());

        if (r.getImages() != null) {
            p.setImages(new ArrayList<>(r.getImages()));
        }

        p.setVideoUrl(r.getVideoUrl());
        p.setWarehouseLocation(r.getWarehouseLocation());
        p.setShippingAddress(r.getShippingAddress());
        p.setProvinceCode(r.getProvinceCode());
        p.setDistrictCode(r.getDistrictCode());
        p.setWardCode(r.getWardCode());
        p.setStockQuantity(r.getStockQuantity());
        p.setShippingFee(r.getShippingFee());

        if (r.getSupportedShippingMethodIds() != null) {
            p.setSupportedShippingMethodIds(new ArrayList<>(r.getSupportedShippingMethodIds()));
        }

        if (r.getBulkDiscounts() != null) {
            p.setBulkDiscounts(
                    r.getBulkDiscounts().stream()
                            .map(b -> new Product.BulkDiscount(
                                    b.getFromQuantity(),
                                    b.getToQuantity(),
                                    b.getUnitPrice()
                            ))
                            .collect(Collectors.toList())   // mutable list
            );
        }

        p.setFrequencyResponse(r.getFrequencyResponse());
        p.setSensitivity(r.getSensitivity());
        p.setImpedance(r.getImpedance());
        p.setPowerHandling(r.getPowerHandling());
        p.setConnectionType(r.getConnectionType());
        p.setVoltageInput(r.getVoltageInput());
        p.setWarrantyPeriod(r.getWarrantyPeriod());
        p.setWarrantyType(r.getWarrantyType());
        p.setManufacturerName(r.getManufacturerName());
        p.setManufacturerAddress(r.getManufacturerAddress());
        p.setProductCondition(r.getProductCondition());
        p.setIsCustomMade(r.getIsCustomMade());
        p.setDriverConfiguration(r.getDriverConfiguration());
        p.setDriverSize(r.getDriverSize());
        p.setEnclosureType(r.getEnclosureType());
        p.setCoveragePattern(r.getCoveragePattern());
        p.setCrossoverFrequency(r.getCrossoverFrequency());
        p.setPlacementType(r.getPlacementType());
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
        p.setMicType(r.getMicType());
        p.setPolarPattern(r.getPolarPattern());
        p.setMaxSPL(r.getMaxSPL());
        p.setMicOutputImpedance(r.getMicOutputImpedance());
        p.setMicSensitivity(r.getMicSensitivity());
        p.setAmplifierType(r.getAmplifierType());
        p.setTotalPowerOutput(r.getTotalPowerOutput());
        p.setThd(r.getThd());
        p.setSnr(r.getSnr());
        p.setInputChannels(r.getInputChannels());
        p.setOutputChannels(r.getOutputChannels());
        p.setSupportBluetooth(r.getSupportBluetooth());
        p.setSupportWifi(r.getSupportWifi());
        p.setSupportAirplay(r.getSupportAirplay());
        p.setPlatterMaterial(r.getPlatterMaterial());
        p.setMotorType(r.getMotorType());
        p.setTonearmType(r.getTonearmType());
        p.setAutoReturn(r.getAutoReturn());
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
    }

    // ============================================================
    // 💡 Map từ UpdateProductRequest → Entity (convert list an toàn)
    // ============================================================
    private void mapUpdateRequestToProduct(Product p, UpdateProductRequest r) {

        // 🏷️ THÔNG TIN CƠ BẢN
        if (r.getBrandName() != null) p.setBrandName(r.getBrandName());
        if (r.getShortDescription() != null) p.setShortDescription(r.getShortDescription());
        if (r.getDescription() != null) p.setDescription(r.getDescription());
        if (r.getModel() != null) p.setModel(r.getModel());
        if (r.getColor() != null) p.setColor(r.getColor());
        if (r.getMaterial() != null) p.setMaterial(r.getMaterial());
        if (r.getDimensions() != null) p.setDimensions(r.getDimensions());
        if (r.getWeight() != null) p.setWeight(r.getWeight());
        if (r.getImages() != null) p.setImages(new ArrayList<>(r.getImages()));
        if (r.getVideoUrl() != null) p.setVideoUrl(r.getVideoUrl());

        // 💰 GIÁ & KHO (stockQuantity sẽ bị override nếu có variant)
        if (r.getWarehouseLocation() != null) p.setWarehouseLocation(r.getWarehouseLocation());
        if (r.getShippingAddress() != null) p.setShippingAddress(r.getShippingAddress());
        if (r.getProvinceCode() != null) p.setProvinceCode(r.getProvinceCode());
        if (r.getDistrictCode() != null) p.setDistrictCode(r.getDistrictCode());
        if (r.getWardCode() != null) p.setWardCode(r.getWardCode());
        if (r.getStockQuantity() != null) p.setStockQuantity(r.getStockQuantity());
        if (r.getShippingFee() != null) p.setShippingFee(r.getShippingFee());
        if (r.getSupportedShippingMethodIds() != null)
            p.setSupportedShippingMethodIds(new ArrayList<>(r.getSupportedShippingMethodIds()));

        // MUA NHIỀU GIẢM GIÁ
        if (r.getBulkDiscounts() != null)
            p.setBulkDiscounts(
                    r.getBulkDiscounts().stream()
                            .map(b -> new Product.BulkDiscount(
                                    b.getFromQuantity(),
                                    b.getToQuantity(),
                                    b.getUnitPrice()
                            ))
                            .collect(Collectors.toList())
            );

        // 📊 TRẠNG THÁI
        if (r.getStatus() != null) p.setStatus(r.getStatus());
        if (r.getIsFeatured() != null) p.setIsFeatured(r.getIsFeatured());

        // ⚙️ KỸ THUẬT & BẢO HÀNH
        if (r.getVoltageInput() != null) p.setVoltageInput(r.getVoltageInput());
        if (r.getWarrantyPeriod() != null) p.setWarrantyPeriod(r.getWarrantyPeriod());
        if (r.getWarrantyType() != null) p.setWarrantyType(r.getWarrantyType());
        if (r.getManufacturerName() != null) p.setManufacturerName(r.getManufacturerName());
        if (r.getManufacturerAddress() != null) p.setManufacturerAddress(r.getManufacturerAddress());
        if (r.getProductCondition() != null) p.setProductCondition(r.getProductCondition());
        if (r.getIsCustomMade() != null) p.setIsCustomMade(r.getIsCustomMade());

        // 🔊 LOA
        if (r.getDriverConfiguration() != null) p.setDriverConfiguration(r.getDriverConfiguration());
        if (r.getDriverSize() != null) p.setDriverSize(r.getDriverSize());
        if (r.getFrequencyResponse() != null) p.setFrequencyResponse(r.getFrequencyResponse());
        if (r.getSensitivity() != null) p.setSensitivity(r.getSensitivity());
        if (r.getImpedance() != null) p.setImpedance(r.getImpedance());
        if (r.getPowerHandling() != null) p.setPowerHandling(r.getPowerHandling());
        if (r.getEnclosureType() != null) p.setEnclosureType(r.getEnclosureType());
        if (r.getCoveragePattern() != null) p.setCoveragePattern(r.getCoveragePattern());
        if (r.getCrossoverFrequency() != null) p.setCrossoverFrequency(r.getCrossoverFrequency());
        if (r.getPlacementType() != null) p.setPlacementType(r.getPlacementType());
        if (r.getConnectionType() != null) p.setConnectionType(r.getConnectionType());

        // 🎧 TAI NGHE
        if (r.getHeadphoneType() != null) p.setHeadphoneType(r.getHeadphoneType());
        if (r.getCompatibleDevices() != null) p.setCompatibleDevices(r.getCompatibleDevices());
        if (r.getIsSportsModel() != null) p.setIsSportsModel(r.getIsSportsModel());
        if (r.getHeadphoneFeatures() != null) p.setHeadphoneFeatures(r.getHeadphoneFeatures());
        if (r.getBatteryCapacity() != null) p.setBatteryCapacity(r.getBatteryCapacity());
        if (r.getHasBuiltInBattery() != null) p.setHasBuiltInBattery(r.getHasBuiltInBattery());
        if (r.getIsGamingHeadset() != null) p.setIsGamingHeadset(r.getIsGamingHeadset());
        if (r.getHeadphoneAccessoryType() != null) p.setHeadphoneAccessoryType(r.getHeadphoneAccessoryType());
        if (r.getHeadphoneConnectionType() != null) p.setHeadphoneConnectionType(r.getHeadphoneConnectionType());
        if (r.getPlugType() != null) p.setPlugType(r.getPlugType());
        if (r.getSirimApproved() != null) p.setSirimApproved(r.getSirimApproved());
        if (r.getSirimCertified() != null) p.setSirimCertified(r.getSirimCertified());
        if (r.getMcmcApproved() != null) p.setMcmcApproved(r.getMcmcApproved());

        // 🎤 MICRO
        if (r.getMicType() != null) p.setMicType(r.getMicType());
        if (r.getPolarPattern() != null) p.setPolarPattern(r.getPolarPattern());
        if (r.getMaxSPL() != null) p.setMaxSPL(r.getMaxSPL());
        if (r.getMicOutputImpedance() != null) p.setMicOutputImpedance(r.getMicOutputImpedance());
        if (r.getMicSensitivity() != null) p.setMicSensitivity(r.getMicSensitivity());

        // 📻 AMPLI / RECEIVER
        if (r.getAmplifierType() != null) p.setAmplifierType(r.getAmplifierType());
        if (r.getTotalPowerOutput() != null) p.setTotalPowerOutput(r.getTotalPowerOutput());
        if (r.getThd() != null) p.setThd(r.getThd());
        if (r.getSnr() != null) p.setSnr(r.getSnr());
        if (r.getInputChannels() != null) p.setInputChannels(r.getInputChannels());
        if (r.getOutputChannels() != null) p.setOutputChannels(r.getOutputChannels());
        if (r.getSupportBluetooth() != null) p.setSupportBluetooth(r.getSupportBluetooth());
        if (r.getSupportWifi() != null) p.setSupportWifi(r.getSupportWifi());
        if (r.getSupportAirplay() != null) p.setSupportAirplay(r.getSupportAirplay());

        // 📀 TURNTABLE
        if (r.getPlatterMaterial() != null) p.setPlatterMaterial(r.getPlatterMaterial());
        if (r.getMotorType() != null) p.setMotorType(r.getMotorType());
        if (r.getTonearmType() != null) p.setTonearmType(r.getTonearmType());
        if (r.getAutoReturn() != null) p.setAutoReturn(r.getAutoReturn());

        // 🎛️ DAC / MIXER / SOUND CARD
        if (r.getDacChipset() != null) p.setDacChipset(r.getDacChipset());
        if (r.getSampleRate() != null) p.setSampleRate(r.getSampleRate());
        if (r.getBitDepth() != null) p.setBitDepth(r.getBitDepth());
        if (r.getBalancedOutput() != null) p.setBalancedOutput(r.getBalancedOutput());
        if (r.getInputInterface() != null) p.setInputInterface(r.getInputInterface());
        if (r.getOutputInterface() != null) p.setOutputInterface(r.getOutputInterface());
        if (r.getChannelCount() != null) p.setChannelCount(r.getChannelCount());
        if (r.getHasPhantomPower() != null) p.setHasPhantomPower(r.getHasPhantomPower());
        if (r.getEqBands() != null) p.setEqBands(r.getEqBands());
        if (r.getFaderType() != null) p.setFaderType(r.getFaderType());
        if (r.getBuiltInEffects() != null) p.setBuiltInEffects(r.getBuiltInEffects());
        if (r.getUsbAudioInterface() != null) p.setUsbAudioInterface(r.getUsbAudioInterface());
        if (r.getMidiSupport() != null) p.setMidiSupport(r.getMidiSupport());
    }

    // ============================================================
    // 🔻 DISABLE PRODUCT
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> disableProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Product not found"));

        product.setStatus(ProductStatus.INACTIVE);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        return ResponseEntity.ok(
                new BaseResponse<>(200, "🚫 Product disabled successfully", toResponse(product))
        );
    }

    // ============================================================
    // 🔎 GET PRODUCT BY ID
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> getProductById(UUID id) {
        try {
            Product p = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("❌ Product not found"));

            return ResponseEntity.ok(
                    new BaseResponse<>(200, "🔎 Product detail", toResponse(p))
            );

        } catch (Exception e) {
            System.err.println("❌ [getProductById ERROR] ID = " + id);
            System.err.println("❌ Error Type: " + e.getClass().getSimpleName());
            System.err.println("❌ Error Message: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    BaseResponse.error("❌ getProductById failed: " + e.getMessage())
            );
        }
    }

    // ============================================================
    // 📦 GET ALL PRODUCTS (FILTER + PAGING)
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> getAllProducts(
            String categoryName,
            UUID storeId,
            String keyword,
            int page,
            int size,
            ProductStatus status
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Product> products = productRepository.findAll(pageable);

            List<String> validCategoryNames = List.of(
                    "Tai Nghe", "Loa", "Micro", "DAC", "Mixer", "Amp",
                    "Turntable", "Sound Card", "DJ Controller", "Combo"
            );

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

            return ResponseEntity.ok(
                    new BaseResponse<>(200, "📦 Product list filtered successfully", filtered)
            );

        } catch (Exception e) {
            System.err.println("❌ [getAllProducts ERROR]");
            System.err.println("   categoryName = " + categoryName);
            System.err.println("   storeId      = " + storeId);
            System.err.println("   keyword      = " + keyword);
            System.err.println("   page/size    = " + page + "/" + size);
            System.err.println("   status       = " + status);
            System.err.println("❌ Error Type: " + e.getClass().getSimpleName());
            System.err.println("❌ Error Message: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    BaseResponse.error("❌ getAllProducts failed: " + e.getMessage())
            );
        }
    }

    // ============================================================
    // 💡 Convert Entity → ProductResponse (FULL)
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
                .shortDescription(p.getShortDescription())
                .description(p.getDescription())
                .model(p.getModel())
                .color(p.getColor())
                .material(p.getMaterial())
                .dimensions(p.getDimensions())
                .weight(p.getWeight())
                // Lấy danh sách biến thể
                .variants(
                        productVariantRepository.findAllByProduct_ProductId(p.getProductId())
                                .stream()
                                .map(v -> new ProductResponse.VariantResponse(
                                        v.getId(),
                                        v.getOptionName(),
                                        v.getOptionValue(),
                                        v.getVariantPrice(),
                                        v.getVariantStock(),
                                        v.getVariantUrl(),
                                        v.getVariantSku()
                                ))
                                .toList()
                )
                .images(p.getImages())
                .videoUrl(p.getVideoUrl())
                .sku(p.getSku())
                .price(p.getPrice())
                .discountPrice(p.getDiscountPrice())
                .promotionPercent(p.getPromotionPercent())
                .priceAfterPromotion(p.getPriceAfterPromotion())
                .priceBeforeVoucher(p.getPriceBeforeVoucher())
                .voucherAmount(p.getVoucherAmount())
                .finalPrice(p.getFinalPrice())
                .platformFeePercent(p.getPlatformFeePercent())
                .currency(p.getCurrency())
                .stockQuantity(p.getStockQuantity())
                .warehouseLocation(p.getWarehouseLocation())
                .shippingAddress(p.getShippingAddress())
                .provinceCode(p.getProvinceCode())
                .districtCode(p.getDistrictCode())
                .wardCode(p.getWardCode())
                .shippingFee(p.getShippingFee())
                .supportedShippingMethodIds(p.getSupportedShippingMethodIds())
                .bulkDiscounts(p.getBulkDiscounts() != null
                        ? p.getBulkDiscounts().stream()
                        .map(b -> new ProductResponse.BulkDiscountResponse(
                                b.getFromQuantity(),
                                b.getToQuantity(),
                                b.getUnitPrice()))
                        .toList()
                        : null)
                .status(p.getStatus())
                .isFeatured(p.getIsFeatured())
                .ratingAverage(p.getRatingAverage())
                .reviewCount(p.getReviewCount())
                .viewCount(p.getViewCount())
                .frequencyResponse(p.getFrequencyResponse())
                .sensitivity(p.getSensitivity())
                .impedance(p.getImpedance())
                .powerHandling(p.getPowerHandling())
                .connectionType(p.getConnectionType())
                .voltageInput(p.getVoltageInput())
                .warrantyPeriod(p.getWarrantyPeriod())
                .warrantyType(p.getWarrantyType())
                .manufacturerName(p.getManufacturerName())
                .manufacturerAddress(p.getManufacturerAddress())
                .productCondition(p.getProductCondition())
                .isCustomMade(p.getIsCustomMade())
                .driverConfiguration(p.getDriverConfiguration())
                .driverSize(p.getDriverSize())
                .enclosureType(p.getEnclosureType())
                .coveragePattern(p.getCoveragePattern())
                .crossoverFrequency(p.getCrossoverFrequency())
                .placementType(p.getPlacementType())
                .headphoneType(p.getHeadphoneType())
                .compatibleDevices(p.getCompatibleDevices())
                .isSportsModel(p.getIsSportsModel())
                .headphoneFeatures(p.getHeadphoneFeatures())
                .batteryCapacity(p.getBatteryCapacity())
                .hasBuiltInBattery(p.getHasBuiltInBattery())
                .isGamingHeadset(p.getIsGamingHeadset())
                .headphoneAccessoryType(p.getHeadphoneAccessoryType())
                .headphoneConnectionType(p.getHeadphoneConnectionType())
                .plugType(p.getPlugType())
                .sirimApproved(p.getSirimApproved())
                .sirimCertified(p.getSirimCertified())
                .mcmcApproved(p.getMcmcApproved())
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
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .lastUpdatedAt(p.getLastUpdatedAt())
                .lastUpdateIntervalDays(p.getLastUpdateIntervalDays())
                .createdBy(p.getCreatedBy())
                .updatedBy(p.getUpdatedBy())
                .build();
    }
}
