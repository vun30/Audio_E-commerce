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
    private String defaultRedirectUri;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception   // ✅ đúng kiểu yêu cầu
    ) throws IOException {
        String platform = request.getParameter("platform");
        String redirectUri = request.getParameter("redirect_uri");
        String errorMessage = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        
        String url;
        
        // 🔹 MOBILE: Redirect to deep link with error
        if ("mobile".equalsIgnoreCase(platform) || 
            (redirectUri != null && redirectUri.startsWith("mobdoan://"))) {
            url = "mobdoan://oauth2/success?error=" + errorMessage;
        }
        // 🔹 WEB: Redirect to web error page
        else {
            url = defaultRedirectUri + "?error=" + errorMessage;
        }
        
        response.setStatus(302);
        response.setHeader("Location", url);
    }
}
