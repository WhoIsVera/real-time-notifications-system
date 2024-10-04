package com.example.notificationservice.service;

import com.example.notificationservice.entity.JwtSecret;
import com.example.notificationservice.repository.JwtSecretRepository;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class JwtSecretService {

    private final JwtSecretRepository jwtSecretRepository;

    @Autowired
    public JwtSecretService(JwtSecretRepository jwtSecretRepository) {
        this.jwtSecretRepository = jwtSecretRepository;
    }

    public Mono<String> getSecret() {
        return jwtSecretRepository.findFirstByOrderByIdAsc()
                .flatMap(jwtSecret -> Mono.just(jwtSecret.getSecret())) // Si encuentra el secreto, lo devuelve.
                .switchIfEmpty(
                        // Si no encuentra un secreto, lo generamos y guardamos
                        Mono.defer(() -> {
                            String secret = generateSecret();
                            JwtSecret newJwtSecret = new JwtSecret(secret);
                            return jwtSecretRepository.save(newJwtSecret)
                                    .map(JwtSecret::getSecret); // Guardamos y devolvemos el secreto
                        })
                );
    }

    // Metodo para generar un secreto de 512 bits (64 bytes)
    private String generateSecret() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] secretBytes = new byte[64]; // 64 bytes = 512 bits
        secureRandom.nextBytes(secretBytes);
        return Base64.getEncoder().withoutPadding().encodeToString(secretBytes);
    }
}