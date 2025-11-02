package org.example.audio_ecommerce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.GhnFeeRequest;
import org.example.audio_ecommerce.service.GhnFeeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class GhnFeeServiceImpl implements GhnFeeService {

    private final RestTemplate restTemplate;

    @Value("${ghn.token}")
    private String ghnToken;

    @Value("${ghn.shopId}")
    private String ghnShopId;

    private static final String BASE_URL = "https://online-gateway.ghn.vn/shiip/public-api";

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("Token", ghnToken);
        h.set("ShopId", ghnShopId);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @Override
    public String calculateFeeRaw(GhnFeeRequest req) {
        HttpEntity<GhnFeeRequest> entity = new HttpEntity<>(req, headers());
        ResponseEntity<String> resp = restTemplate.exchange(
                BASE_URL + "/v2/shipping-order/fee",
                HttpMethod.POST, entity, String.class);
        return resp.getBody();
    }
}
