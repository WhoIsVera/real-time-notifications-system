package com.example.notificationservice.controller;

import com.example.notificationservice.HttpResponse.CustomApiResponse;
import com.example.notificationservice.HttpResponse.ResponseUtil;
import com.example.notificationservice.dto.NotificationDto;
import com.example.notificationservice.dto.NotificationRequestDto;
import com.example.notificationservice.dto.NotificationResponseDto;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.notificationservice.security.JwtUtil;
import com.example.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api-clients/v1.0/notifications")
@Tag(name = "Notifications", description = "Operations related to Notifications in the notification system")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final JwtUtil jwtUtil;

    @Autowired
    public NotificationController(NotificationService notificationService, NotificationRepository notificationRepository, JwtUtil jwtUtil) {
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.jwtUtil = jwtUtil;
    }


            // Endpoint para obtener notificaciones no leídas en tiempo real para todos los usuarios
            @Operation(summary = "Stream unread notifications for all users", description = "Stream unread notifications in real time for all users in the system")
            @ApiResponses(value = {
                    @ApiResponse(responseCode = "200", description = "Successfully streaming unread notifications",
                            content = @Content(schema = @Schema(implementation = Notification.class)))
            })

            // Endpoint SSE para transmitir notificaciones no leídas en tiempo real de todos los usuarios existentes en bdd
            @GetMapping(value = "/users/unread-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
            public Flux<Notification> streamUnreadNotifications() {
                return notificationService.getUnreadNotificationStream();  // Flujo global de notificaciones no leídas
            }

                    // Anotaciones para documentar el el edpoint de II-.  getNotificationsUserIdByMessage
                    @Operation(summary = "Create a notification")
                    @ApiResponses(value = {
                            @ApiResponse(responseCode = "200", description = "Notification created successfully"),
                            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid token"),
                            @ApiResponse(responseCode = "500", description = "Internal server error")
                    })

                    @PostMapping("/users/{userId}")
                    public Mono<ResponseEntity<CustomApiResponse<Notification>>> createNotification(
                            @PathVariable String userId,
                            @Valid @RequestBody NotificationRequestDto notificationRequest,
                            @RequestHeader("Authorization") String token) {

                        // Validar el token recibido
                        return jwtUtil.validateToken(token.replace("Bearer ", "").trim())
                                .flatMap(isValid -> {
                                    if (!Boolean.TRUE.equals(isValid)) {
                                        CustomApiResponse<Notification> errorResponse = new CustomApiResponse<>(
                                                "error",
                                                "Token inválido",
                                                null,
                                                HttpStatus.UNAUTHORIZED.value()
                                        );
                                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
                                    }

                                    // Si el token es válido, creamos la notificación
                                    Notification notification = new Notification();
                                    notification.setUserReferenceId(userId);
                                    notification.setMessage(notificationRequest.getMessage());
                                    notification.setTimestamp(Instant.now());
                                    notification.setRead(false);

                                    return notificationService.createNotification(userId, notification)
                                            .flatMap(createdNotification -> {
                                                CustomApiResponse<Notification> response = new CustomApiResponse<>(
                                                        "success",
                                                        "Notificación creada exitosamente",
                                                        createdNotification,
                                                        HttpStatus.OK.value()
                                                );
                                                return Mono.just(ResponseEntity.ok(response));
                                            })
                                            .onErrorResume(e -> {
                                                CustomApiResponse<Notification> errorResponse = new CustomApiResponse<>(
                                                        "error",
                                                        "Error al crear la notificación: " + e.getMessage(),
                                                        null,
                                                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                                                );
                                                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                                            });
                                });
                    }

                            @PutMapping("/{notificationId}/read-and-delete")
                            @Operation(summary = "Mark a notification as read and delete it", description = "Marks a notification as read and then deletes it from both the notification collection and the user's notification list")
                            @ApiResponses(value = {
                                    @ApiResponse(responseCode = "200", description = "Notification successfully marked as read and deleted"),
                                    @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid Token"),
                                    @ApiResponse(responseCode = "404", description = "Notification not found")
                            })
                            public Mono<ResponseEntity<CustomApiResponse<Void>>> markNotificationAsReadAndDelete(
                                    @RequestHeader("Authorization") String token,
                                    @PathVariable String notificationId) {

                                String jwtToken = token.replace("Bearer ", "").trim();

                                return jwtUtil.validateToken(jwtToken)
                                        .flatMap(isValid -> {
                                            if (!isValid) {
                                                CustomApiResponse<Void> response = new CustomApiResponse<>(
                                                        "error",
                                                        "Token inválido. Verifique sus credenciales o el token que está utilizando.",
                                                        null,
                                                        HttpStatus.UNAUTHORIZED.value()
                                                );
                                                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response));
                                            }

                                            // Si el token es válido, procedemos a actualizar el estado de la notificación
                                            return notificationService.markNotificationAsReadAndDelete(notificationId)
                                                    .flatMap(successMessage -> {
                                                        CustomApiResponse<Void> response = new CustomApiResponse<>(
                                                                "success",
                                                                successMessage,
                                                                null,
                                                                HttpStatus.OK.value()
                                                        );
                                                        return Mono.just(ResponseEntity.ok(response));
                                                    })
                                                    .onErrorResume(e -> {
                                                        CustomApiResponse<Void> errorResponse = new CustomApiResponse<>(
                                                                "error",
                                                                e.getMessage(),
                                                                null,
                                                                HttpStatus.NOT_FOUND.value()
                                                        );
                                                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse));
                                                    });
                                        });
                            }

                                        //Endpoint para obtener las notificaciones de un usuario especifico en tiempo real
                                        @Operation(summary = "Get stream user notifications", description = "Stream notifications for a specific user in real time")
                                        @ApiResponses(value = {
                                                @ApiResponse(responseCode = "200", description = "Successfully streaming notifications",
                                                        content = @Content(schema = @Schema(implementation = Notification.class))),
                                                @ApiResponse(responseCode = "404", description = "Notifications for a user not found")
                                        })
                                        @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
                                        public Flux<Notification> streamNotifications(@PathVariable String userId) {
                                            return notificationService.getNotificationsStream(userId);  // Flujo de notificaciones para un usuario específico
                                        }

}