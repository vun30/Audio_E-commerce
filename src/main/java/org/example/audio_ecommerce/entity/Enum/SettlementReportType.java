package org.example.audio_ecommerce.entity.Enum;

public enum SettlementReportType {
    UNDELI_COD,
    UNDELI_ONLINE,
    DELI_COD,
    DELI_ONLINE,
    PLATFORM_FEE_TO_COLLECT, // eligible=true && payoutProcessed=false && deliveredAt = date
    TOTAL_COLLECTED // eligible=true && payoutProcessed=true && deliveredAt = date
}
