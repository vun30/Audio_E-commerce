package org.example.audio_ecommerce.service.Impl;

import org.example.audio_ecommerce.dto.request.DepositTransferRequest;
import org.example.audio_ecommerce.dto.request.WithdrawDepositToDefaultRequest;
import org.example.audio_ecommerce.dto.request.WithdrawRequest;
import org.example.audio_ecommerce.util.SecurityUtils;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.scheduler.StoreDebtUnlockService;
import org.example.audio_ecommerce.scheduler.StoreWalletDebtCron;
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
    private final StoreWalletDebtCron storeWalletDebtCron;
    private final StoreDebtUnlockService storeDebtUnlockService;
    private final SecurityUtils securityUtils;


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
        if (walletId == null) walletId = getCurrentStoreWalletId();

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Thời gian 'from' phải nhỏ hơn hoặc bằng 'to'");
        }

        Page<StoreWalletTransaction> transactionsPage =
                storeWalletTransactionRepository.filterTransactions(
                        walletId, from, to, type, transactionId, pageable
                );

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
            case DEPOSIT -> "Tiền bán hàng (payout vào ví)";
            case PENDING_HOLD -> "Giữ tiền tạm thời (pending hold)";
            case RELEASE_PENDING -> "Giải phóng tiền giữ (pending → default)";
            case WITHDRAW -> "Rút tiền về ngân hàng";
            case REFUND -> "Hoàn tiền cho khách";
            case ADJUSTMENT -> "Điều chỉnh thủ công (admin)";
            case REFUND_RETURN -> "Hoàn tiền do trả hàng";
            case REFUND_FORCE -> "Hoàn tiền cưỡng chế";
            case TOPUP -> "Nạp tiền vào ví";
            case DEBT_PAYMENT -> "Thanh toán nợ";
            case TRANSFER_TO_DEPOSIT -> "Chuyển tiền giữa các ví";
            case TRANSFER_DEPOSIT_TO_DEFAULT -> "Chuyển tiền từu ví ký quỹ sang ví chính";
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


    @Transactional
    public ResponseEntity<BaseResponse> payMyDebtFromDefaultBalance() {

        // 1) Resolve store hiện tại
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy store cho tài khoản: " + email));

        UUID storeId = store.getStoreId();

        // 2) Load wallet
        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(storeId)
                .orElseThrow(() -> new RuntimeException("❌ Cửa hàng này chưa có ví."));

        // 3) Lấy các khoản nợ FINAL chưa trả (StoreOrder)
        List<StoreOrder> unpaidFinalOrders = storeOrderRepository.findUnpaidFinalOrdersOfStore(storeId);

        // 4) Lấy các khoản return fee SHOP chịu chưa trả
        List<ReturnShippingFee> unpaidReturnFees = returnShippingFeeRepository.findUnpaidShopReturnFees(storeId);

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
                .storeId(storeId)
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
        for (StoreOrder o : unpaidFinalOrders) o.setPaidByShop(true);
        storeOrderRepository.saveAll(unpaidFinalOrders);

        // 10) Mark paid các return fee shop chịu
        for (ReturnShippingFee f : unpaidReturnFees) f.setPaidByShop(true);
        returnShippingFeeRepository.saveAll(unpaidReturnFees);

        // ✅ Flush để đảm bảo query recalc thấy paidByShop=true ngay trong transaction
        storeOrderRepository.flush();
        returnShippingFeeRepository.flush();
        storeWalletRepository.flush();

        // ==========================================================
        // ✅ NEW: GỌI GIÁN TIẾP CRON -> RECALC debtBalance CHO STORE NÀY
        // ==========================================================
        storeWalletDebtCron.recalcStoreDebtBalanceByStoreId(storeId);

        // ==========================================================
        // ✅ NEW: GỌI UNLOCK SERVICE (rule chuẩn: debt<limit + deposit>=10%debt)
        // ==========================================================
        storeDebtUnlockService.tryUnlockStore(storeId);
        // hoặc nếu bạn vẫn muốn delay 3 phút:
        // storeDebtUnlockService.scheduleUnlockCheck(storeId);

        // ==========================================================
        // ❌ OLD LOGIC: RECHECK + UNBLOCK inline (KHÔNG DÙNG NỮA)
        // -> comment giữ lại theo yêu cầu, không xóa
        // ==========================================================
    /*
    storeRepository.flush();

    StoreWallet walletFresh = storeWalletRepository.findByStore_StoreId(storeId)
            .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy ví sau khi cập nhật"));

    Store storeFresh = storeRepository.findById(storeId)
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
            return ResponseEntity.ok(...);
        }

        storeFresh.setStatus(StoreStatus.ACTIVE);
        storeFresh.setLastRiskWarningAt(now);
        storeRepository.save(storeFresh);

        productRepository.bulkUpdateStatusByStoreAndStatus(...);
        productRepository.bulkUpdateStatusByStoreAndStatus(...);
    }
    */

        // 11) Response
        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Thanh toán nợ thành công",
                PayDebtResult.builder()
                        .storeId(storeId)
                        .paidAmount(totalToPay)
                        .balanceAfter(balanceAfter)
                        .paidOrdersCount(unpaidFinalOrders.size())
                        .paidReturnFeesCount(unpaidReturnFees.size())
                        .paidAt(now)
                        .transactionId(tx.getTransactionId())
                        // optional debug:
                        // .debtAfterRecalc(newDebt)
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

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }  // chống ỗi khi cộng trừ BigDecimal , tránh null pointer exception , trả về 0 nếu null , nó mà lỗi thì kiếm lại hàm này check nha vu

    @Transactional
    @Override
    public ResponseEntity<BaseResponse> withdrawFromDefaultBalance(WithdrawRequest req) {

        if (req == null || req.getAmount() == null) {
            throw new RuntimeException("❌ amount is required");
        }
        BigDecimal amount = req.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("❌ amount must be > 0");
        }

        // 1) Resolve store hiện tại từ token
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy store cho tài khoản: " + email));

        UUID storeId = store.getStoreId();

        // 2) Load wallet
        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(storeId)
                .orElseThrow(() -> new RuntimeException("❌ Cửa hàng này chưa có ví."));

        // 3) Check số dư defaultBalance
        BigDecimal defaultBalance = nz(wallet.getDefaultBalance());
        BigDecimal balanceAfter = defaultBalance.subtract(amount);

        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            // ❌ không đủ tiền -> không tạo transaction
            throw new RuntimeException("❌ Số dư defaultBalance không đủ để rút. " +
                    "Cần=" + amount + ", hiện có=" + defaultBalance);
        }

        LocalDateTime now = LocalDateTime.now();

        // 4) Trừ tiền
        wallet.setDefaultBalance(balanceAfter);
        wallet.setUpdatedAt(now);
        storeWalletRepository.save(wallet);

        // 5) Lưu StoreWalletTransaction
        String desc = "Rút tiền từ ví defaultBalance";
        if (req.getBankName() != null && !req.getBankName().isBlank()) desc += " | bank=" + req.getBankName();
        if (req.getBankAccountNo() != null && !req.getBankAccountNo().isBlank())
            desc += " | accNo=" + req.getBankAccountNo();
        if (req.getNote() != null && !req.getNote().isBlank()) desc += " | note=" + req.getNote();

        StoreWalletTransaction tx = StoreWalletTransaction.builder()
                .wallet(wallet)
                .type(StoreWalletTransactionType.WITHDRAW)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(desc)
                .orderId(null)
                .createdAt(now)
                .build();
        storeWalletTransactionRepository.save(tx);

        // 6) Lưu PlatformTransaction để truy soát
        PlatformWallet platformWallet = platformWalletRepository.findMainPlatformWallet()
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy PlatformWallet chính"));

        PlatformTransaction flat = PlatformTransaction.builder()
                .wallet(platformWallet)
                .orderId(null)
                .storeId(storeId)
                .customerId(null)
                .amount(amount)
                .type(TransactionType.WITHDRAW) // ✅ đảm bảo enum TransactionType có WITHDRAW
                .status(TransactionStatus.SUCCESS)
                .description("Store withdraw from defaultBalance | tx=" + tx.getTransactionId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        platformTransactionRepository.save(flat);

        // 7) Response
        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Rút tiền thành công",
                WithdrawResult.builder()
                        .storeId(storeId)
                        .withdrawAmount(amount)
                        .balanceAfter(balanceAfter)
                        .withdrawAt(now)
                        .transactionId(tx.getTransactionId())
                        .build()
        ));
    }


    @Transactional
    @Override
    public ResponseEntity<BaseResponse> transferDefaultToDeposit(DepositTransferRequest req) {

        if (req == null || req.getAmount() == null) {
            throw new RuntimeException("❌ amount is required");
        }
        BigDecimal amount = req.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("❌ amount must be > 0");
        }

        // 1) Resolve store từ token
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy store cho tài khoản: " + email));

        UUID storeId = store.getStoreId();

        // 2) Load wallet
        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(storeId)
                .orElseThrow(() -> new RuntimeException("❌ Cửa hàng này chưa có ví."));

        BigDecimal defaultBal = nz(wallet.getDefaultBalance());
        BigDecimal depositBal = nz(wallet.getDepositBalance());

        // 3) Check đủ tiền ở defaultBalance
        BigDecimal defaultAfter = defaultBal.subtract(amount);
        if (defaultAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("❌ defaultBalance không đủ để chuyển sang ví cọc. " +
                    "Cần=" + amount + ", hiện có=" + defaultBal);
        }

        BigDecimal depositAfter = depositBal.add(amount);

        LocalDateTime now = LocalDateTime.now();

        // 4) Update wallet (chuyển nội bộ)
        wallet.setDefaultBalance(defaultAfter);
        wallet.setDepositBalance(depositAfter);
        wallet.setUpdatedAt(now);
        storeWalletRepository.save(wallet);

        // 5) Lưu StoreWalletTransaction
        String desc = "Chuyển tiền từ defaultBalance sang depositBalance";
        if (req.getNote() != null && !req.getNote().isBlank()) desc += " | note=" + req.getNote();

        StoreWalletTransaction tx = StoreWalletTransaction.builder()
                .wallet(wallet)
                .type(StoreWalletTransactionType.TRANSFER_TO_DEPOSIT) // ✅ thêm enum này
                .amount(amount)
                .balanceAfter(defaultAfter) // nếu field này hiểu là "balanceAfter của ví default"
                .description(desc)
                .orderId(null)
                .createdAt(now)
                .build();
        storeWalletTransactionRepository.save(tx);

        // ✅ Nếu bạn muốn: có thể lưu thêm 1 record thứ 2 cho "deposit side"
        // nhưng thường 1 record là đủ để truy soát.

        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Chuyển sang ví cọc thành công",
                DepositTransferResult.builder()
                        .storeId(storeId)
                        .amount(amount)
                        .defaultBalanceAfter(defaultAfter)
                        .depositBalanceAfter(depositAfter)
                        .transactionId(tx.getTransactionId())
                        .transferredAt(now)
                        .build()
        ));
    }

    @Transactional
    @Override
    public ResponseEntity<BaseResponse> withdrawDepositToDefault(WithdrawDepositToDefaultRequest req) {

        BigDecimal amount = nz(req.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("❌ Số tiền rút phải > 0");
        }

        // 1) Resolve store từ token
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy store cho tài khoản: " + email));

        UUID storeId = store.getStoreId();

        // 2) Load wallet
        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(storeId)
                .orElseThrow(() -> new RuntimeException("❌ Cửa hàng này chưa có ví."));

        BigDecimal depositBefore = nz(wallet.getDepositBalance());
        BigDecimal defaultBefore = nz(wallet.getDefaultBalance());

        // 3) Check đủ tiền cọc
        BigDecimal depositAfter = depositBefore.subtract(amount);
        if (depositAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("❌ Số dư ví cọc không đủ. Hiện có=" + depositBefore + ", cần rút=" + amount);
        }

        // 4) Check rule: creditAfter >= debtNow
        // creditAfter = depositAfter + legalPoint*100000
        BigDecimal legalPoint = nz(store.getLegalPoint());
        BigDecimal debtNow = nz(wallet.getDebtBalance());

        BigDecimal creditAfter = depositAfter.add(legalPoint.multiply(new BigDecimal("100000")));

        if (creditAfter.compareTo(debtNow) < 0) {
            // ✅ báo lỗi đúng nghiệp vụ: rút sẽ làm credit < nợ => không cho rút
            throw new RuntimeException(
                    "❌ Không thể rút vì sau khi rút CREDIT < DEBT. " +
                            "debt=" + debtNow + ", creditAfter=" + creditAfter +
                            " (creditAfter = depositAfter + legalPoint*100000)"
            );
        }

        // 5) Update ví: deposit giảm, default tăng
        BigDecimal defaultAfter = defaultBefore.add(amount);

        LocalDateTime now = LocalDateTime.now();
        wallet.setDepositBalance(depositAfter);
        wallet.setDefaultBalance(defaultAfter);
        wallet.setUpdatedAt(now);
        storeWalletRepository.save(wallet);

        // 6) Lưu StoreWalletTransaction
        StoreWalletTransaction tx = StoreWalletTransaction.builder()
                .wallet(wallet)
                .type(StoreWalletTransactionType.TRANSFER_DEPOSIT_TO_DEFAULT)
                .amount(amount)
                .balanceAfter(defaultAfter) // ✅ bạn đang dùng balanceAfter như defaultBalanceAfter
                .description("Chuyển tiền từ ví cọc -> ví default")
                .orderId(null)
                .createdAt(now)
                .build();
        storeWalletTransactionRepository.save(tx);

        // 7) Response
        WithdrawDepositToDefaultResult result = WithdrawDepositToDefaultResult.builder()
                .storeId(storeId)
                .amount(amount)
                .depositBefore(depositBefore)
                .depositAfter(depositAfter)
                .defaultBefore(defaultBefore)
                .defaultAfter(defaultAfter)
                .debtNow(debtNow)
                .creditAfter(creditAfter)
                .transactionId(tx.getTransactionId())
                .createdAt(now)
                .build();

        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Rút tiền từ ví cọc về ví default thành công", result));
    }


    @Override
    @Transactional(readOnly = true)
    public StoreWalletOverviewResponse getMyWalletOverview() {
        UUID storeId = securityUtils.getCurrentStoreId();

        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(storeId)
                .orElseThrow(() -> new RuntimeException("❌ Store chưa có ví"));

        return StoreWalletOverviewResponse.builder()
                .storeId(storeId)
                .storeName(wallet.getStore() != null ? wallet.getStore().getStoreName(): null) // nếu có
                .walletId(wallet.getWalletId())
                .defaultBalance(nz(wallet.getDefaultBalance()))
                .depositBalance(nz(wallet.getDepositBalance()))
                .debtBalance(nz(wallet.getDebtBalance()))
                .build();
    }

    }

