package com.example.notificationservice.controller;

import com.example.notificationservice.HttpResponse.CustomApiResponse;
import com.example.notificationservice.dto.NotificationRequestDto;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.notificationservice.security.JwtUtil;
import com.example.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(NotificationController.class)
class NotificationControllerTest {

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private JwtUtil jwtUtil;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(
                new NotificationController(notificationService, notificationRepository, jwtUtil)
        ).build();
    }

    @Test
    void testCreateNotification() {
        // Datos de ejemplo
        NotificationRequestDto notificationRequest = new NotificationRequestDto("Notification message");
        Notification notification = new Notification("1", "user1", notificationRequest.getMessage(), Instant.now(), false);

        // Simular la validación del token y la creación de la notificación
        when(jwtUtil.validateToken(anyString())).thenReturn(Mono.just(true));
        when(notificationService.createNotification(anyString(), any(Notification.class))).thenReturn(Mono.just(notification));

        // Ejecuta la petición POST
        webTestClient.post()
                .uri("/api-clients/v1.0/notifications/users/{userId}", "user1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid_token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(notificationRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<CustomApiResponse<Notification>>() {})
                .value(response -> {
                    assertEquals("success", response.getStatus());
                    assertEquals("Notificación creada exitosamente", response.getMessage());
                    assertEquals(notificationRequest.getMessage(), response.getData().getMessage());
                });
    }



    @Test
    void shouldMarkNotificationAsReadAndDelete() {
        // Datos de prueba
        String notificationId = "171c72";
        String successMessage = "La notificación con ID: '" + notificationId + "' fue marcada como leída y eliminada con éxito.";
        CustomApiResponse<Void> apiResponse = new CustomApiResponse<>("success", successMessage, null, 200);

        // Simular la validación del token y el marcado de la notificación como leída y eliminada
        when(jwtUtil.validateToken(anyString())).thenReturn(Mono.just(true));
        when(notificationService.markNotificationAsReadAndDelete(notificationId)).thenReturn(Mono.just(successMessage));

        // Ejecuta la solicitud PUT
        webTestClient.put()
                .uri("/api-clients/v1.0/notifications/{notificationId}/read-and-delete", notificationId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid_token")
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomApiResponse.class)
                .value(response -> {
                    assertEquals("success", response.getStatus());
                    assertEquals(successMessage, response.getMessage());
                });
    }

    @Test
    void shouldReturnNotFoundWhenNotificationDoesNotExist() {
        // Datos de prueba
        String notificationId = "nonExistingId";
        String errorMessage = "No se encontró la notificación con el ID proporcionado.";
        CustomApiResponse<Void> apiResponse = new CustomApiResponse<>("error", errorMessage, null, 404);

        // Simular la validación del token y el fallo al encontrar la notificación
        when(jwtUtil.validateToken(anyString())).thenReturn(Mono.just(true));
        when(notificationService.markNotificationAsReadAndDelete(notificationId)).thenReturn(Mono.error(new RuntimeException(errorMessage)));

        // Ejecuta la solicitud PUT
        webTestClient.put()
                .uri("/api-clients/v1.0/notifications/{notificationId}/read-and-delete", notificationId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid_token")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(CustomApiResponse.class)
                .value(response -> {
                    assertEquals("error", response.getStatus());
                    assertEquals(errorMessage, response.getMessage());
                });
    }
}
