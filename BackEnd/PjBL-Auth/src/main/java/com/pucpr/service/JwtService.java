package com.pucpr.service;

import com.pucpr.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtService {

    // Carregada via variável de ambiente JWT_SECRET (mínimo 32 caracteres para HS256).
    // Fallback só deve ser usado em desenvolvimento local.
    private final String SECRET_KEY;

    public JwtService() {
        String envKey = System.getenv("JWT_SECRET");
        this.SECRET_KEY = (envKey != null && envKey.length() >= 32)
                ? envKey
                : "chave_padrao_apenas_dev_32chars!!";
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String generateToken(Usuario user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole())
                .claim("name", user.getNome())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000)) // 15 min
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            System.err.println("Token inválido: " + e.getMessage());
            return false;
        }
    }
}
