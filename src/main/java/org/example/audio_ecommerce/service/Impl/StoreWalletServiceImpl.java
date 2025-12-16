package org.example.audio_ecommerce.service.Impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.StoreWalletTransactionType;
import org.example.audio_ecommerce.entity.Enum.TransactionStatus;
import org.example.audio_ecommerce.entity.Enum.TransactionType;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.StoreWalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
public class StoreWalletServiceImpl implements StoreWalletService {

    private final StoreRepository storeRepository;
    private final StoreWalletRepository storeWalletRepository;
    private final StoreWalletTransactionRepository storeWalletTransactionRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final ReturnShippingFeeRepository returnShippingFeeRepository;
    private final PlatformTransactionRepository platformTransactionRepository;
    private final PlatformWalletRepository platformWalletRepository;


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
            // ❌ Nếu âm: rollback toàn bộ
            throw new RuntimeException("❌ Số dư defaultBalance không đủ để thanh toán nợ. " +
                    "Cần=" + totalToPay + ", hiện có=" + defaultBalance);
        }

        LocalDateTime now = LocalDateTime.now();

        // 7) Trừ tiền
        wallet.setDefaultBalance(balanceAfter);
        wallet.setUpdatedAt(now);
        storeWalletRepository.save(wallet);

        // 8) Lưu StoreWalletTransaction (sao kê ví shop)
        StoreWalletTransaction tx = StoreWalletTransaction.builder()
                .wallet(wallet)
                .type(StoreWalletTransactionType.ADJUSTMENT) // ✅ khuyến nghị tạo enum DEBT_PAYMENT
                .amount(totalToPay)
                .balanceAfter(balanceAfter)
                .description("Thanh toán nợ (final orders + return shipping fees)")
                .orderId(null)
                .createdAt(now)
                .build();

        storeWalletTransactionRepository.save(tx);

        // ===========================
        // 8.1) Lưu Flat Transaction (PlatformTransaction) để truy soát
        // ===========================
        PlatformWallet platformWallet = platformWalletRepository.findMainPlatformWallet()
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy PlatformWallet chính"));

        PlatformTransaction flat = PlatformTransaction.builder()
                .wallet(platformWallet)
                .orderId(null)                 // trả nợ tổng, không gắn order
                .storeId(store.getStoreId())
                .customerId(null)
                .amount(totalToPay)
                .type(TransactionType.DEBT_PAYMENT)      // ✅ thêm enum này cho rõ
                .status(TransactionStatus.SUCCESS)       // ✅ success vì đã trừ ví shop OK
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

        // 11) Response
        return ResponseEntity.ok(new BaseResponse<>(200, "✅ Thanh toán nợ thành công", PayDebtResult.builder()
                .storeId(store.getStoreId())
                .paidAmount(totalToPay)
                .balanceAfter(balanceAfter)
                .paidOrdersCount(unpaidFinalOrders.size())
                .paidReturnFeesCount(unpaidReturnFees.size())
                .paidAt(now)
                .transactionId(tx.getTransactionId())
                .build()));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
