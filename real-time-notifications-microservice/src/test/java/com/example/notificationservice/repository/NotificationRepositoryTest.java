package com.example.notificationservice.repository;

import com.example.notificationservice.entity.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

@DataMongoTest  // Indica que el test está enfocado en la capa de persistencia de datos usando MongoDB.
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        // Limpiar la base de datos antes de cada test para evitar conflictos.
        notificationRepository.deleteAll().block();
    }

    @Test
    void testSaveAndFindByUserReferenceId() {
        // Crear notificación para un usuario específico
        Notification notification1 = new Notification("1", "user123", "Notification 1", Instant.now(), false);
        Notification notification2 = new Notification("2", "user123", "Notification 2", Instant.now(), false);

        // Guardar las notificaciones
        Flux<Notification> savedNotifications = notificationRepository.saveAll(Flux.just(notification1, notification2));

        // Verificar que las notificaciones fueron guardadas correctamente
        StepVerifier.create(savedNotifications)
                .expectNextCount(2)
                .verifyComplete();

        // Buscar notificaciones por userReferenceId
        Flux<Notification> foundNotifications = notificationRepository.findByUserReferenceId("user123")
                .sort((n1, n2) -> n1.getMessage().compareTo(n2.getMessage())); // Ordenar por el campo 'message' para garantizar el orden

        // Verificar que las notificaciones fueron encontradas correctamente
        StepVerifier.create(foundNotifications)
                .expectNextMatches(notification -> notification.getMessage().equals("Notification 1"))
                .expectNextMatches(notification -> notification.getMessage().equals("Notification 2"))
                .verifyComplete();
    }


    @Test
    void testFindByMessage() {
        // Crear notificaciones con el mismo mensaje
        Notification notification1 = new Notification("1", "user123", "Same Message", Instant.now(), false);
        Notification notification2 = new Notification("2", "user456", "Same Message", Instant.now(), false);

        // Guardar las notificaciones
        Flux<Notification> savedNotifications = notificationRepository.saveAll(Flux.just(notification1, notification2));

        // Verificar que las notificaciones fueron guardadas correctamente
        StepVerifier.create(savedNotifications)
                .expectNextCount(2)
                .verifyComplete();

        // Buscar notificaciones por mensaje
        Flux<Notification> foundNotifications = notificationRepository.findByMessage("Same Message");

        // Verificar que las notificaciones con el mensaje fueron encontradas correctamente
        StepVerifier.create(foundNotifications)
                .expectNextMatches(notification -> notification.getUserReferenceId().equals("user123"))
                .expectNextMatches(notification -> notification.getUserReferenceId().equals("user456"))
                .verifyComplete();
    }

    @Test
    void testFindById() {
        // Crear notificación y guardar en la base de datos
        Notification notification = new Notification("1", "user123", "Test Message", Instant.now(), false);
        Mono<Notification> savedNotification = notificationRepository.save(notification);

        // Verificar que la notificación fue guardada correctamente
        StepVerifier.create(savedNotification)
                .assertNext(saved -> {
                    assert saved.getId().equals("1");
                    assert saved.getMessage().equals("Test Message");
                })
                .verifyComplete();

        // Buscar la notificación por ID
        Mono<Notification> foundNotification = notificationRepository.findById("1");

        // Verificar que la notificación fue encontrada correctamente
        StepVerifier.create(foundNotification)
                .assertNext(found -> {
                    assert found.getId().equals("1");
                    assert found.getMessage().equals("Test Message");
                })
                .verifyComplete();
    }
}
