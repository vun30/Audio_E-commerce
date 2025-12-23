package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.entity.Enum.VoucherStatus;
import org.example.audio_ecommerce.entity.PlatformCampaign;
import org.example.audio_ecommerce.repository.*;
import org.example.audio_ecommerce.service.Impl.PlatformCampaignServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlatformCampaignServiceImplUpdateCampaignStatusTest {

    private PlatformCampaignServiceImpl newSut(PlatformCampaignRepository campaignRepository) {
        PlatformCampaignFlashSlotRepository flashSlotRepository = mock(PlatformCampaignFlashSlotRepository.class);
        PlatformCampaignProductRepository campaignProductRepository = mock(PlatformCampaignProductRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreRepository storeRepository = mock(StoreRepository.class);
        PlatformCampaignStoreRepository campaignStoreRepository = mock(PlatformCampaignStoreRepository.class);

        return new PlatformCampaignServiceImpl(
                campaignRepository,
                flashSlotRepository,
                campaignProductRepository,
                productRepository,
                storeRepository,
                campaignStoreRepository
        );
    }

    @Test
    void disable_isAllowed_whenOldIsDraft() {
        PlatformCampaignRepository campaignRepository = mock(PlatformCampaignRepository.class);
        var sut = newSut(campaignRepository);

        UUID id = UUID.randomUUID();
        PlatformCampaign campaign = new PlatformCampaign();
        campaign.setId(id);
        campaign.setStatus(VoucherStatus.DRAFT);

        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(PlatformCampaign.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> sut.updateCampaignStatus(id, "DISABLED"));
        assertEquals(VoucherStatus.DISABLED, campaign.getStatus());
        verify(campaignRepository, times(1)).save(campaign);
    }

    @Test
    void disable_isAllowed_whenOldIsOnOpen() {
        PlatformCampaignRepository campaignRepository = mock(PlatformCampaignRepository.class);
        var sut = newSut(campaignRepository);

        UUID id = UUID.randomUUID();
        PlatformCampaign campaign = new PlatformCampaign();
        campaign.setId(id);
        campaign.setStatus(VoucherStatus.ONOPEN);

        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(PlatformCampaign.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> sut.updateCampaignStatus(id, "disabled"));
        assertEquals(VoucherStatus.DISABLED, campaign.getStatus());
        verify(campaignRepository, times(1)).save(campaign);
    }

    @Test
    void disable_isRejected_whenOldIsActive() {
        PlatformCampaignRepository campaignRepository = mock(PlatformCampaignRepository.class);
        var sut = newSut(campaignRepository);

        UUID id = UUID.randomUUID();
        PlatformCampaign campaign = new PlatformCampaign();
        campaign.setId(id);
        campaign.setStatus(VoucherStatus.ACTIVE);

        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> sut.updateCampaignStatus(id, "DISABLED"));
        assertTrue(ex.getMessage().contains("Chỉ cho phép"));
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void disable_isRejected_whenOldIsExpired() {
        PlatformCampaignRepository campaignRepository = mock(PlatformCampaignRepository.class);
        var sut = newSut(campaignRepository);

        UUID id = UUID.randomUUID();
        PlatformCampaign campaign = new PlatformCampaign();
        campaign.setId(id);
        campaign.setStatus(VoucherStatus.EXPIRED);

        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> sut.updateCampaignStatus(id, "DISABLED"));
        assertTrue(ex.getMessage().contains("đã hết hạn"));
        verify(campaignRepository, never()).save(any());
    }
}
