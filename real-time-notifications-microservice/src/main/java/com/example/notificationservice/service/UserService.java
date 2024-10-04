package com.example.notificationservice.service;

import com.example.notificationservice.HttpResponse.CustomApiResponse;
import com.example.notificationservice.dto.UserDto;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.User;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.notificationservice.repository.UserRepository;
import com.example.notificationservice.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public UserService(UserRepository userRepository, NotificationRepository notificationRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Flux<User> getAllUsersWithNotificationMessages() {
        return userRepository.findAll()  // Obtener todos los usuarios
                .flatMap(user -> {
                    // Buscar las notificaciones del usuario usando su userReferenceId
                    return notificationRepository.findByUserReferenceId(user.getId())
                            .collectList()  // Recoger las notificaciones en una lista
                            .map(notifications -> {
                                // Convertir las notificaciones a mensajes y asignarlas al usuario
                                List<String> notificationMessages = notifications.stream()
                                        .map(Notification::getMessage)
                                        .collect(Collectors.toList());
                                user.setNotifications(notificationMessages);  // Asignar los mensajes de notificación al usuario
                                return user;
                            });
                });
    }

    // Guardar un usuario y generar token
    public <S extends User> Mono<S> saveUser(S user) {
        if (user.getId() == null || user.getId().isEmpty()) {
            user.setId(UUID.randomUUID().toString().substring(0, 6));  // Generar un ID único para el usuario
        }
        return userRepository.save(user)
                .onErrorResume(e -> Mono.error(new RuntimeException("Error al guardar el usuario: " + e.getMessage())));
    }

    public Mono<ResponseEntity<CustomApiResponse<UserDto>>> authenticateUser(String email, String password) {
        return userRepository.findByEmail(email)
                .flatMap(user -> {
                    if (passwordEncoder.matches(password, user.getPassword())) {
                        // Renovar el token si la contraseña coincide
                        return jwtUtil.validateToken(user.getToken())
                                .flatMap(isValid -> {
                                    if (Boolean.TRUE.equals(isValid)) {
                                        // Si el token es válido, devolver la autenticación exitosa
                                        return buildAuthenticationResponse(user, user.getToken());
                                    } else {
                                        // Si el token no es válido (vencido), renovar el token
                                        return jwtUtil.renewToken(user.getToken())
                                                .flatMap(newToken -> {
                                                    user.setToken(newToken); // Actualizar el token en el usuario
                                                    return userRepository.save(user)
                                                            .flatMap(savedUser -> buildAuthenticationResponse(savedUser, newToken));
                                                });
                                    }
                                });
                    } else {
                        CustomApiResponse<UserDto> errorResponse = new CustomApiResponse<>(
                                "error",
                                "Contraseña incorrecta",
                                null,
                                HttpStatus.UNAUTHORIZED.value()
                        );
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    CustomApiResponse<UserDto> errorResponse = new CustomApiResponse<>(
                            "error",
                            "Usuario no encontrado",
                            null,
                            HttpStatus.NOT_FOUND.value()
                    );
                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse));
                }));
    }

    private Mono<ResponseEntity<CustomApiResponse<UserDto>>> buildAuthenticationResponse(User user, String token) {
        UserDto userDto = new UserDto(
                user.getName(),
                user.getEmail(),
                null, // No incluimos la contraseña por seguridad
                token,
                user.getRefreshToken()

        );
        CustomApiResponse<UserDto> response = new CustomApiResponse<>(
                "success",
                "Autenticación exitosa",
                userDto,
                HttpStatus.OK.value()
        );
        return Mono.just(ResponseEntity.ok(response));
    }


    // Eliminar un usuario por ID
    public Mono<String> deleteUserById(String id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado con ID: " + id)))
                .flatMap(user -> userRepository.deleteById(id)
                        .thenReturn("Usuario con ID: " + id + " ha sido eliminado con éxito.")
                );
    }
}
