package com.example.notificationservice.repository;

import com.example.notificationservice.entity.Notification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface NotificationRepository extends ReactiveMongoRepository<Notification, String> {
    // Devuelve todas las notificaciones para un usuario específico
    Flux<Notification> findByUserReferenceId(String userReferenceId);

    //Devuelve todos los  userID o los mutiples userID que tienen el mismo mensaje
    Flux<Notification> findByMessage(String message);

    //Mono<Notification> findByMessage(String message);
}
