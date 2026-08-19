package com.example.Restaurante.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpiration;

    public String gerarToken(ClienteUserDetails userDetails) {
        long now = System.currentTimeMillis();
        long expiration = now + jwtExpiration;

        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = String.format(
                "{\"sub\":\"%s\",\"id\":\"%s\",\"papel\":\"%s\",\"iat\":%d,\"exp\":%d}",
                userDetails.getUsername(),
                userDetails.getCliente().getId(),
                userDetails.getCliente().getPapel().name(),
                now / 1000,
                expiration / 1000
        );

        String headerEncoded = encodeBase64(header);
        String payloadEncoded = encodeBase64(payload);
        String signature = gerarAssinatura(headerEncoded, payloadEncoded);

        return headerEncoded + "." + payloadEncoded + "." + signature;
    }

    public String extrairEmail(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Pattern pattern = Pattern.compile("\"sub\":\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(payload);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validarToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Pattern pattern = Pattern.compile("\"exp\":(\\d+)");
            Matcher matcher = pattern.matcher(payload);

            if (!matcher.find()) return false;
            long exp = Long.parseLong(matcher.group(1)) * 1000;

            if (System.currentTimeMillis() > exp) return false;

            String signature = gerarAssinatura(parts[0], parts[1]);
            return signature.equals(parts[2]);
        } catch (Exception e) {
            return false;
        }
    }

    private String gerarAssinatura(String header, String payload) {
        try {
            String data = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String encodeBase64(String input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }
}
