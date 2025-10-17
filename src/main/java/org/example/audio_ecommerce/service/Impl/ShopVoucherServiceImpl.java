package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ShopVoucherRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.ShopVoucherResponse;
import org.example.audio_ecommerce.entity.*;
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
    // ➕ Tạo Voucher cho nhiều sản phẩm
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

        // === Khởi tạo Voucher
        ShopVoucher voucher = new ShopVoucher();
        voucher.setShop(store);
        voucher.setCode(req.getCode().toUpperCase());
        voucher.setTitle(req.getTitle());
        voucher.setDescription(req.getDescription());
        voucher.setType(req.getType());
        voucher.setDiscountValue(req.getDiscountValue());
        voucher.setDiscountPercent(req.getDiscountPercent());
        voucher.setMaxDiscountValue(req.getMaxDiscountValue());
        voucher.setMinOrderValue(req.getMinOrderValue());
        voucher.setTotalVoucherIssued(req.getTotalVoucherIssued());
        voucher.setTotalUsageLimit(req.getTotalUsageLimit());
        voucher.setUsagePerUser(req.getUsagePerUser());
        voucher.setRemainingUsage(req.getTotalUsageLimit());
        voucher.setStartTime(req.getStartTime());
        voucher.setEndTime(req.getEndTime());
        voucher.setStatus(VoucherStatus.ACTIVE);
        voucher.setCreatedAt(now);
        voucher.setUpdatedAt(now);
        voucher.setLastUpdatedAt(now);
        voucher.setLastUpdateIntervalDays(0L);
        voucher.setCreatedBy(store.getAccount().getId());
        voucher.setUpdatedBy(store.getAccount().getId());

        // === Áp dụng voucher cho sản phẩm
        List<ShopVoucherProduct> appliedProducts = new ArrayList<>();

        if (req.getProducts() != null && !req.getProducts().isEmpty()) {
            for (ShopVoucherRequest.VoucherProductItem item : req.getProducts()) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new RuntimeException("❌ Product not found: " + item.getProductId()));

                // kiểm tra sản phẩm có thuộc store hiện tại không
                if (!product.getStore().getStoreId().equals(store.getStoreId())) {
                    throw new RuntimeException("❌ Product does not belong to current store: " + product.getName());
                }

                ShopVoucherProduct vp = new ShopVoucherProduct();
                vp.setVoucher(voucher);
                vp.setProduct(product);
                vp.setOriginalPrice(product.getPrice());
                vp.setDiscountPercent(item.getDiscountPercent());
                vp.setDiscountAmount(item.getDiscountAmount());

                // tính giá giảm
                if (item.getDiscountPercent() != null) {
                    vp.setDiscountedPrice(
    product.getPrice().subtract(
        product.getPrice().multiply(
            new java.math.BigDecimal(item.getDiscountPercent())
                .divide(new java.math.BigDecimal(100))
        )
    )
);
                } else if (item.getDiscountAmount() != null) {
                    vp.setDiscountedPrice(product.getPrice().subtract(item.getDiscountAmount()));
                } else {
                    vp.setDiscountedPrice(product.getPrice());
                }

                vp.setStock(product.getStockQuantity());
                vp.setPromotionStockLimit(item.getPromotionStockLimit());
                vp.setPurchaseLimitPerCustomer(item.getPurchaseLimitPerCustomer());
                vp.setActive(true);

                appliedProducts.add(vp);
            }
        }

        voucher.setVoucherProducts(appliedProducts);
        voucherRepository.save(voucher); // cascade ALL

        // 🔄 Trả về DTO thay vì Entity
        ShopVoucherResponse response = ShopVoucherResponse.fromEntity(voucher);
        return ResponseEntity.ok(new BaseResponse<>(201, "✅ Voucher created and applied to products", response));
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
}
