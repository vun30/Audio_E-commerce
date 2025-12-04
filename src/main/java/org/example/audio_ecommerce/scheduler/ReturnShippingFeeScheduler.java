package org.example.audio_ecommerce.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.audio_ecommerce.entity.GhnOrder;
import org.example.audio_ecommerce.entity.ReturnShippingFee;
import org.example.audio_ecommerce.entity.Enum.GhnStatus;
import org.example.audio_ecommerce.repository.GhnOrderRepository;
import org.example.audio_ecommerce.repository.ReturnShippingFeeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnShippingFeeScheduler {
    private final GhnOrderRepository ghnOrderRepo;
    private final ReturnShippingFeeRepository returnShippingFeeRepo;

    @Scheduled(cron = "0 */1 * * * *")
    @Transactional
    public void syncPickedStatus() {
        // Nếu muốn bó hẹp lại, có thể dùng updatedAtAfter(…)
        // LocalDateTime after = LocalDateTime.now().minusDays(1);
        // List<GhnOrder> pickedOrders = ghnOrderRepo.findByStatusAndUpdatedAtAfter(GhnStatus.PICKED, after);

        List<GhnOrder> pickedOrders = ghnOrderRepo.findByStatus(GhnStatus.PICKED);

        if (pickedOrders.isEmpty()) {
            return;
        }

        for (GhnOrder o : pickedOrders) {
            String orderCode = o.getOrderGhn();

            // Tìm các log return_shipping_fees tương ứng chưa picked
            List<ReturnShippingFee> feeLogs =
                    returnShippingFeeRepo.findByGhnOrderCodeAndPickedFalse(orderCode);

            if (feeLogs.isEmpty()) {
                continue;
            }

            for (ReturnShippingFee fee : feeLogs) {
                fee.setPicked(true); // ✅ đánh dấu là đã PICKED
                // nếu muốn log thời gian có thể thêm field pickedAt trong entity
                returnShippingFeeRepo.save(fee);
            }

            log.info("[RETURN FEE] Marked picked=true for {} fee logs of GHN order {}",
                    feeLogs.size(), orderCode);
        }
    }
}
