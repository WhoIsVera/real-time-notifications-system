package com.example.notificationservice.security;


import com.example.notificationservice.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public class JwtAuthenticationFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final ServerSecurityContextRepository securityContextRepository;
    private UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ServerSecurityContextRepository securityContextRepository, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.securityContextRepository = securityContextRepository;
        this.userRepository = userRepository;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = extractTokenFromRequest(exchange.getRequest());
        if (token != null) {
            return jwtUtil.validateToken(token)
                    .flatMap(isValid -> {
                        if (isValid) {
                            // Si el token es válido, proceder con la autenticación
                            return jwtUtil.getAuthentication(token)
                                    .flatMap(auth -> saveAuthentication(exchange, chain, auth));
                        } else {
                            // Intentar renovar el token si ha expirado
                            return jwtUtil.renewToken(token)
                                    .flatMap(newToken -> {
                                        // Actualizar la cabecera de la respuesta con el nuevo token
                                        exchange.getResponse().getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + newToken);
                                        // Obtener la nueva autenticación y proceder
                                        return jwtUtil.getAuthentication(newToken)
                                                .flatMap(auth -> saveAuthentication(exchange, chain, auth));
                                    })
                                    .onErrorResume(e -> {
                                        // Si no se puede renovar, devolver un error
                                        return buildErrorResponse(exchange, "Token inválido o no renovado.", HttpStatus.UNAUTHORIZED);
                                    });
                        }
                    })
                    .onErrorResume(ExpiredJwtException.class, e -> {
                        // Si el token ha expirado, tratar de renovar el token
                        return jwtUtil.renewToken(token)
                                .flatMap(newToken -> {
                                    // Actualizar la cabecera de la respuesta con el nuevo token
                                    exchange.getResponse().getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + newToken);
                                    // Obtener la nueva autenticación y proceder
                                    return jwtUtil.getAuthentication(newToken)
                                            .flatMap(auth -> saveAuthentication(exchange, chain, auth));
                                })
                                .onErrorResume(renewError -> {
                                    // En caso de error en la renovación, retornar un error 401 Unauthorized
                                    return buildErrorResponse(exchange, "Token expirado, por favor vuelva a renovar el token .", HttpStatus.UNAUTHORIZED);
                                });
                    })
                    .onErrorResume(e -> {
                        // Para cualquier otro error, devolver un 401 Unauthorized
                        return buildErrorResponse(exchange, "Token inválido.", HttpStatus.UNAUTHORIZED);
                    });
        }
        return chain.filter(exchange);
    }

    private Mono<Void> buildErrorResponse(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        String body = String.format("{\"message\": \"%s\"}", message);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }



    private String extractTokenFromRequest(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Mono<Void> saveAuthentication(ServerWebExchange exchange, WebFilterChain chain, Authentication authentication) {
        SecurityContext context = new SecurityContextImpl(authentication);
        return securityContextRepository.save(exchange, context)
                .then(chain.filter(exchange));
    }
}
