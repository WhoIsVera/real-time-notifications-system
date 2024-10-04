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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateNotification() {
        // Datos de ejemplo
        Notification notification = new Notification("1", "user1", "Notification message", Instant.now(), false);
        User user = new User();
        user.setId("user1");
        user.setName("Test User");
        user.setNotifications(new ArrayList<>()); // Utiliza una lista mutable

        // Configuración de comportamiento de los mocks
        when(notificationRepository.save(any(Notification.class))).thenReturn(Mono.just(notification));
        when(userRepository.findById("user1")).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        // Llamada al método del servicio
        Mono<Notification> result = notificationService.createNotification("user1", notification);

        // Verificar el resultado
        StepVerifier.create(result)
                .assertNext(savedNotification -> {
                    assert savedNotification.getMessage().equals("Notification message");
                    assert savedNotification.getUserReferenceId().equals("user1");
                })
                .verifyComplete();

        // Verificar interacciones
        verify(notificationRepository).save(notification);
        verify(userRepository).findById("user1");
        verify(userRepository).save(user);
    }

    @Test
    void testMarkNotificationAsReadAndDelete_WhenNotificationIsNotRead() {
        // Datos de ejemplo
        String notificationId = "notif1";
        String userId = "user1";

        Notification notification = new Notification(notificationId, userId, "Mark as read and delete", Instant.now(), false);
        User user = new User(userId, "Test User", "test.user@example.com", null, null, null,
                new ArrayList<>(Arrays.asList("Mark as read and delete"))); // Utiliza una lista mutable

        // Configuración de comportamiento de los mocks
        when(notificationRepository.findById(notificationId)).thenReturn(Mono.just(notification));
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(notificationRepository.save(notification)).thenReturn(Mono.just(notification));
        when(userRepository.save(user)).thenReturn(Mono.just(user));
        when(notificationRepository.deleteById(notificationId)).thenReturn(Mono.empty());

        // Llamada al método bajo prueba
        StepVerifier.create(notificationService.markNotificationAsReadAndDelete(notificationId))
                .expectNext("La notificación con ID: '" + notificationId + "' fue marcada como leída y eliminada con éxito.")
                .verifyComplete();

        // Verificar interacciones
        verify(notificationRepository).findById(notificationId);
        verify(notificationRepository).save(notification); // Marcar como leída
        verify(userRepository).findById(userId);
        verify(userRepository).save(user); // Eliminar de las notificaciones del usuario
        verify(notificationRepository).deleteById(notificationId);
    }

    @Test
    void testMarkNotificationAsReadAndDelete_WhenNotificationIsAlreadyRead() {
        // Datos de ejemplo
        String notificationId = "notif2";
        String userId = "user1";

        Notification notification = new Notification(notificationId, userId, "Already read notification", Instant.now(), true);
        User user = new User(userId, "Test User", "test.user@example.com", null, null, null,
                new ArrayList<>(Arrays.asList("Already read notification"))); // Utiliza una lista mutable

        // Configuración de comportamiento de los mocks
        when(notificationRepository.findById(notificationId)).thenReturn(Mono.just(notification));
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(user)).thenReturn(Mono.just(user));
        when(notificationRepository.deleteById(notificationId)).thenReturn(Mono.empty());

        // Llamada al método bajo prueba
        StepVerifier.create(notificationService.markNotificationAsReadAndDelete(notificationId))
                .expectNext("La notificación con ID: '" + notificationId + "' ya estaba marcada como leída y ha sido eliminada.")
                .verifyComplete();

        // Verificar interacciones
        verify(notificationRepository).findById(notificationId);
        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
        verify(notificationRepository).deleteById(notificationId);
    }

    @Test
    void testGetUnreadNotificationStream() {
        // Crear notificaciones
        Notification notification1 = new Notification("1", "user1", "First notification", Instant.now(), false);
        Notification notification2 = new Notification("2", "user2", "Second notification", Instant.now(), false);

        // Emitir notificaciones directamente al Sink para simular el comportamiento real
        notificationService.getNotificationSink().tryEmitNext(notification1);
        notificationService.getNotificationSink().tryEmitNext(notification2);

        // Verificar que el flujo contenga las notificaciones emitidas
        StepVerifier.create(notificationService.getUnreadNotificationStream())
                .expectNext(notification1)
                .expectNext(notification2)
                .thenCancel() // Cancela el flujo para evitar que continúe indefinidamente
                .verify();

        // Verificar que las notificaciones se emitieron correctamente
        verify(notificationRepository, never()).save(any(Notification.class));
    }

}
