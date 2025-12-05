package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ShopVoucherRequest;
import org.example.audio_ecommerce.dto.request.ShopWideVoucherRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.ShopVoucherResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.ShopVoucherScopeType;
import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.ShopVoucherService;
import org.example.audio_ecommerce.util.VoucherCodeGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ShopVoucherServiceImpl implements ShopVoucherService {

    private final ShopVoucherRepository voucherRepository;
    private final ShopVoucherProductRepository voucherProductRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;

    // ============================================================
    // 🎲 Helper: Sinh mã voucher unique
    // ============================================================
    private String generateUniqueVoucherCode() {
        String code;
        int maxRetries = 10;
        int attempts = 0;

        do {
            code = VoucherCodeGenerator.generateCode();
            attempts++;

            if (attempts >= maxRetries) {
                throw new RuntimeException("❌ Không thể tạo mã voucher unique sau " + maxRetries + " lần thử");
            }
        } while (voucherRepository.existsByCodeIgnoreCase(code));

        return code;
    }

    // ============================================================
    // ➕ Tạo Voucher cho nhiều sản phẩm
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse<ShopVoucherResponse>> createVoucher(ShopVoucherRequest req) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Store not found for current user"));

        // 🎲 Tự sinh code
        String voucherCode = (req.getCode() == null || req.getCode().trim().isEmpty())
                ? generateUniqueVoucherCode()
                : req.getCode().toUpperCase();

        if (voucherRepository.existsByCodeIgnoreCase(voucherCode))
            throw new RuntimeException("❌ Voucher code already exists: " + voucherCode);

        LocalDateTime now = LocalDateTime.now();

        // =====================================================
        // 🔥 FIX TIMEZONE: tự trừ 7 giờ khi lưu
        // =====================================================
        LocalDateTime fixedStart = req.getStartTime().minusHours(7);
        LocalDateTime fixedEnd = req.getEndTime().minusHours(7);

        // ========== VALIDATE TIME RANGE (validate theo fixed) ==========
        if (req.getStartTime() == null || req.getEndTime() == null) {
            throw new RuntimeException("❌ Start time và End time không được để trống");
        }

        if (!fixedStart.isBefore(fixedEnd)) {
            throw new RuntimeException("❌ Start time phải nhỏ hơn End time");
        }

        if (fixedEnd.isBefore(now)) {
            throw new RuntimeException("❌ End time phải lớn hơn thời điểm hiện tại");
        }
        // ================================================================

        // === Khởi tạo voucher (sử dụng fixed time)
        ShopVoucher voucher = ShopVoucher.builder()
                .shop(store)
                .code(voucherCode)
                .title(req.getTitle())
                .description(req.getDescription())
                .type(req.getType())
                .discountValue(req.getDiscountValue())
                .discountPercent(req.getDiscountPercent())
                .maxDiscountValue(req.getMaxDiscountValue())
                .minOrderValue(req.getMinOrderValue())
                .totalVoucherIssued(req.getTotalVoucherIssued())
                .usagePerUser(req.getUsagePerUser())
                .startTime(fixedStart)
                .endTime(fixedEnd)
                .status(VoucherStatus.ACTIVE)
                .scopeType(ShopVoucherScopeType.PRODUCT_VOUCHER)
                .createdAt(now)
                .updatedAt(now)
                .lastUpdatedAt(now)
                .lastUpdateIntervalDays(0L)
                .createdBy(store.getAccount().getId())
                .updatedBy(store.getAccount().getId())
                .build();

        // === Gán sản phẩm
        List<ShopVoucherProduct> appliedProducts = new ArrayList<>();

        if (req.getProducts() != null && !req.getProducts().isEmpty()) {
            for (ShopVoucherRequest.VoucherProductItem item : req.getProducts()) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new RuntimeException("❌ Product not found: " + item.getProductId()));

                if (!product.getStore().getStoreId().equals(store.getStoreId())) {
                    throw new RuntimeException("❌ Product does not belong to current store: " + product.getName());
                }

                boolean hasActiveVoucher = voucherProductRepository.existsByProduct_ProductIdAndVoucher_Status(
                        product.getProductId(),
                        VoucherStatus.ACTIVE
                );

                if (hasActiveVoucher) {
                    throw new RuntimeException("⚠️ Product '" + product.getName() +
                            "' đã nằm trong một voucher ACTIVE khác. Hãy disable voucher cũ trước khi thêm mới.");
                }

                ShopVoucherProduct vp = ShopVoucherProduct.builder()
                        .voucher(voucher)
                        .product(product)
                        .promotionStockLimit(item.getPromotionStockLimit())
                        .purchaseLimitPerCustomer(item.getPurchaseLimitPerCustomer())
                        .active(true)
                        .build();

                appliedProducts.add(vp);
            }
        }

        voucher.setVoucherProducts(appliedProducts);
        voucherRepository.save(voucher);

        ShopVoucherResponse response = ShopVoucherResponse.fromEntity(voucher);
        return ResponseEntity.ok(new BaseResponse<>(201, "✅ Voucher created and linked to products", response));
    }

    // ============================================================
    // ➕ Tạo Voucher toàn shop
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse<ShopVoucherResponse>> createShopWideVoucher(ShopWideVoucherRequest req) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Store not found for current user"));

        String voucherCode = (req.getCode() == null || req.getCode().trim().isEmpty())
                ? generateUniqueVoucherCode()
                : req.getCode().toUpperCase();

        if (voucherRepository.existsByCodeIgnoreCase(voucherCode))
            throw new RuntimeException("❌ Voucher code already exists: " + voucherCode);

        LocalDateTime now = LocalDateTime.now();

        // =====================================================
        // 🔥 FIX TIMEZONE: tự trừ 7 giờ
        // =====================================================
        LocalDateTime fixedStart = req.getStartTime().minusHours(7);
        LocalDateTime fixedEnd   = req.getEndTime().minusHours(7);

        // Validate theo fixed
        if (!fixedStart.isBefore(fixedEnd)) {
            throw new RuntimeException("❌ Start time phải nhỏ hơn End time");
        }
        if (fixedEnd.isBefore(now)) {
            throw new RuntimeException("❌ End time phải lớn hơn thời điểm hiện tại");
        }

        ShopVoucher voucher = ShopVoucher.builder()
                .shop(store)
                .code(voucherCode)
                .title(req.getTitle())
                .description(req.getDescription())
                .type(req.getType())
                .discountValue(req.getDiscountValue())
                .discountPercent(req.getDiscountPercent())
                .maxDiscountValue(req.getMaxDiscountValue())
                .minOrderValue(req.getMinOrderValue())
                .totalVoucherIssued(req.getTotalVoucherIssued())
                .usagePerUser(req.getUsagePerUser())
                .remainingUsage(req.getRemainingUsage() != null ? req.getRemainingUsage() : req.getTotalVoucherIssued())
                .scopeType(ShopVoucherScopeType.ALL_SHOP_VOUCHER)
                .startTime(fixedStart)
                .endTime(fixedEnd)
                .status(VoucherStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .lastUpdatedAt(now)
                .lastUpdateIntervalDays(0L)
                .createdBy(store.getAccount().getId())
                .updatedBy(store.getAccount().getId())
                .voucherProducts(new ArrayList<>())
                .build();

        voucherRepository.save(voucher);

        ShopVoucherResponse response = ShopVoucherResponse.fromEntity(voucher);
        return ResponseEntity.ok(new BaseResponse<>(201, "✅ Voucher toàn shop đã được tạo", response));
    }

    // ============================================================
    // Các API còn lại giữ nguyên
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse<List<ShopVoucherResponse>>> getAllVouchers() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Store not found"));

        List<ShopVoucher> vouchers = voucherRepository.findAll()
                .stream()
                .filter(v -> v.getShop().getStoreId().equals(store.getStoreId()))
                .toList();

        List<ShopVoucherResponse> dtoList = vouchers.stream()
                .map(ShopVoucherResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(new BaseResponse<>(200, "📦 List of vouchers for store", dtoList));
    }

    @Override
    public ResponseEntity<BaseResponse<ShopVoucherResponse>> getVoucherById(UUID id) {
        ShopVoucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Voucher not found"));
        return ResponseEntity.ok(new BaseResponse<>(200, "🔎 Voucher detail", ShopVoucherResponse.fromEntity(voucher)));
    }

    @Override
    public ResponseEntity<BaseResponse<ShopVoucherResponse>> disableVoucher(UUID id) {
        ShopVoucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Voucher not found"));

        voucher.setStatus(voucher.getStatus() == VoucherStatus.ACTIVE
                ? VoucherStatus.DISABLED
                : VoucherStatus.ACTIVE);
        voucher.setUpdatedAt(LocalDateTime.now());
        voucherRepository.save(voucher);

        return ResponseEntity.ok(new BaseResponse<>(200, "🔄 Voucher status updated", ShopVoucherResponse.fromEntity(voucher)));
    }

    @Override
    public ResponseEntity<BaseResponse<ShopVoucherResponse>> getActiveVoucherByProductId(UUID productId) {
        ShopVoucherProduct vp = voucherProductRepository
                .findFirstByProduct_ProductIdAndVoucher_Status(productId, VoucherStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("❌ Sản phẩm này chưa có voucher ACTIVE nào áp dụng"));

        ShopVoucher voucher = vp.getVoucher();

        return ResponseEntity.ok(new BaseResponse<>(200,
                "🎟️ Voucher ACTIVE của sản phẩm",
                ShopVoucherResponse.fromEntity(voucher)
        ));
    }

    @Override
    public ResponseEntity<BaseResponse<List<ShopVoucherResponse>>> getActiveVouchersByType(VoucherStatus status, ShopVoucherScopeType scopeType) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;
        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Store not found"));
        List<ShopVoucher> vouchers;
        if (scopeType != null) {
            vouchers = voucherRepository.findAllByShop_StoreIdAndStatusAndScopeType(store.getStoreId(), status, scopeType);
        } else {
            vouchers = voucherRepository.findAllByShop_StoreIdAndStatus(store.getStoreId(), status);
        }
        List<ShopVoucherResponse> dtoList = vouchers.stream().map(ShopVoucherResponse::fromEntity).toList();
        return ResponseEntity.ok(new BaseResponse<>(200, "📦 List of vouchers by status and type", dtoList));
    }

    @Override
    public ResponseEntity<BaseResponse<List<ShopVoucherResponse>>> getVouchersByStore(UUID storeId, VoucherStatus status, ShopVoucherScopeType scopeType) {
        List<ShopVoucher> vouchers;
        if (status != null && scopeType != null) {
            vouchers = voucherRepository.findAllByShop_StoreIdAndStatusAndScopeType(storeId, status, scopeType);
        } else if (status != null) {
            vouchers = voucherRepository.findAllByShop_StoreIdAndStatus(storeId, status);
        } else if (scopeType != null) {
            vouchers = voucherRepository.findAllByShop_StoreIdAndScopeType(storeId, scopeType);
        } else {
            vouchers = voucherRepository.findAllByShop_StoreId(storeId);
        }
        List<ShopVoucherResponse> dtoList = vouchers.stream().map(ShopVoucherResponse::fromEntity).toList();
        return ResponseEntity.ok(new BaseResponse<>(200, "📦 List of vouchers by storeId, status, and type", dtoList));
    }

    @Override
    public ResponseEntity<BaseResponse<String>> generateVoucherCode() {
        String code = generateUniqueVoucherCode();
        return ResponseEntity.ok(new BaseResponse<>(200, "🎲 Generated voucher code", code));
    }

}
