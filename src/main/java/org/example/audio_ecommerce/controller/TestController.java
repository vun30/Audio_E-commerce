package org.example.audio_ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {

    // ✅ API public, không cần token
    @GetMapping("/public")
    public ResponseEntity<String> publicApi() {
        return ResponseEntity.ok("✅ Public API: ai cũng gọi được, không cần token");
    }

    // ✅ API private, cần token hợp lệ
    @GetMapping("/private")
    public ResponseEntity<String> privateApi(Authentication authentication) {
        return ResponseEntity.ok("🔒 Private API: bạn đã login với user = " + authentication.getName());
    }

    // ✅ API test role
    @GetMapping("/role")
    public ResponseEntity<String> testRole(@AuthenticationPrincipal org.springframework.security.core.userdetails.User user) {
        return ResponseEntity.ok("👤 User: " + user.getUsername() +
                                 " | Roles: " + user.getAuthorities());
    }
}
