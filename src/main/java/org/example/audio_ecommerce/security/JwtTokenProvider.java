package org.example.audio_ecommerce.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import java.nio.charset.StandardCharsets;
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

    private final Key key;
    private final long expiry;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretBase64,
            @Value("${jwt.expiration-ms}") long expiry,
            UserDetailsService uds
    ) {
        this.uds = uds;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64)); // >=256-bit
        this.expiry = expiry;
    }

    public String generateToken(String subject) {
        var now = new Date();
        var exp = new Date(now.getTime() + expiry);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) { return false; }
    }

    public Authentication getAuthentication(String token) {
        String username = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
        var user = uds.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }
}
