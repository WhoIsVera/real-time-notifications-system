package com.example.notificationservice.service;

import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.User;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.notificationservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllUsersWithNotificationMessages() {
        // Crea datos de ejemplo de usuarios
        User user1 = new User();
        user1.setId(UUID.randomUUID().toString());
        user1.setNotifications(Arrays.asList("notif1", "notif2"));

        User user2 = new User();
        user2.setId(UUID.randomUUID().toString());
        user2.setNotifications(Collections.emptyList());

        // Crea datos de ejemplo de notificaciones
        Notification notification1 = new Notification("notif1", user1.getId(), "Notification message 1", Instant.now(), false);
        Notification notification2 = new Notification("notif2", user1.getId(), "Notification message 2", Instant.now(), false);

        // Simula el comportamiento del repositorio de usuarios
        when(userRepository.findAll()).thenReturn(Flux.just(user1, user2));

        // Simula el comportamiento del repositorio de notificaciones
        when(notificationRepository.findByUserReferenceId(user1.getId())).thenReturn(Flux.just(notification1, notification2));
        when(notificationRepository.findByUserReferenceId(user2.getId())).thenReturn(Flux.empty());

        // Ejecuta el método del servicio
        Flux<User> result = userService.getAllUsersWithNotificationMessages();

        // Verifica el comportamiento esperado
        StepVerifier.create(result)
                .expectNext(user1)  // Se espera que devuelva el primer usuario
                .expectNext(user2)  // Se espera que devuelva el segundo usuario
                .verifyComplete();
    }


    @Test
    void testSaveUser() {
        // Crea un usuario de ejemplo
        User user = new User();
        user.setId(null);  // El ID debe ser generado
        user.setName("Test User");

        // Simula el comportamiento del repositorio
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        // Ejecuta el metodo del servicio
        Mono<User> result = userService.saveUser(user);

        // Verifica el comportamiento esperado
        StepVerifier.create(result)
                .expectNextMatches(savedUser -> savedUser.getId() != null && savedUser.getName().equals("Test User"))
                .verifyComplete();
    }

    @Test
    void testDeleteUserById_Success() {
        // Crea un usuario de ejemplo
        String userId = UUID.randomUUID().toString();
        User user = new User();
        user.setId(userId);

        // Simula el comportamiento del repositorio
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.deleteById(userId)).thenReturn(Mono.empty());

        // Ejecuta el metodo del servicio
        Mono<String> result = userService.deleteUserById(userId);

        // Verifica el comportamiento esperado
        StepVerifier.create(result)
                .expectNext("Usuario con ID: " + userId + " ha sido eliminado con éxito.")
                .verifyComplete();
    }

    @Test
    void testDeleteUserById_UserNotFound() {
        String userId = UUID.randomUUID().toString();

        // Simula que el usuario no existe
        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        // Ejecuta el metodo del servicio
        Mono<String> result = userService.deleteUserById(userId);

        // Verifica el comportamiento esperado
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Usuario no encontrado con ID: " + userId))
                .verify();
    }
}