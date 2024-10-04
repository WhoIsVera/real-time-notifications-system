package com.example.notificationservice.security;

import com.example.notificationservice.entity.JwtSecret;
import com.example.notificationservice.repository.JwtSecretRepository;
import com.example.notificationservice.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private final JwtSecretRepository jwtSecretRepository;
    private final UserRepository userRepository;

    @Autowired
    public JwtUtil(JwtSecretRepository jwtSecretRepository, UserRepository userRepository) {
        this.jwtSecretRepository = jwtSecretRepository;
        this.userRepository = userRepository;
    }

    public Mono<Authentication> getAuthentication(String token) {
        return extractUsername(token).flatMap(username -> {
            if (username != null) {
                return userRepository.findByEmail(username)
                        .map(user -> new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        ));
            } else {
                return Mono.empty();
            }
        });
    }

    // Método ajustado para permitir establecer la duración del token
    public Mono<String> generateToken(String username, long durationMillis) {
        return getSecret().flatMap(secret -> {
            String token = Jwts.builder()
                    .setSubject(username)
                    .setExpiration(new Date(System.currentTimeMillis() + durationMillis)) // Expira según la duración especificada
                    .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                    .compact();
            return Mono.just(token);
        });
    }

    // Generación del token inicial con duración específica (2 minutos)
    public Mono<String> generateToken(String username) {
        long initialDurationMillis = 1000 * 60 * 2; // 2 minutos
        return generateToken(username, initialDurationMillis);
    }

    // Método ajustado para renovar el token con una nueva duración
    public Mono<String> renewToken(String token) {
        return getSecret().flatMap(secretKey -> {
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                // Si los claims existen, generar un nuevo token basado en ellos
                long renewedDurationMillis = 1000 * 60 * 5; // 5 minutos para el token renovado
                return generateToken(claims.getSubject(), renewedDurationMillis);
            } catch (ExpiredJwtException e) {
                // En caso de que esté expirado pero los claims sean accesibles
                Claims claims = e.getClaims();
                if (claims != null) {
                    long renewedDurationMillis = 1000 * 60 * 5; // 5 minutos para el token renovado
                    return generateToken(claims.getSubject(), renewedDurationMillis);
                }
                return Mono.error(new RuntimeException("No se puede renovar el token: no se puede acceder a los claims."));
            } catch (Exception e) {
                return Mono.error(new RuntimeException("No se puede renovar el token: token inválido."));
            }
        });
    }

    public Mono<Boolean> validateToken(String token) {
        return getSecret().flatMap(secretKey -> {
            try {
                Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseClaimsJws(token);
                return Mono.just(true);
            } catch (ExpiredJwtException e) {
                log.info("El token ha expirado: {}", e.getMessage());
                return Mono.error(new ExpiredJwtException(null, null, "Token Expirado"));
            } catch (Exception e) {
                log.error("Error al validar el token: {}", e.getMessage());
                return Mono.just(false);
            }
        });
    }

    public Mono<String> extractUsername(String token) {
        return getSecret().flatMap(secret -> {
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
                return Mono.just(claims.getSubject());
            } catch (ExpiredJwtException e) {
                log.info("El token ha expirado, pero se puede extraer el nombre de usuario: {}", e.getClaims().getSubject());
                return Mono.just(e.getClaims().getSubject());
            } catch (Exception e) {
                log.error("Error al extraer el nombre de usuario del token JWT: {}", e.getMessage());
                return Mono.error(new RuntimeException("Token inválido"));
            }
        });
    }

    public Mono<String> getSecret() {
        return jwtSecretRepository.findFirstByOrderByIdAsc()
                .flatMap(jwtSecret -> Mono.just(jwtSecret.getSecret()))
                .switchIfEmpty(Mono.defer(() -> {
                    String secret = generateSecret();
                    JwtSecret newJwtSecret = new JwtSecret(secret);
                    return jwtSecretRepository.save(newJwtSecret).map(JwtSecret::getSecret);
                }));
    }

    private String generateSecret() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] secretBytes = new byte[64];
        secureRandom.nextBytes(secretBytes);
        return Base64.getEncoder().encodeToString(secretBytes);
    }
}

