package org.example.audio_ecommerce.service;


import org.example.audio_ecommerce.dto.response.GhnFlatDebtSummaryResponse;

import java.util.UUID;

public interface DebtSummaryService {
    GhnFlatDebtSummaryResponse calcGhnFlatDebtSummary(UUID storeId);
}
