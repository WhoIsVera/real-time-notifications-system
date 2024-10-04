package com.example.notificationservice.repository;

import com.example.notificationservice.entity.JwtSecret;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface JwtSecretRepository extends ReactiveMongoRepository<JwtSecret, String> {

    //metodo para obtener el primer secret por orden ascendente
    Mono<JwtSecret> findFirstByOrderByIdAsc();
}
