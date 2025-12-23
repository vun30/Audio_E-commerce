// OAuth2AuthenticationSuccessHandler.java
package org.example.audio_ecommerce.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.entity.Account;
import org.example.audio_ecommerce.entity.Customer;
import org.example.audio_ecommerce.entity.Enum.RoleEnum;
import org.example.audio_ecommerce.repository.AccountRepository;
import org.example.audio_ecommerce.repository.CustomerRepository; // 👈 thêm
import org.example.audio_ecommerce.security.JwtTokenProvider;
import org.example.audio_ecommerce.service.Impl.DomainProvisioningService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository; // 👈 thêm
    private final JwtTokenProvider jwtTokenProvider;
    private final DomainProvisioningService provisioningService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attr = oAuth2User.getAttributes();

        String email = (String) attr.getOrDefault("email", "");
        String name  = (String) attr.getOrDefault("name", email);

        // tránh circular: dùng encoder cục bộ
        var passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

        // 1) Tìm hoặc tạo Account (mặc định CUSTOMER)
        Account account = accountRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            String randomPwd = passwordEncoder.encode("GOOG-" + UUID.randomUUID());
            Account acc = Account.builder()
                    .email(email)
                    .name(name)
                    .password(randomPwd)
                    .role(RoleEnum.CUSTOMER)
                    .build();
            return accountRepository.save(acc);
        });

        // 2) Đảm bảo có Customer + Wallet
        provisioningService.ensureCustomerAndWallet(account);

        // 3) Lấy customerId theo accountId
        UUID customerId = customerRepository.findByAccount_Id(account.getId())
                .map(Customer::getId)
                .orElse(null);

        // 4) Phát JWT kèm accountId + customerId
        String accessToken = jwtTokenProvider.generateToken(
                account.getId(),           // accountId
                customerId,                // customerId
                account.getEmail(),
                account.getRole().name()
        );
        
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                account.getId(),           // accountId
                customerId,                // customerId
                account.getEmail(),
                account.getRole().name()
        );

        // 5) Detect platform (mobile vs web) and redirect accordingly
        String platform = request.getParameter("platform");
        String redirectUri = request.getParameter("redirect_uri");
        
        try {
            String redirectUrl;
            
            // 🔹 MOBILE: Redirect to deep link
            if ("mobile".equalsIgnoreCase(platform) || 
                (redirectUri != null && redirectUri.startsWith("mobdoan://"))) {
                redirectUrl = String.format(
                        "mobdoan://oauth2/success?token=%s&refreshToken=%s&accountId=%s&customerId=%s",
                        URLEncoder.encode(accessToken, StandardCharsets.UTF_8),
                        URLEncoder.encode(refreshToken, StandardCharsets.UTF_8),
                        account.getId(),
                        customerId != null ? customerId.toString() : ""
                );
            } 
            // 🔹 WEB: Redirect to web success page
            else {
                redirectUrl = String.format(
                        "https://sep-490-audio-wep-app.vercel.app/oauth-success?accessToken=%s&refreshToken=%s&accountId=%s&customerId=%s",
                        accessToken,
                        refreshToken,
                        account.getId(),
                        customerId != null ? customerId.toString() : ""
                );
            }
            
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth2 callback failed");
        }
    }
}
