package com.pucpr.service;

import com.pucpr.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Base64;
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
                .signWith(getSigningKey()) // força HS256
                .compact();
    }

    public String extractEmail(String token) {
        rejectUnsecuredToken(token);
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            rejectUnsecuredToken(token);
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token); // lança exceção se expirado ou assinatura inválida
            return true;
        } catch (Exception e) {
            System.err.println("Token rejeitado: " + e.getMessage());
            return false;
        }
    }

    /**
     * Proteção explícita contra o ataque alg:none (CVE-2015-9235).
     * Decodifica o header Base64url e rejeita qualquer token cujo "alg" não seja HS256.
     * O JJWT 0.12.5 já bloqueia isso via parseSignedClaims(), mas a verificação
     * antecipada torna a defesa independente da biblioteca e auditável.
     */
    private void rejectUnsecuredToken(String token) {
        if (token == null || !token.contains(".")) {
            throw new IllegalArgumentException("Formato de token inválido.");
        }
        String headerB64 = token.split("\\.")[0];
        // Base64url → Base64 padrão
        String headerJson = new String(
                Base64.getUrlDecoder().decode(padBase64(headerB64))
        );
        String algLower = headerJson.toLowerCase();
        if (algLower.contains("\"alg\"") && (algLower.contains("\"none\"") || algLower.contains("none"))) {
            throw new SecurityException("alg:none não é permitido.");
        }
        if (!algLower.contains("\"hs256\"")) {
            throw new SecurityException("Algoritmo de assinatura não aceito.");
        }
    }

    private String padBase64(String b64) {
        return b64 + "=".repeat((4 - b64.length() % 4) % 4);
    }
}
