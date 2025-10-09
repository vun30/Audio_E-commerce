package org.example.audio_ecommerce.security.oauth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) {
        OAuth2User user = super.loadUser(req);
        // Bạn có thể log để debug:
        // log.info("Google attributes: {}", user.getAttributes());
        return user; // giữ nguyên, dùng SuccessHandler để persist
    }
}
