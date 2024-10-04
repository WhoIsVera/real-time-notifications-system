package com.example.notificationservice.controller;

import com.example.notificationservice.HttpResponse.CustomApiResponse;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.User;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.notificationservice.repository.UserRepository;
import com.example.notificationservice.security.JwtUtil;
import com.example.notificationservice.service.NotificationService;
import com.example.notificationservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
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

@WebFluxTest(UserController.class)
class UserControllerTest {

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(
                new UserController(userService, userRepository, notificationService, jwtUtil, passwordEncoder)
        ).build();
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

        // Simula el comportamiento del método del servicio userService
        when(userService.getAllUsersWithNotificationMessages()).thenReturn(Flux.just(user1, user2));

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
        user.setId("1");
        user.setName("Test User");
        user.setEmail("testuser@example.com");
        user.setPassword("password123");

        // Simula el servicio de guardar usuario
        when(userService.saveUser(any(User.class))).thenReturn(Mono.just(user));

        // Simula la generación del token JWT
        when(jwtUtil.generateToken(anyString())).thenReturn(Mono.just("mockedJwtToken"));

        // Ejecuta la petición POST
        webTestClient.post().uri("/api-clients/v1.0/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomApiResponse.class)
                .consumeWith(response -> {
                    CustomApiResponse<?> customApiResponse = response.getResponseBody();
                    assert customApiResponse != null;
                    assert customApiResponse.getStatus().equals("success");
                    assert customApiResponse.getMessage().equals("Usuario guardado con éxito");
                });
    }


    @Test
    void testDeleteUser() {
        // Simula el servicio
        when(userService.deleteUserById(anyString())).thenReturn(Mono.just("Usuario con ID: 1 ha sido eliminado con éxito."));

        // Ejecuta la petición DELETE
        webTestClient.delete().uri("/api-clients/v1.0/users/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomApiResponse.class)
                .consumeWith(response -> {
                    CustomApiResponse<?> customApiResponse = response.getResponseBody();
                    assert customApiResponse != null;
                    assert customApiResponse.getStatus().equals("success");
                    assert customApiResponse.getMessage().equals("Usuario con ID: 1 ha sido eliminado con éxito.");
                });
    }

    @Test
    void testDeleteUser_UserNotFound() {
        // Simula el servicio cuando el usuario no existe
        when(userService.deleteUserById(anyString())).thenReturn(Mono.error(new RuntimeException("Usuario no encontrado con ID: 1")));

        // Ejecuta la petición DELETE
        webTestClient.delete().uri("/api-clients/v1.0/users/1")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(CustomApiResponse.class)
                .consumeWith(response -> {
                    CustomApiResponse<?> customApiResponse = response.getResponseBody();
                    assert customApiResponse != null;
                    assert customApiResponse.getStatus().equals("error");
                    assert customApiResponse.getMessage().equals("Usuario no encontrado con ID: 1");
                });
    }
}