package org.example.audio_ecommerce.service.Impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.StoreWalletService;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@Service
@RequiredArgsConstructor
public class StoreWalletServiceImpl implements StoreWalletService {

    private final StoreRepository storeRepository;
    private final StoreWalletRepository storeWalletRepository;
    private final StoreWalletTransactionRepository storeWalletTransactionRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final ReturnShippingFeeRepository returnShippingFeeRepository;
    private final PlatformTransactionRepository platformTransactionRepository;
    private final PlatformWalletRepository platformWalletRepository;
    private final ProductRepository productRepository;


    /**
     * ✅ Lấy thông tin ví của cửa hàng đang đăng nhập
     * (Bao gồm: tiền khả dụng, tiền pending, tổng doanh thu, và tiền ký quỹ)
     */
    @Override
    public ResponseEntity<BaseResponse> getMyWallet() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy store cho tài khoản: " + email));

        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(store.getStoreId())
                .orElseThrow(() -> new RuntimeException("❌ Cửa hàng này chưa có ví."));

        StoreWalletSummaryResponse response = StoreWalletSummaryResponse.builder()
                .storeId(store.getStoreId())
                .storeName(store.getStoreName())
                .walletId(wallet.getWalletId())
                .availableBalance(wallet.getAvailableBalance())
                .pendingBalance(wallet.getPendingBalance())
                .depositBalance(wallet.getDepositBalance())
                .totalRevenue(wallet.getTotalRevenue())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();

