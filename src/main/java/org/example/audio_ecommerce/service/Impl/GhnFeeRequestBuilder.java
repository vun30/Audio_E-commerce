package org.example.audio_ecommerce.service.Impl;

import org.example.audio_ecommerce.dto.request.GhnFeeRequest;
import org.example.audio_ecommerce.entity.CartItem;
import org.example.audio_ecommerce.entity.Product;
import org.example.audio_ecommerce.entity.ProductCombo;
import org.example.audio_ecommerce.entity.ComboItem;

import java.util.*;

import static org.example.audio_ecommerce.util.DimensionsUtils.*;
import static org.example.audio_ecommerce.util.WeightUtils.kgToGram;

public final class GhnFeeRequestBuilder {

    private GhnFeeRequestBuilder() {}

    public static GhnFeeRequest buildForStoreShipment(
            List<CartItem> itemsOfStore,
            Integer toDistrictId, String toWardCode,
            Integer serviceTypeId // 2=nhẹ, 5=nặng
    ) {
        List<int[]> dims = new ArrayList<>();
        int totalWeightGram = 0;
        List<GhnFeeRequest.FeeItem> feeItems = new ArrayList<>();

        for (CartItem it : itemsOfStore) {
            if (it.getProduct() != null) {
                Product p = it.getProduct();
                int[] lwh = toLWHcm(p.getDimensions());
                int weightG = kgToGram(p.getWeight());
                int qty = it.getQuantity();

                dims.add(lwh);
                totalWeightGram += weightG * qty;
                feeItems.add(feeItemOf(p.getName(), qty, lwh, weightG));

            } else if (it.getCombo() != null) {
                ProductCombo combo = it.getCombo();
                if (combo.getItems() != null) {
                    for (ComboItem ci : combo.getItems()) {
                        Product p = ci.getProduct();
                        if (p == null) continue;
                        int[] lwh = toLWHcm(p.getDimensions());
                        int weightG = kgToGram(p.getWeight());

                        // Số lượng thực = quantity của combo * quantity của product trong combo
                        int qty = Math.max(1, it.getQuantity()) * Math.max(1, ci.getQuantity());

                        dims.add(lwh);
                        totalWeightGram += weightG * qty;
                        feeItems.add(feeItemOf(p.getName(), qty, lwh, weightG));
                    }
                }
            }
        }

        int[] oneParcel = aggregateStack(dims);

        GhnFeeRequest req = new GhnFeeRequest();
        req.setService_type_id(serviceTypeId == null ? 5 : serviceTypeId); // default hàng nặng

        // === Origin chọn an toàn theo item đầu tiên ===
        CartItem first = itemsOfStore.get(0);
        Product originProduct = null;

        if (first.getProduct() != null) {
            originProduct = first.getProduct();
        } else if (first.getCombo() != null && first.getCombo().getItems() != null && !first.getCombo().getItems().isEmpty()) {
            ComboItem c0 = first.getCombo().getItems().get(0);
            originProduct = c0.getProduct();
        }

        // Nếu bạn muốn ưu tiên origin của combo (khi có), có thể dùng block dưới:
        // ProductCombo firstCombo = first.getCombo();
        // if (firstCombo != null && firstCombo.getDistrictCode() != null && firstCombo.getWardCode() != null) {
        //     req.setFrom_district_id(parseIntSafe(firstCombo.getDistrictCode()));
        //     req.setFrom_ward_code(firstCombo.getWardCode());
        // } else {
        //     // fallback product
        //     req.setFrom_district_id(parseIntSafe(originProduct != null ? originProduct.getDistrictCode() : null));
        //     req.setFrom_ward_code(originProduct != null ? originProduct.getWardCode() : null);
        // }

        req.setFrom_district_id(parseIntSafe(originProduct != null ? originProduct.getDistrictCode() : null));
        req.setFrom_ward_code(originProduct != null ? originProduct.getWardCode() : null);

        req.setTo_district_id(toDistrictId);
        req.setTo_ward_code(toWardCode);

        req.setLength(Math.max(oneParcel[0], 1));
        req.setWidth(Math.max(oneParcel[1], 1));
        req.setHeight(Math.max(oneParcel[2], 1));
        req.setWeight(Math.max(totalWeightGram, 1)); // GHN không nhận 0g

        req.setInsurance_value(0);
        req.setCoupon(null);
        req.setItems(feeItems);

        try {
            String jsonReq = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(req);
            org.slf4j.LoggerFactory.getLogger(GhnFeeRequestBuilder.class)
                    .info("[GHN-FEE][BUILDER] Payload:\n{}", jsonReq);
        } catch (Exception ignore) {}

        return req;
    }

    private static GhnFeeRequest.FeeItem feeItemOf(String name, int qty, int[] lwh, int weightG) {
        GhnFeeRequest.FeeItem fi = new GhnFeeRequest.FeeItem();
        fi.setName(name);
        fi.setQuantity(qty);
        fi.setLength(Math.max(lwh[0], 1));
        fi.setWidth(Math.max(lwh[1], 1));
        fi.setHeight(Math.max(lwh[2], 1));
        fi.setWeight(Math.max(weightG, 1));
        return fi;
    }

    private static Integer parseIntSafe(String s) {
        try { return s == null ? null : Integer.valueOf(s); } catch (Exception e) { return null; }
    }
}
