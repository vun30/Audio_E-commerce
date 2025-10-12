package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.Enum.StoreWalletTransactionType;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.entity.StoreWallet;
import org.example.audio_ecommerce.entity.StoreWalletTransaction;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.repository.StoreWalletRepository;
import org.example.audio_ecommerce.repository.StoreWalletTransactionRepository;
import org.example.audio_ecommerce.service.StoreWalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreWalletServiceImpl implements StoreWalletService {

    private final StoreRepository storeRepository;
    private final StoreWalletRepository storeWalletRepository;
    private final StoreWalletTransactionRepository storeWalletTransactionRepository;

    /**
     * ✅ Lấy thông tin ví của cửa hàng đang đăng nhập
     *    (Bao gồm: tiền khả dụng, tiền pending, tổng doanh thu, và tiền ký quỹ)
     */
    @Override
    public ResponseEntity<BaseResponse> getMyWallet() {
        // 📩 Lấy email từ JWT
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        // 🔍 Tìm store theo email
        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy store cho tài khoản: " + email));

        // 🔍 Tìm ví theo storeId
        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(store.getStoreId())
                .orElseThrow(() -> new RuntimeException("❌ Cửa hàng này chưa có ví."));

        // ✅ Chuẩn bị DTO response (có thêm depositBalance)
        StoreWalletSummaryResponse response = StoreWalletSummaryResponse.builder()
                .storeId(store.getStoreId())
                .storeName(store.getStoreName())
                .walletId(wallet.getWalletId())
                .availableBalance(wallet.getAvailableBalance())  // tiền có thể rút
                .pendingBalance(wallet.getPendingBalance())      // tiền đang hold
                .depositBalance(wallet.getDepositBalance())      // 💰 tiền ký quỹ (mới thêm)
                .totalRevenue(wallet.getTotalRevenue())          // tổng doanh thu
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
        // 📩 Lấy email từ JWT
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = principal.contains(":") ? principal.split(":")[0] : principal;

        // 🔍 Tìm store theo email
        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() -> new RuntimeException("❌ Không tìm thấy store cho tài khoản: " + email));

        // 🔍 Tìm ví theo storeId
        StoreWallet wallet = storeWalletRepository.findByStore_StoreId(store.getStoreId())
                .orElseThrow(() -> new RuntimeException("❌ Cửa hàng này chưa có ví."));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<StoreWalletTransaction> transactions;

        // ⚙️ Lọc theo loại giao dịch (nếu có)
        if (type != null && !type.isBlank()) {
            StoreWalletTransactionType enumType;
            try {
                enumType = StoreWalletTransactionType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("❌ Loại giao dịch không hợp lệ. Hợp lệ: DEPOSIT, WITHDRAW, REFUND, PENDING_HOLD, RELEASE_PENDING, ADJUSTMENT");
            }

            transactions = storeWalletTransactionRepository
                    .findByWallet_WalletIdAndTypeOrderByCreatedAtDesc(wallet.getWalletId(), enumType, pageable);
        } else {
            transactions = storeWalletTransactionRepository
                    .findByWallet_WalletIdOrderByCreatedAtDesc(wallet.getWalletId(), pageable);
        }

        // 🔄 Map entity → DTO
        List<StoreWalletTransactionResponse> items = transactions.getContent().stream()
                .map(tx -> StoreWalletTransactionResponse.builder()
                        .transactionId(tx.getTransactionId())
                        .type(tx.getType().name())
                        .amount(tx.getAmount())
                        .balanceAfter(tx.getBalanceAfter())
                        .description(tx.getDescription())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        // 📦 Đóng gói kết quả phân trang
        PagedResult<StoreWalletTransactionResponse> result = PagedResult.<StoreWalletTransactionResponse>builder()
                .items(items)
                .page(page)
                .size(size)
                .totalElements(transactions.getTotalElements())
                .totalPages(transactions.getTotalPages())
                .build();

        return ResponseEntity.ok(
                new BaseResponse<>(200, "📜 Lấy danh sách giao dịch ví thành công", result)
        );
    }
}
