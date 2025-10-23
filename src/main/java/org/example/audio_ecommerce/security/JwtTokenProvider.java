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
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final UserDetailsService uds;
    private final Key key;
    private final Key refreshKey;
    private final long expiry;
    private final long refreshExpiry;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretBase64,
            @Value("${jwt.expiration-ms}") long expiry,
            @Value("${jwt.refresh-secret}") String refreshSecretBase64,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpiry,
            UserDetailsService uds
    ) {
        this.uds = uds;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.refreshKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecretBase64));
        this.expiry = expiry;
        this.refreshExpiry = refreshExpiry;
    }

    // ✅ ACCESS TOKEN: subject = email:ROLE (giữ đúng format)
    public String generateToken(UUID id, UUID customerId, String email, String role) {
        var now = new Date();
        var exp = new Date(now.getTime() + expiry);

        return Jwts.builder()
                .setSubject(email + ":" + role)
                .claim("accountId", id != null ? id.toString() : null)
                .claim("customerId", customerId != null ? customerId.toString() : null)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ REFRESH TOKEN: chỉ chứa thông tin cần thiết để tạo lại access token
    public String generateRefreshToken(UUID id, UUID customerId, String email, String role) {
        var now = new Date();
        var exp = new Date(now.getTime() + refreshExpiry);

        return Jwts.builder()
                .setSubject(email + ":" + role)
                .claim("accountId", id != null ? id.toString() : null)
                .claim("customerId", customerId != null ? customerId.toString() : null)
                .claim("role", role)
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(refreshKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String refreshToken) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(refreshKey)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();
            
            // Kiểm tra xem có phải refresh token không
            String type = claims.get("type", String.class);
            return "refresh".equals(type);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String usernameWithRole = claims.getSubject(); // email:ROLE
        var user = uds.loadUserByUsername(usernameWithRole);

        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    public String getRoleFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    public UUID getCustomerIdFromToken(String token) {
        String id = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody()
                .get("customerId", String.class);
        return (id != null) ? UUID.fromString(id) : null;
    }

    // ✅ Lấy thông tin từ refresh token
    public Claims getClaimsFromRefreshToken(String refreshToken) {
        return Jwts.parserBuilder()
                .setSigningKey(refreshKey)
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();
    }

    public UUID getAccountIdFromRefreshToken(String refreshToken) {
        String id = getClaimsFromRefreshToken(refreshToken).get("accountId", String.class);
        return (id != null) ? UUID.fromString(id) : null;
    }

    public UUID getCustomerIdFromRefreshToken(String refreshToken) {
        String id = getClaimsFromRefreshToken(refreshToken).get("customerId", String.class);
        return (id != null) ? UUID.fromString(id) : null;
    }

    public String getEmailFromRefreshToken(String refreshToken) {
        String subject = getClaimsFromRefreshToken(refreshToken).getSubject();
        return subject.split(":")[0];
    }

    public String getRoleFromRefreshToken(String refreshToken) {
        String subject = getClaimsFromRefreshToken(refreshToken).getSubject();
        return subject.split(":")[1];
    }

}
