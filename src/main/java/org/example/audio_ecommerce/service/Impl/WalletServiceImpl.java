package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.dto.request.WalletTxnRequest;
import org.example.audio_ecommerce.dto.response.WalletResponse;
import org.example.audio_ecommerce.dto.response.WalletTransactionResponse;
import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.entity.Enum.*;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txnRepo;
    private final StoreWalletRepository storeWalletRepo;
    private final StoreWalletTransactionRepository storeWalletTxnRepo;
    private final CustomerOrderItemRepository customerOrderItemRepo;
    private final PlatformWalletRepository platformWalletRepo;
    private final PlatformTransactionRepository platformTxnRepo;


    @Override
    @Transactional(readOnly = true)
    public WalletResponse getByCustomer(UUID customerId) {
        Wallet w = walletRepo.findByCustomer_Id(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
        return toWalletResponse(w);
    }

    @Override
    public WalletTransactionResponse deposit(UUID customerId, WalletTxnRequest req) {
        return doChangeBalance(customerId, req, WalletTransactionType.DEPOSIT);
    }

    @Override
    public WalletTransactionResponse withdraw(UUID customerId, WalletTxnRequest req) {
        return doChangeBalance(customerId, req, WalletTransactionType.WITHDRAW);
    }

    @Override
    public WalletTransactionResponse payment(UUID customerId, WalletTxnRequest req) {
        if (req.getOrderId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required for PAYMENT");
        // Idempotency: nếu đã có PAYMENT cho order này rồi -> trả về luôn
        var existed = txnRepo.findFirstByWallet_Customer_IdAndOrderIdAndTransactionTypeOrderByCreatedAtDesc(
                customerId, req.getOrderId(), WalletTransactionType.PAYMENT);
        if (existed.isPresent()) return toTxnResponse(existed.get());
        return doChangeBalance(customerId, req, WalletTransactionType.PAYMENT);
    }

    @Override
    public WalletTransactionResponse refund(UUID customerId, WalletTxnRequest req) {
        if (req.getOrderId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required for REFUND");
        return doChangeBalance(customerId, req, WalletTransactionType.REFUND);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> listTransactions(UUID customerId, Pageable pageable) {
        return txnRepo.findByWallet_Customer_IdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::toTxnResponse);
    }

    /**
     * Luồng refund bình thường cho return:
     *  - Trừ tiền khỏi pendingBalance + totalRevenue của StoreWallet
     *  - Cộng tiền vào balance của Wallet (customer)
     *  - Ghi 1 dòng WalletTransaction cho customer (store có StoreWalletTransaction riêng nếu muốn)
     */
    @Override
    @Transactional
    public void refundForReturn(ReturnRequest r) {
        BigDecimal amount = r.getItemPrice();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid refund amount");
        }

        // ===== Lấy orderId từ ReturnRequest → CustomerOrderItem → CustomerOrder =====
        CustomerOrderItem orderItem = customerOrderItemRepo.findById(r.getOrderItemId())
                .orElseThrow(() -> new NoSuchElementException("Order item not found for return request"));
        UUID orderId = orderItem.getCustomerOrder().getId();

        // ===== Lấy ví customer =====
        Wallet customerWallet = walletRepo.findByCustomerId(r.getCustomerId())
                .orElseThrow(() -> new NoSuchElementException("Customer wallet not found"));

        // ===== Lấy ví PLATFORM (trung gian) =====
        // Giả định ví nền tảng là 1 record có ownerType = PLATFORM và ownerId = null
        PlatformWallet platformWallet = platformWalletRepo
                .findByOwnerTypeAndOwnerId(WalletOwnerType.PLATFORM, null)
                .orElseThrow(() -> new NoSuchElementException("Platform wallet not found"));

        // ===== PLATFORM: trừ pendingBalance + totalBalance, tăng refundedTotal =====
        BigDecimal pfPendingBefore = platformWallet.getPendingBalance();
        BigDecimal pfPendingAfter = pfPendingBefore.subtract(amount);
        ensureNonNegative(pfPendingAfter, "Platform pending balance cannot be negative");

        BigDecimal pfTotalBefore = platformWallet.getTotalBalance();
        BigDecimal pfTotalAfter = pfTotalBefore.subtract(amount);
        ensureNonNegative(pfTotalAfter, "Platform totalBalance cannot be negative");

        platformWallet.setPendingBalance(pfPendingAfter);
        platformWallet.setTotalBalance(pfTotalAfter);
        platformWallet.setRefundedTotal(
                platformWallet.getRefundedTotal() == null
                        ? amount
                        : platformWallet.getRefundedTotal().add(amount)
        );
        platformWallet.setUpdatedAt(LocalDateTime.now());
        platformWalletRepo.save(platformWallet);

        // 🔹 Log PlatformTransaction: nền tảng trả tiền lại cho customer
        PlatformTransaction pfTxn = PlatformTransaction.builder()
                .wallet(platformWallet)
                .orderId(orderId)
                .storeId(r.getShopId())              // nếu muốn link shop liên quan
                .customerId(r.getCustomerId())
                .amount(amount)
                .type(TransactionType.REFUND_CUSTOMER_RETURN)  // hoặc TransactionType.REFUND nếu enum bạn đang dùng vậy
                .status(TransactionStatus.DONE)
                .description("Refund trả hàng từ platform pending cho customer, returnId=" + r.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        platformTxnRepo.save(pfTxn);

        // ===== CUSTOMER: cộng balance + log WalletTransaction =====
        BigDecimal cusBefore = customerWallet.getBalance();
        BigDecimal cusAfter = cusBefore.add(amount);

        customerWallet.setBalance(cusAfter);
        customerWallet.setLastTransactionAt(LocalDateTime.now());
        walletRepo.save(customerWallet);

        WalletTransaction cusTxn = WalletTransaction.builder()
                .wallet(customerWallet)
                .amount(amount)
                .transactionType(WalletTransactionType.RETURN_REFUND_CUSTOMER_CREDIT)
                .status(WalletTransactionStatus.SUCCESS)
                .description("Hoàn tiền trả hàng, sản phẩm: " + r.getProductName())
                .balanceBefore(cusBefore)
                .balanceAfter(cusAfter)
                .orderId(orderId)
                .externalRef("RETURN:" + r.getId())
                .build();
        txnRepo.save(cusTxn);

        log.info("[RETURN REFUND] returnRequest={}, orderId={}, amount={} hoàn vào ví customer từ platform",
                r.getId(), orderId, amount);
    }



    /**
     * Luồng ép hoàn (customer complaint, không cần hoàn hàng):
     *  - Trừ tiền khỏi availableBalance + totalRevenue của StoreWallet
     *  - Cộng tiền vào balance của Wallet (customer)
     *  - KHÔNG đụng tới phí ship
     */
    @Override
    @Transactional
    public void forceRefundWithoutReturn(ReturnRequest r) {
        BigDecimal amount = r.getItemPrice();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid refund amount");
        }

        // ===== Lấy orderId từ ReturnRequest → CustomerOrderItem → CustomerOrder =====
        CustomerOrderItem orderItem = customerOrderItemRepo.findById(r.getOrderItemId())
                .orElseThrow(() -> new NoSuchElementException("Order item not found for return request"));
        UUID orderId = orderItem.getCustomerOrder().getId();

        // ===== Lấy ví customer =====
        Wallet customerWallet = walletRepo.findByCustomerId(r.getCustomerId())
                .orElseThrow(() -> new NoSuchElementException("Customer wallet not found"));

        // ===== Lấy ví shop =====
        StoreWallet shopWallet = storeWalletRepo.findByStore_StoreId(r.getShopId())
                .orElseThrow(() -> new NoSuchElementException("Store wallet not found"));

        // ===== SHOP: trừ availableBalance + totalRevenue =====
        BigDecimal shopAvailableBefore = shopWallet.getAvailableBalance();
        BigDecimal shopAvailableAfter = shopAvailableBefore.subtract(amount);
        ensureNonNegative(shopAvailableAfter, "Shop availableBalance cannot be negative");

        BigDecimal shopTotalBefore = shopWallet.getTotalRevenue();
        BigDecimal shopTotalAfter = shopTotalBefore.subtract(amount);
        ensureNonNegative(shopTotalAfter, "Shop totalRevenue cannot be negative");

        shopWallet.setAvailableBalance(shopAvailableAfter);
        shopWallet.setTotalRevenue(shopTotalAfter);
        storeWalletRepo.save(shopWallet);

        // 🔹 Log StoreWalletTransaction: ép hoàn → trừ availableBalance
        StoreWalletTransaction forceTxn = StoreWalletTransaction.builder()
                .wallet(shopWallet)
                .type(StoreWalletTransactionType.REFUND) // hoặc REFUND_FORCE nếu bạn muốn tách
                .amount(amount)
                .balanceAfter(shopAvailableAfter)        // coi như "availableBalance sau giao dịch"
                .description("Ép hoàn do complaint (trừ availableBalance), returnId=" + r.getId())
                .orderId(orderId)                        // ✅ gắn đúng orderId
                .createdAt(LocalDateTime.now())
                .build();
        storeWalletTxnRepo.save(forceTxn);

        // ===== CUSTOMER: cộng balance + log WalletTransaction =====
        BigDecimal cusBefore = customerWallet.getBalance();
        BigDecimal cusAfter = cusBefore.add(amount);

        customerWallet.setBalance(cusAfter);
        customerWallet.setLastTransactionAt(LocalDateTime.now());
        walletRepo.save(customerWallet);

        WalletTransaction cusTxn = WalletTransaction.builder()
                .wallet(customerWallet)
                .amount(amount)
                .transactionType(WalletTransactionType.FORCE_RETURN_REFUND_CUSTOMER)
                .status(WalletTransactionStatus.SUCCESS)
                .description("Ép hoàn tiền do complaint, sản phẩm: " + r.getProductName())
                .balanceBefore(cusBefore)
                .balanceAfter(cusAfter)
                .orderId(orderId)                        // ✅ gắn đúng orderId
                .externalRef("FORCE_RETURN:" + r.getId())
                .build();
        txnRepo.save(cusTxn);

        log.info("[FORCE RETURN REFUND] returnRequest={}, orderId={}, amount={} ép hoàn vào ví customer",
                r.getId(), orderId, amount);
    }


    private void ensureNonNegative(BigDecimal value, String message) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(message);
        }
    }

    // ===== Core =====
    private WalletTransactionResponse doChangeBalance(UUID customerId, WalletTxnRequest req,
                                                      WalletTransactionType type) {
        Wallet wallet = walletRepo.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        if (wallet.getStatus() == WalletStatus.LOCKED)
            throw new ResponseStatusException(HttpStatus.LOCKED, "Wallet is locked");

        BigDecimal amount = req.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be > 0");
        }

        BigDecimal before = wallet.getBalance();
        BigDecimal after;

        switch (type) {
            case DEPOSIT, REFUND -> after = before.add(amount);
            case WITHDRAW, PAYMENT -> {
                if (before.compareTo(amount) < 0)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
                after = before.subtract(amount);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported type");
        }

        // cập nhật ví
        wallet.setBalance(after);
        wallet.setLastTransactionAt(LocalDateTime.now());

        // ghi giao dịch
        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(type)
                .status(WalletTransactionStatus.SUCCESS) // xử lý đồng bộ
                .description(req.getDescription())
                .balanceBefore(before)
                .balanceAfter(after)
                .orderId(req.getOrderId())
                .build();

        txnRepo.save(txn);
        return toTxnResponse(txn);
    }

    // ===== Mappers =====
    private WalletResponse toWalletResponse(Wallet w) {
        return WalletResponse.builder()
                .id(w.getId())
                .customerId(w.getCustomer().getId())
                .balance(w.getBalance())
                .currency(w.getCurrency())
                .status(w.getStatus().name())
                .lastTransactionAt(w.getLastTransactionAt())
                .build();
    }

    private WalletTransactionResponse toTxnResponse(WalletTransaction t) {
        return WalletTransactionResponse.builder()
                .id(t.getId())
                .walletId(t.getWallet().getId())
                .orderId(t.getOrderId())
                .type(t.getTransactionType().name())
                .status(t.getStatus().name())
                .amount(t.getAmount())
                .balanceBefore(t.getBalanceBefore())
                .balanceAfter(t.getBalanceAfter())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
