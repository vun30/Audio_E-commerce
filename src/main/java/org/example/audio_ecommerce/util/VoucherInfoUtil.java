package org.example.audio_ecommerce.util;

import org.example.audio_ecommerce.entity.*;
import org.example.audio_ecommerce.dto.request.PlatformVoucherUse;
import org.example.audio_ecommerce.dto.request.StoreVoucherUse;
import org.example.audio_ecommerce.repository.PlatformCampaignProductRepository;
import org.example.audio_ecommerce.repository.ShopVoucherRepository;

import java.math.BigDecimal;
import java.util.*;

public class VoucherInfoUtil {

    /**
     * Updates voucher information columns in StoreOrderItem based on applied vouchers
     *
     * @param item                   The StoreOrderItem to update
     * @param storeVouchers          List of store vouchers applied
     * @param platformVouchers       List of platform vouchers applied
     * @param voucherRepo            Repository for accessing shop vouchers
     * @param campaignProductRepo    Repository for accessing platform campaign products
     */
    public static void updateVoucherInfoForItem(
            StoreOrderItem item,
            List<StoreVoucherUse> storeVouchers,
            List<PlatformVoucherUse> platformVouchers,
            ShopVoucherRepository voucherRepo,
            PlatformCampaignProductRepository campaignProductRepo
    ) {
        UUID storeId = item.getStoreOrder().getStore().getStoreId();
        
        // Reset values first
        item.setStoreVoucherPercentage(BigDecimal.ZERO);
        item.setStoreItemVoucherPercentage(BigDecimal.ZERO);
        item.setPlatformCampaignPercentage(BigDecimal.ZERO);
        item.setStoreVoucherName(null);
        item.setStoreItemVoucherName(null);
        item.setPlatformCampaignName(null);

        // Process store vouchers
        if (storeVouchers != null) {
            for (StoreVoucherUse storeVoucherUse : storeVouchers) {
                if (storeVoucherUse.getStoreId().equals(storeId) && storeVoucherUse.getCodes() != null) {
                    for (String code : storeVoucherUse.getCodes()) {
                        Optional<ShopVoucher> voucherOpt = voucherRepo.findByShop_StoreIdAndCodeIgnoreCase(storeId, code);
                        if (voucherOpt.isPresent()) {
                            ShopVoucher voucher = voucherOpt.get();
                            
                            // Check if this is a store-wide voucher or item-specific voucher
                            if (voucher.getVoucherProducts() == null || voucher.getVoucherProducts().isEmpty()) {
                                // Store-wide voucher
                                if (voucher.getDiscountPercent() != null) {
                                    item.setStoreVoucherPercentage(new BigDecimal(voucher.getDiscountPercent()));
                                }
                                item.setStoreVoucherName(voucher.getCode());
                            } else {
                                // Item-specific voucher - check if this item is eligible
                                boolean isEligible = voucher.getVoucherProducts().stream()
                                        .filter(ShopVoucherProduct::isActive)
                                        .anyMatch(vp -> vp.getProduct() != null && 
                                                vp.getProduct().getProductId().equals(item.getRefId()));
                                
                                if (isEligible) {
                                    if (voucher.getDiscountPercent() != null) {
                                        item.setStoreItemVoucherPercentage(new BigDecimal(voucher.getDiscountPercent()));
                                    }
                                    item.setStoreItemVoucherName(voucher.getCode());
                                }
                            }
                        }
                    }
                }
            }
        }

        // Process platform vouchers
        if (platformVouchers != null) {
            for (PlatformVoucherUse platformVoucherUse : platformVouchers) {
                if (platformVoucherUse.getCampaignProductId() != null) {
                    Optional<PlatformCampaignProduct> campaignProductOpt = 
                            campaignProductRepo.findById(platformVoucherUse.getCampaignProductId());
                    
                    if (campaignProductOpt.isPresent()) {
                        PlatformCampaignProduct campaignProduct = campaignProductOpt.get();
                        
                        // Check if this campaign product applies to this item
                        if (campaignProduct.getProduct() != null && 
                                campaignProduct.getProduct().getProductId().equals(item.getRefId())) {
                            
                            if (campaignProduct.getDiscountPercent() != null) {
                                item.setPlatformCampaignPercentage(new BigDecimal(campaignProduct.getDiscountPercent()));
                            }
                            
                            // Set campaign name from the parent campaign
                            if (campaignProduct.getCampaign() != null) {
                                item.setPlatformCampaignName(campaignProduct.getCampaign().getCode());
                            }
                        }
                    }
                }
            }
        }
    }
}