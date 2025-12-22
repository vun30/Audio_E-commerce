package org.example.audio_ecommerce.config;


import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.security.JwtFilter;
import org.example.audio_ecommerce.security.oauth.OAuth2AuthenticationFailureHandler;
import org.example.audio_ecommerce.security.oauth.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@Component
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final OAuth2UserService oAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler  oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfiguration = new CorsConfiguration();
                    corsConfiguration.setAllowedHeaders(List.of("*"));
                    corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                    corsConfiguration.setAllowedOrigins(List.of(
                            "http://localhost:3000",
                            "http://localhost:5173",
                            "https://audioe-commerce-production.up.railway.app"
                    ));
                    corsConfiguration.setAllowCredentials(true);
                    return corsConfiguration;
                }))

                // ✅ CHO PHÉP TẠO SESSION KHI CẦN (AI, OAuth2, chat memory...)
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                .authorizeHttpRequests(auth -> auth
                        // ✅ THẢ TOÀN BỘ API
                        .requestMatchers("/api/**").permitAll()

                        // swagger, oauth, webhook...
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/loaderio-*"
                        ).permitAll()

                        // ❗ còn lại cũng cho qua
                        .anyRequest().permitAll()
                )

                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(ae -> ae.baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(re -> re.baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(ui -> ui.userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )

                // ✅ JWT filter vẫn chạy (nếu có token thì set SecurityContext)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthEntryPoint())
                );

        return http.build();
    }

    //Trả lỗi cho Authen
    @Bean
    AuthenticationEntryPoint restAuthEntryPoint() {
        return (req, res, ex) -> {
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":401,\"message\":\"Unauthorized\"}");
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
