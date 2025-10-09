package org.example.audio_ecommerce.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException; // ✅ import đúng class này
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception   // ✅ đúng kiểu yêu cầu
    ) throws IOException {
        String url = redirectUri + "?error=" + URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        response.setStatus(302);
        response.setHeader("Location", url);
    }
}
