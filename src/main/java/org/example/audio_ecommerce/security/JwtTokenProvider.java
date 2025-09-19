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

@Component
public class JwtTokenProvider {
    private final UserDetailsService uds;

    private final Key accessKey;
    private final long accessExpiryMs;

    private final Key refreshKey;
    private final long refreshExpiryMs;

    public JwtTokenProvider(
            UserDetailsService uds,
            @Value("${jwt.access-secret}") String accessSecretBase64,
            @Value("${jwt.access-expiration-ms}") long accessExpiryMs,
            @Value("${jwt.refresh-secret}") String refreshSecretBase64,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpiryMs
    ) {
        this.uds = uds;
        this.accessKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessSecretBase64));   // >= 256-bit
        this.accessExpiryMs = accessExpiryMs;
        this.refreshKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecretBase64)); // >= 256-bit
        this.refreshExpiryMs = refreshExpiryMs;
    }

    /* ===================== ACCESS TOKEN ===================== */

    public String generateAccessToken(String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessExpiryMs);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim("typ", "access")
                .signWith(accessKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        try {
            var claims = Jwts.parserBuilder().setSigningKey(accessKey).build().parseClaimsJws(token);
            Object typ = claims.getBody().get("typ");
            return "access".equals(typ);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /* ===================== REFRESH TOKEN ===================== */

    public String generateRefreshToken(String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshExpiryMs);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim("typ", "refresh")
                .signWith(refreshKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateRefreshToken(String token) {
        try {
            var claims = Jwts.parserBuilder().setSigningKey(refreshKey).build().parseClaimsJws(token);
            Object typ = claims.getBody().get("typ");
            return "refresh".equals(typ);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /* ===================== COMMON HELPERS ===================== */

    public String getUsernameFromAccess(String accessToken) {
        return Jwts.parserBuilder().setSigningKey(accessKey).build()
                .parseClaimsJws(accessToken).getBody().getSubject();
    }

    public String getUsernameFromRefresh(String refreshToken) {
        return Jwts.parserBuilder().setSigningKey(refreshKey).build()
                .parseClaimsJws(refreshToken).getBody().getSubject();
    }

    public Authentication getAuthentication(String accessToken) {
        String username = getUsernameFromAccess(accessToken);
        var user = uds.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }
}
