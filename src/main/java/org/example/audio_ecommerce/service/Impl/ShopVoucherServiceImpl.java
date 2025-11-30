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
    // ➕ Tạo Voucher cho nhiều sản phẩm (runtime logic)
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> createVoucher(ShopVoucherRequest req) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Store not found for current user"));

        if (voucherRepository.existsByCodeIgnoreCase(req.getCode()))
            throw new RuntimeException("❌ Voucher code already exists: " + req.getCode());

        LocalDateTime now = LocalDateTime.now();

        // === Khởi tạo voucher ===
        ShopVoucher voucher = ShopVoucher.builder()
                .shop(store)
                .code(req.getCode().toUpperCase())
                .title(req.getTitle())
                .description(req.getDescription())
                .type(req.getType())
                .discountValue(req.getDiscountValue())
                .discountPercent(req.getDiscountPercent())
                .maxDiscountValue(req.getMaxDiscountValue())
                .minOrderValue(req.getMinOrderValue())
                .totalVoucherIssued(req.getTotalVoucherIssued())
                .usagePerUser(req.getUsagePerUser())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .status(VoucherStatus.ACTIVE)
                .scopeType(ShopVoucherScopeType.PRODUCT_VOUCHER) // Luôn set là PRODUCT_VOUCHER khi tạo voucher sản phẩm
                .createdAt(now)
                .updatedAt(now)
                .lastUpdatedAt(now)
                .lastUpdateIntervalDays(0L)
                .createdBy(store.getAccount().getId())
                .updatedBy(store.getAccount().getId())
                .build();

        // === Gán voucher cho sản phẩm (chỉ lưu liên kết, không tính giá) ===
        List<ShopVoucherProduct> appliedProducts = new ArrayList<>();

        if (req.getProducts() != null && !req.getProducts().isEmpty()) {
            for (ShopVoucherRequest.VoucherProductItem item : req.getProducts()) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new RuntimeException("❌ Product not found: " + item.getProductId()));

                if (!product.getStore().getStoreId().equals(store.getStoreId())) {
                    throw new RuntimeException("❌ Product does not belong to current store: " + product.getName());
                }

                // 🔹 RULE: 1 sản phẩm chỉ có thể nằm trong 1 voucher ACTIVE duy nhất
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
        voucherRepository.save(voucher); // Cascade ALL sẽ tự lưu voucherProducts

        ShopVoucherResponse response = ShopVoucherResponse.fromEntity(voucher);
        return ResponseEntity.ok(new BaseResponse<>(201, "✅ Voucher created and linked to products", response));
    }

    // ============================================================
    // 📜 Lấy tất cả voucher cửa hàng
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> getAllVouchers() {
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

    // ============================================================
    // 🔍 Lấy chi tiết voucher
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> getVoucherById(UUID id) {
        ShopVoucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("❌ Voucher not found"));
        return ResponseEntity.ok(new BaseResponse<>(200, "🔎 Voucher detail", ShopVoucherResponse.fromEntity(voucher)));
    }

    // ============================================================
    // 🚫 Disable / Enable Voucher
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> disableVoucher(UUID id) {
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
    public ResponseEntity<BaseResponse> getActiveVoucherByProductId(UUID productId) {
        ShopVoucherProduct vp = voucherProductRepository
                .findFirstByProduct_ProductIdAndVoucher_Status(productId, VoucherStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("❌ Sản phẩm này chưa có voucher ACTIVE nào áp dụng"));

        ShopVoucher voucher = vp.getVoucher();

        return ResponseEntity.ok(new BaseResponse<>(200,
                "🎟️ Voucher ACTIVE của sản phẩm",
                ShopVoucherResponse.fromEntity(voucher)
        ));
    }

    // ============================================================
    // ➕ Tạo Voucher toàn shop (không giới hạn số lượng, không liên kết sản phẩm)
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> createShopWideVoucher(ShopWideVoucherRequest req) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Store not found for current user"));

        if (voucherRepository.existsByCodeIgnoreCase(req.getCode()))
            throw new RuntimeException("❌ Voucher code already exists: " + req.getCode());

        LocalDateTime now = LocalDateTime.now();

        ShopVoucher.ShopVoucherBuilder builder = ShopVoucher.builder()
                .shop(store)
                .code(req.getCode().toUpperCase())
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
                .scopeType(org.example.audio_ecommerce.entity.Enum.ShopVoucherScopeType.ALL_SHOP_VOUCHER)
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .status(VoucherStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .lastUpdatedAt(now)
                .lastUpdateIntervalDays(0L)
                .createdBy(store.getAccount().getId())
                .updatedBy(store.getAccount().getId())
                .voucherProducts(new ArrayList<>());

        ShopVoucher voucher = builder.build();

        voucherRepository.save(voucher);
        ShopVoucherResponse response = ShopVoucherResponse.fromEntity(voucher);
        return ResponseEntity.ok(new BaseResponse<>(201, "✅ Voucher toàn shop đã được tạo", response));
    }

    // ============================================================
    // 📦 Lấy voucher theo trạng thái và loại scopeType
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> getActiveVouchersByType(VoucherStatus status, ShopVoucherScopeType scopeType) {
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

    // ============================================================
    // 📦 Lấy voucher theo storeId, trạng thái và loại scopeType
    // ============================================================
    @Override
    public ResponseEntity<BaseResponse> getVouchersByStore(UUID storeId, VoucherStatus status, ShopVoucherScopeType scopeType) {
        // Lấy tất cả voucher của một cửa hàng theo storeId, có thể lọc theo trạng thái và loại voucher
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

}