        return ResponseEntity.ok(
                new BaseResponse<>(200, "✅ Lấy thông tin ví thành công", response)
        );
    }

    /**
     * 📜 Lấy danh sách giao dịch ví (phân trang + lọc loại giao dịch)
     */
    @Override
    public ResponseEntity<BaseResponse> getMyWalletTransactions(int page, int size, String type) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy store cho tài khoản: " + email));

        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(store.getStoreId())
                .orElseThrow(() -> new RuntimeException("❌ Cửa hàng này chưa có ví."));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<StoreWalletTransaction> transactions;

        if (type != null && !type.isBlank()) {
            StoreWalletTransactionType enumType;
            try {
                enumType = StoreWalletTransactionType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("❌ Loại giao dịch không hợp lệ: " + type);
            }
            transactions = storeWalletTransactionRepository
                    .findByWallet_WalletIdAndTypeOrderByCreatedAtDesc(wallet.getWalletId(), enumType, pageable);
        } else {
            transactions = storeWalletTransactionRepository
                    .findByWallet_WalletIdOrderByCreatedAtDesc(wallet.getWalletId(), pageable);
        }

        // 🔄 Chuyển sang DTO
        List<StoreWalletTransactionResponse> items = transactions.getContent().stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());

        PagedResult<StoreWalletTransactionResponse> result = PagedResult.<StoreWalletTransactionResponse>builder()
                .items(items)
                .page(page)
                .size(size)
                .totalElements(transactions.getTotalElements())
                .totalPages(transactions.getTotalPages())
                .build();

        return ResponseEntity.ok(new BaseResponse<>(200, "📜 Lấy danh sách giao dịch thành công", result));
    }

    /**
     * 🔎 Lọc giao dịch theo thời gian, loại, transactionId, v.v.
     */
    @Override
    public Page<StoreWalletTransactionResponse> filterTransactions(
            UUID walletId,
            LocalDateTime from,
            LocalDateTime to,
            StoreWalletTransactionType type,
            UUID transactionId,
            Pageable pageable
    ) {
        // ✅ Nếu không có walletId, tự động lấy từ tài khoản đang đăng nhập
        if (walletId == null) {
            walletId = getCurrentStoreWalletId();
        }

        // ✅ Kiểm tra điều kiện thời gian hợp lệ
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Thời gian 'from' phải nhỏ hơn hoặc bằng 'to'");
        }

        // ✅ Lấy dữ liệu từ repository
        Page<StoreWalletTransaction> transactionsPage = storeWalletTransactionRepository.filterTransactions(
                walletId, from, to, type, transactionId, pageable
        );

        // ✅ Ánh xạ sang DTO
        return transactionsPage.map(this::mapToTransactionResponse);
    }

    /**
     * 🧩 Hàm tiện ích: Lấy walletId của cửa hàng đang đăng nhập
     */
    private UUID getCurrentStoreWalletId() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy cửa hàng cho tài khoản: " + email));

        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(store.getStoreId())
                .orElseThrow(() -> new RuntimeException("❌ Cửa hàng này chưa có ví."));

        return wallet.getWalletId();
    }

    private StoreWalletTransactionResponse mapToTransactionResponse(StoreWalletTransaction tx) {
        return StoreWalletTransactionResponse.builder()
                .transactionId(tx.getTransactionId())
                .walletId(tx.getWallet().getWalletId())
                .orderId(tx.getOrderId())
                .amount(tx.getAmount())
                .balanceAfter(tx.getBalanceAfter())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .type(tx.getType()) // ✅ Giữ Enum
                .displayType(getDisplayName(tx.getType())) // ✅ Thêm tên thân thiện
                .build();
    }

    /**
     * 🧩 Hàm helper hiển thị text dễ đọc cho FE
     */
    private String getDisplayName(StoreWalletTransactionType type) {
        if (type == null) return "Không xác định";
        return switch (type) {
            case DEPOSIT -> "Nạp tiền vào ví";
            case PENDING_HOLD -> "Giữ tiền chờ xác nhận";
            case RELEASE_PENDING -> "Giải phóng tiền chờ";
            case WITHDRAW -> "Rút tiền về ngân hàng";
            case REFUND -> "Hoàn tiền cho khách hàng";
            case ADJUSTMENT -> "Điều chỉnh thủ công";
            case REFUND_RETURN -> "Hoàn trả hàng";
            case REFUND_FORCE -> "Hoàn tiền (bắt buộc)";
            case TOPUP -> "Nạp tiền vào ví";
            case DEBT_PAYMENT -> "Phí dịch vụ";
        };
    }

    @Override
    public UUID resolveWalletIdForCurrentUser() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy cửa hàng cho tài khoản: " + email));

        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(store.getStoreId())
                .orElseThrow(() -> new RuntimeException("❌ Cửa hàng này chưa có ví."));

        return wallet.getWalletId();
    }


    //============================Thanh toán nợ từ default balance============================
    @Transactional
    public ResponseEntity<BaseResponse> payMyDebtFromDefaultBalance() {

        // 1) Resolve store hiện tại
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy store cho tài khoản: " + email));

        // 2) Load wallet
        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(store.getStoreId())
                .orElseThrow(() -> new RuntimeException("❌ Cửa hàng này chưa có ví."));

        // 3) Lấy các khoản nợ FINAL chưa trả (StoreOrder)
        List<StoreOrder> unpaidFinalOrders =
                storeOrderRepository.findUnpaidFinalOrdersOfStore(store.getStoreId());

        // 4) Lấy các khoản return fee SHOP chịu chưa trả
        List<ReturnShippingFee> unpaidReturnFees =
                returnShippingFeeRepository.findUnpaidShopReturnFees(store.getStoreId());

        // 5) Tính tổng nợ cần trả
        BigDecimal totalOrderDebt = unpaidFinalOrders.stream()
                .map(o -> nz(o.getTotalDebtOrder()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReturnFeeDebt = unpaidReturnFees.stream()
                .map(f -> nz(f.getChargedToShop()).compareTo(BigDecimal.ZERO) > 0
                        ? nz(f.getChargedToShop())
                        : nz(f.getShippingFee()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalToPay = totalOrderDebt.add(totalReturnFeeDebt);

        if (totalToPay.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.ok(new BaseResponse<>(200, "✅ Không có khoản nợ nào cần thanh toán", null));
        }

        // 6) Kiểm tra đủ tiền trong defaultBalance
        BigDecimal defaultBalance = nz(wallet.getDefaultBalance());

        BigDecimal balanceAfter = defaultBalance.subtract(totalToPay);
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("❌ Số dư defaultBalance không đủ để thanh toán nợ. " +
                    "Cần=" + totalToPay + ", hiện có=" + defaultBalance);
        }

        LocalDateTime now = LocalDateTime.now();

        // 7) Trừ tiền
        wallet.setDefaultBalance(balanceAfter);
        wallet.setUpdatedAt(now);
        storeWalletRepository.save(wallet);

        // 8) Lưu StoreWalletTransaction
        StoreWalletTransaction tx = StoreWalletTransaction.builder()
                .wallet(wallet)
                .type(StoreWalletTransactionType.DEBT_PAYMENT)
                .amount(totalToPay)
                .balanceAfter(balanceAfter)
                .description("Thanh toán nợ (final orders + return shipping fees)")
                .orderId(null)
                .createdAt(now)
                .build();
        storeWalletTransactionRepository.save(tx);

        // 8.1) Lưu PlatformTransaction để truy soát
        PlatformWallet platformWallet = platformWalletRepository.findMainPlatformWallet()
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy PlatformWallet chính"));

        PlatformTransaction flat = PlatformTransaction.builder()
                .wallet(platformWallet)
                .orderId(null)
                .storeId(store.getStoreId())
                .customerId(null)
                .amount(totalToPay)
                .type(TransactionType.DEBT_PAYMENT)
                .status(TransactionStatus.SUCCESS)
                .description("Store pay debt from defaultBalance | tx=" + tx.getTransactionId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        platformTransactionRepository.save(flat);

        // 9) Mark paid các order final
        for (StoreOrder o : unpaidFinalOrders) {
            o.setPaidByShop(true);
        }
        storeOrderRepository.saveAll(unpaidFinalOrders);

        // 10) Mark paid các return fee shop chịu
        for (ReturnShippingFee f : unpaidReturnFees) {
            f.setPaidByShop(true);
        }
        returnShippingFeeRepository.saveAll(unpaidReturnFees);

        // ==========================================================
        // 10.1) RECHECK "CÒN VƯỢT HẠN MỨC" KHÔNG? (để quyết định unblock)
        // ==========================================================
        // ✅ Flush đúng repository vừa update để DB/trigger/calc debtBalance kịp phản ánh
        storeOrderRepository.flush();
        returnShippingFeeRepository.flush();
        storeWalletRepository.flush();
        storeRepository.flush();

        StoreWallet walletFresh = storeWalletRepository.findByStore_StoreId(store.getStoreId())
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy ví sau khi cập nhật"));

        Store storeFresh = storeRepository.findById(store.getStoreId())
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy store sau khi cập nhật"));

        BigDecimal debtNow = nz(walletFresh.getDebtBalance());
        BigDecimal depositNow = nz(walletFresh.getDepositBalance());
        BigDecimal legalPointNow = nz(storeFresh.getLegalPoint());

        BigDecimal creditLimit = depositNow.add(legalPointNow.multiply(new BigDecimal("100000")));

        boolean canUnblock;
        if (creditLimit.compareTo(BigDecimal.ZERO) <= 0) {
            canUnblock = debtNow.compareTo(BigDecimal.ZERO) <= 0;
        } else {
            BigDecimal ratioNow = debtNow.divide(creditLimit, 4, RoundingMode.HALF_UP);
            canUnblock = ratioNow.compareTo(BigDecimal.ONE) < 0;
        }

        if (storeFresh.getStatus() == StoreStatus.SUSPENDED_DEBT) {

            if (!canUnblock) {
                BigDecimal needTopup = debtNow.subtract(creditLimit).max(BigDecimal.ZERO);

                log.warn("[DEBT-PAY][STILL-BLOCKED] storeId={} debtNow={} creditLimit={} needTopup={}",
                        storeFresh.getStoreId(), debtNow, creditLimit, needTopup);

                return ResponseEntity.ok(new BaseResponse<>(200,
                        "✅ Đã thanh toán nợ FINAL/return fee. Tuy nhiên cửa hàng vẫn vượt hạn mức => chưa thể mở khóa. " +
                                "Cần nạp thêm cọc tối thiểu: " + needTopup,
                        PayDebtResult.builder()
                                .storeId(storeFresh.getStoreId())
                                .paidAmount(totalToPay)
                                .balanceAfter(balanceAfter)
                                .paidOrdersCount(unpaidFinalOrders.size())
                                .paidReturnFeesCount(unpaidReturnFees.size())
                                .paidAt(now)
                                .transactionId(tx.getTransactionId())
                                .build()));
            }

            // ✅ CAN UNBLOCK
            storeFresh.setStatus(StoreStatus.ACTIVE);
            storeFresh.setLastRiskWarningAt(now);
            storeRepository.save(storeFresh);

            int backToActive = productRepository.bulkUpdateStatusByStoreAndStatus(
                    storeFresh.getStoreId(),
                    ProductStatus.SUSPENDED_DEBT,
                    ProductStatus.ACTIVE
            );

            int backToUnlisted = productRepository.bulkUpdateStatusByStoreAndStatus(
                    storeFresh.getStoreId(),
                    ProductStatus.UNLISTED_BEFORE_SUSPENDED_DEBT,
                    ProductStatus.UNLISTED
            );

            log.warn("[DEBT-PAY][UNBLOCK] storeId={} storeStatus=ACTIVE backToActive={} backToUnlisted={}",
                    storeFresh.getStoreId(), backToActive, backToUnlisted);
        }

        // 11) Response
        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Thanh toán nợ thành công",
                PayDebtResult.builder()
                        .storeId(storeFresh.getStoreId()) // ✅ dùng storeFresh cho thống nhất
                        .paidAmount(totalToPay)
                        .balanceAfter(balanceAfter)
                        .paidOrdersCount(unpaidFinalOrders.size())
                        .paidReturnFeesCount(unpaidReturnFees.size())
                        .paidAt(now)
                        .transactionId(tx.getTransactionId())
                        .build()
        ));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
    public ResponseEntity<BaseResponse> getMyDebtComponents(
            DebtComponentType componentType,
            String status,
            Boolean payableNowOnly,          // true = chỉ lấy nợ trả ngay (NOW), null/false = không lọc
            LocalDateTime from,
            LocalDateTime to,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String orderCode,
            String ghnOrderCode,
            int page,
            int size
    ) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy store cho tài khoản: " + email));

        UUID storeId = store.getStoreId();

        // ⚠️ NOTE: repo này đang load nhiều store, tốt nhất nên query theo storeId
        List<StoreOrder> orders = storeOrderRepository.findOrdersForDebtCron();

        // Return fee: repo đã lấy unpaid shop return fees theo storeId (tốt)
        List<ReturnShippingFee> returnFees = returnShippingFeeRepository.findUnpaidShopReturnFees(storeId);

        Stream<DebtComponentItemResponse> orderComponents = orders.stream()
                .filter(o -> o.getStore() != null && storeId.equals(o.getStore().getStoreId()))
                .flatMap(o -> {
                    BigDecimal R = nvl(o.getShippingFeeReal());
                    BigDecimal E = nvl(o.getShippingFee());

                    // Không có phí thực tế => không phát sinh component
                    if (R.compareTo(BigDecimal.ZERO) <= 0) return Stream.empty();

                    boolean paid = Boolean.TRUE.equals(o.getPaidByShop());
                    String st = paid ? "PAID" : "UNPAID";

                    // =========================================================
                    // 1) SHIP_DIFF
                    // - Luôn chỉ phát sinh khi deliveredAt != null
                    // - Nếu payableNowOnly=true => vẫn OK vì delivered là NOW
                    // =========================================================
                    if (o.getDeliveredAt() != null) {
                        BigDecimal shipDiff = R.subtract(E).max(BigDecimal.ZERO);
                        if (shipDiff.compareTo(BigDecimal.ZERO) <= 0) return Stream.empty();

                        return Stream.of(DebtComponentItemResponse.builder()
                                .componentType(DebtComponentType.SHIP_DIFF)
                                .displayType("Chênh lệch phí ship (GHN thực tế - dự kiến)")
                                .refId(o.getId())
                                .orderCode(o.getOrderCode())
                                .ghnOrderCode(null)
                                .amount(shipDiff)
                                .status(st)
                                .occurredAt(o.getDeliveredAt())
                                .description("SHIP_DIFF = max(shippingFeeReal - shippingFee, 0)")
                                .build());
                    }

                    // =========================================================
                    // 2) RTO_FEE
                    // - payableNowOnly=true  => chỉ lấy cái đã “chốt phát sinh”
                    //   (khuyến nghị: returnChargeApplied == true)
                    // - payableNowOnly null/false => KHÔNG lọc (hiện cả pending)
                    // =========================================================
                    if (Boolean.TRUE.equals(payableNowOnly)) {
                        // NOW: phải chốt phí quay đầu
                        if (!Boolean.TRUE.equals(o.getReturnChargeApplied())) {
                            return Stream.empty();
                        }
                    }

                    BigDecimal extra = BigDecimal.ZERO;
                    if (Boolean.TRUE.equals(o.getReturnChargeApplied())) {
                        BigDecimal rate = nvl(o.getReturnShippingChargeRate());
                        extra = R.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    }

                    BigDecimal rtoFee = R.add(extra);
                    if (rtoFee.compareTo(BigDecimal.ZERO) <= 0) return Stream.empty();

                    LocalDateTime occurred = o.getReturnChargeAppliedAt() != null
                            ? o.getReturnChargeAppliedAt()
                            : o.getCreatedAt();

                    return Stream.of(DebtComponentItemResponse.builder()
                            .componentType(DebtComponentType.RTO_FEE)
                            .displayType("Phí quay đầu / không nhận hàng")
                            .refId(o.getId())
                            .orderCode(o.getOrderCode())
                            .ghnOrderCode(null)
                            .amount(rtoFee)
                            .status(st)
                            .occurredAt(occurred)
                            .description("RTO_FEE = shippingFeeReal + (returnChargeApplied? shippingFeeReal*rate% : 0)")
                            .build());
                });

        Stream<DebtComponentItemResponse> returnFeeComponents = returnFees.stream()
                .filter(f -> f.getStoreId() != null && storeId.equals(f.getStoreId()))
                .filter(f -> "SHOP".equalsIgnoreCase(f.getPayer()))
                .filter(f -> !Boolean.TRUE.equals(f.getPaidByShop())) // chưa paid
                .map(f -> {
                    BigDecimal charged = nvl(f.getChargedToShop());
                    BigDecimal amt = charged.compareTo(BigDecimal.ZERO) > 0 ? charged : nvl(f.getShippingFee());

                    return DebtComponentItemResponse.builder()
                            .componentType(DebtComponentType.RETURN_SHIPPING_FEE)
                            .displayType("Phí hoàn/return (SHOP chịu)")
                            .refId(f.getReturnRequestId())
                            .orderCode(null)
                            .ghnOrderCode(f.getGhnOrderCode())
                            .amount(amt)
                            .status("UNPAID")
                            .occurredAt(f.getCreatedAt())
                            .description("RETURN_SHIPPING_FEE = chargedToShop > 0 ? chargedToShop : shippingFee")
                            .build();
                })
                .filter(it -> it.getAmount() != null && it.getAmount().compareTo(BigDecimal.ZERO) > 0);

        Stream<DebtComponentItemResponse> all = Stream.concat(orderComponents, returnFeeComponents);

        // ====== FILTERS (client truyền) ======
        if (componentType != null) all = all.filter(x -> x.getComponentType() == componentType);

        if (status != null && !status.isBlank()) {
            String s = status.trim().toUpperCase();
            all = all.filter(x -> s.equals(x.getStatus()));
        }

        // Nếu filter theo ngày => occurredAt bắt buộc phải có
        if (from != null) all = all.filter(x -> x.getOccurredAt() != null && !x.getOccurredAt().isBefore(from));
        if (to != null) all = all.filter(x -> x.getOccurredAt() != null && !x.getOccurredAt().isAfter(to));

        if (minAmount != null) all = all.filter(x -> x.getAmount() != null && x.getAmount().compareTo(minAmount) >= 0);
        if (maxAmount != null) all = all.filter(x -> x.getAmount() != null && x.getAmount().compareTo(maxAmount) <= 0);

        if (orderCode != null && !orderCode.isBlank()) {
            String key = orderCode.trim();
            all = all.filter(x -> x.getOrderCode() != null && x.getOrderCode().contains(key));
        }
        if (ghnOrderCode != null && !ghnOrderCode.isBlank()) {
            String key = ghnOrderCode.trim();
            all = all.filter(x -> x.getGhnOrderCode() != null && x.getGhnOrderCode().contains(key));
        }

        List<DebtComponentItemResponse> list = all
                .sorted(Comparator.comparing(DebtComponentItemResponse::getOccurredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int fromIdx = Math.min(page * size, list.size());
        int toIdx = Math.min(fromIdx + size, list.size());

        Page<DebtComponentItemResponse> pageResult =
                new PageImpl<>(list.subList(fromIdx, toIdx), PageRequest.of(page, size), list.size());

        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Lấy breakdown nợ theo thành phần thành công", pageResult));
    }

    private BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

}
