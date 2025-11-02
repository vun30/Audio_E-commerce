package org.example.audio_ecommerce.service.Impl;

import org.example.audio_ecommerce.dto.request.GhnFeeRequest;
import org.example.audio_ecommerce.entity.CartItem;
import org.example.audio_ecommerce.entity.Product;
import org.example.audio_ecommerce.entity.ProductCombo;

import java.util.*;

import static org.example.audio_ecommerce.util.DimensionsUtils.*;
import static org.example.audio_ecommerce.util.WeightUtils.kgToGram;

public final class GhnFeeRequestBuilder {

    private GhnFeeRequestBuilder() {}

    public static GhnFeeRequest buildForStoreShipment(
            List<CartItem> itemsOfStore,
            // origin lấy từ từng product; chọn theo rule “đa số/quy ước item đầu tiên”
            // hoặc tách nhiều kiện — ở đây lấy theo rule “theo từng item/combination”
            Integer toDistrictId, String toWardCode,
            Integer serviceTypeId // 2=nhẹ, 5=nặng
    ) {
        // Nếu muốn một kiện duy nhất: gom kích thước/khối lượng
        List<int[]> dims = new ArrayList<>();
        int totalWeightGram = 0;

        List<GhnFeeRequest.FeeItem> feeItems = new ArrayList<>();

        for (CartItem it : itemsOfStore) {
            if (it.getProduct() != null) {
                Product p = it.getProduct();
                int[] lwh = toLWHcm(p.getDimensions());
                int weightG = kgToGram(p.getWeight());
                dims.add(lwh);
                totalWeightGram += weightG * it.getQuantity();

                feeItems.add(feeItemOf(p.getName(), it.getQuantity(), lwh, weightG));

            } else if (it.getCombo() != null) {
                ProductCombo combo = it.getCombo();
                // expand products trong combo
                if (combo.getIncludedProducts() != null) {
                    for (Product p : combo.getIncludedProducts()) {
                        int[] lwh = toLWHcm(p.getDimensions());
                        int weightG = kgToGram(p.getWeight());
                        // số lượng của product trong combo = quantity của combo (giả định 1:1)
                        int qty = it.getQuantity();
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
        // from_* bắt buộc: GHN yêu cầu 1 điểm xuất phát. Ở đây lấy theo item đầu tiên.
        // Nếu muốn chính xác hơn: bạn có thể tách *mỗi store* thành nhiều GHN shipments theo origin khác nhau.
        CartItem first = itemsOfStore.get(0);
        Product originProduct = first.getProduct() != null ? first.getProduct()
                : first.getCombo().getIncludedProducts().get(0);

        req.setFrom_district_id(parseIntSafe(originProduct.getDistrictCode()));
        req.setFrom_ward_code(originProduct.getWardCode());

        req.setTo_district_id(toDistrictId);
        req.setTo_ward_code(toWardCode);

        req.setLength(oneParcel[0]);
        req.setWidth(oneParcel[1]);
        req.setHeight(oneParcel[2]);
        req.setWeight(Math.max(totalWeightGram, 1)); // GHN không nhận 0g

        req.setInsurance_value(0);
        req.setCoupon(null);
        req.setItems(feeItems);
        return req;
    }

    private static GhnFeeRequest.FeeItem feeItemOf(String name, int qty, int[] lwh, int weightG) {
        GhnFeeRequest.FeeItem fi = new GhnFeeRequest.FeeItem();
        fi.setName(name);
        fi.setQuantity(qty);
        fi.setLength(Math.max(lwh[0],1));
        fi.setWidth(Math.max(lwh[1],1));
        fi.setHeight(Math.max(lwh[2],1));
        fi.setWeight(Math.max(weightG,1));
        return fi;
    }

    private static Integer parseIntSafe(String s) {
        try { return s == null ? null : Integer.valueOf(s); } catch (Exception e) { return null; }
    }
}
