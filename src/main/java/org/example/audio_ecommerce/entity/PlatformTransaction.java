package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "platform_transaction")
public class PlatformTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Ví platform trung gian chứa giao dịch
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private PlatformWallet wallet;

    // Giao dịch liên quan tới đơn hàng nào
    @Column(nullable = true)
    private UUID orderId;

    // ID cửa hàng nhận tiền (nếu là giao dịch với shop)
    @Column
    private UUID storeId;

    // ID khách hàng nhận tiền (nếu là giao dịch refund)
    @Column
    private UUID customerId;

    // Số tiền giao dịch
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    // Loại giao dịch
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    // Trạng thái giao dịch
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    // Ghi chú chi tiết
    @Column(length = 255)
    private String description;

    // Thời điểm tạo và cập nhật
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();


//Định danh & chống bắn callback nhiều lần (idempotency)
    // Khóa chống ghi trùng (PayOS callback, job unlock, payout retry...)
    @Column(length = 80, unique = true)
    private String idempotencyKey;   // vd: "PAYOS:txn_123", "UNLOCK:orderId", "PAYOUT:req_456"

    //Nguồn tiền / kênh thanh toán
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentChannel channel; // PAYOS, COD, WALLET, BANK_TRANSFER, GHN_SETTLEMENT, INTERNAL

    @Column(length = 120)
    private String externalRefId;    // payosTransactionId / bankTxnId / ghnStatementId...

    @Column(length = 120)
    private String externalRefCode;  // orderCode, payosPaymentLinkId, ...


    //. “Bucket” bị ảnh hưởng (để biết tiền vào/ra cái nào)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WalletBucket bucket; // CASH, PENDING, PAYABLE_TO_STORE, STORE_DEBT, COMMISSION, LOGISTICS_PAYABLE, SHIP_CUS_COLLECTED, SHIP_STORE_CHARGED


    //Chiều (+/-) và số dư trước/sau để audit
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TxDirection direction; // IN, OUT

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceBefore;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceAfter;


    //Chi tiết “phân bổ” theo đơn (rất cần cho payout/commission/ship diff)
    @Column(precision = 18, scale = 2)
    private BigDecimal itemAmount;          // tiền hàng (100k)

    @Column(precision = 18, scale = 2)
    private BigDecimal shipCustomerPaid;    // ship khách trả (30k)

    @Column(precision = 18, scale = 2)
    private BigDecimal shipReal;            // ship thực tế GHN (40k)

    @Column(precision = 18, scale = 2)
    private BigDecimal shipDiffChargeStore; // phần shop bù (10k)

    @Column(precision = 18, scale = 2)
    private BigDecimal commissionAmount;    // phí nền tảng (10k)

    @Column(precision = 5, scale = 2)
    private BigDecimal commissionRate;      // 10.00 (%)


    //Payout: số tiền shop yêu cầu rút & số tiền thực nhận (cấn nợ)
    @Column
    private UUID payoutRequestId;           // nếu bạn có bảng payout_request

    @Column(precision = 18, scale = 2)
    private BigDecimal payoutGross;         // shop "được nhận" theo đơn (vd 90k)

    @Column(precision = 18, scale = 2)
    private BigDecimal debtDeducted;        // cấn trừ nợ (vd 10k ship diff)

    @Column(precision = 18, scale = 2)
    private BigDecimal payoutNet;           // thực chuyển shop (vd 80k)

    //Metadata để tra cứu nhanh (JSON)
    @Column(columnDefinition = "TEXT")
    private String metadataJson; // lưu json nhỏ: ghn fee breakdown, rto fee, lý do refund, ...

}
