package org.example.audio_ecommerce.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private final UserDetailsService uds;
    private final Key key;
    private final long expiry;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretBase64,
            @Value("${jwt.expiration-ms}") long expiry,
            UserDetailsService uds
    ) {
        this.uds = uds;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.expiry = expiry;
    }

    // 🔹 Sinh token với email + role
    public String generateToken(String email, String role) {
        var now = new Date();
        var exp = new Date(now.getTime() + expiry);

        return Jwts.builder()
                .setSubject(email) // email làm subject
                .addClaims(Map.of("role", role)) // thêm role vào claim
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 🔹 Kiểm tra token hợp lệ
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 🔹 Lấy Authentication từ token
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String email = claims.getSubject();
        String role = claims.get("role", String.class);

        var user = uds.loadUserByUsername(email);

        // UserDetailsService phải trả về UserDetails có authorities từ DB
        // Nếu bạn muốn lấy role từ token thay vì DB:
        // var auth = new SimpleGrantedAuthority("ROLE_" + role);
        // return new UsernamePasswordAuthenticationToken(email, null, List.of(auth));

        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    // 🔹 Lấy role trực tiếp từ token (nếu cần dùng nhanh)
    public String getRoleFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }
}
