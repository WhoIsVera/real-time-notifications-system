    package com.example.notificationservice.controller;

    import com.example.notificationservice.HttpResponse.CustomApiResponse;
    import com.example.notificationservice.HttpResponse.ResponseUtil;
    import com.example.notificationservice.dto.UserDto;
    import com.example.notificationservice.entity.User;
    import com.example.notificationservice.repository.UserRepository;
    import com.example.notificationservice.security.JwtUtil;
    import com.example.notificationservice.service.NotificationService;
    import com.example.notificationservice.service.UserService;
    import io.swagger.v3.oas.annotations.Operation;
    import io.swagger.v3.oas.annotations.tags.Tag;
    import io.swagger.v3.oas.annotations.responses.ApiResponses;
    import io.swagger.v3.oas.annotations.responses.ApiResponse;
    import io.swagger.v3.oas.annotations.media.Content;
    import io.swagger.v3.oas.annotations.media.Schema;
    import jakarta.validation.Valid;
    import org.slf4j.Logger;
    import org.springframework.http.HttpHeaders;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.web.bind.annotation.*;
    import reactor.core.publisher.Mono;
    import java.util.List;


    @RestController
    @RequestMapping("/api-clients/v1.0/users")
    @Tag(name = "Users", description = "Operations related to User management in the notification system")
    public class UserController {
        private final UserService userService;
        private final UserRepository userRepository;
        private final NotificationService notificationService;
        private final JwtUtil jwtUtil;
        private final PasswordEncoder passwordEncoder;
        private static final Logger log = org.slf4j.LoggerFactory.getLogger(UserController.class);

        public UserController(UserService userService, UserRepository userRepository, NotificationService notificationService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
            this.userService = userService;
            this.userRepository = userRepository;
            this.notificationService = notificationService;
            this.jwtUtil = jwtUtil;
            this.passwordEncoder = passwordEncoder;
        }

                @Operation(summary = "Get a user with their notifications", description = "Retrieve a user along with their notifications and refresh token")
                @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved user",
                                content = @Content(schema = @Schema(implementation = UserDto.class))),
                        @ApiResponse(responseCode = "404", description = "User not found",
                                content = @Content(schema = @Schema(implementation = CustomApiResponse.class)))
                })
                @GetMapping("/{userId}")
                public Mono<ResponseEntity<CustomApiResponse<UserDto>>> getUserById(@PathVariable String userId) {
                    return userRepository.findById(userId)
                            .flatMap(user -> {
                                UserDto userDto = new UserDto(
                                        user.getName(),
                                        user.getEmail(),
                                        null, // No incluimos la contraseña por seguridad
                                        null, // No incluimos el token por seguridad
                                        user.getRefreshToken()  // Incluimos el refresh token
                                );
                                CustomApiResponse<UserDto> response = new CustomApiResponse<>(
                                        "success",
                                        "Usuario encontrado con éxito",
                                        userDto,
                                        HttpStatus.OK.value()
                                );
                                return Mono.just(ResponseEntity.ok(response));
                            })
                            .switchIfEmpty(Mono.just(
                                    ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                            new CustomApiResponse<>(
                                                    "error",
                                                    "Usuario no encontrado con ID: " + userId,
                                                    null,
                                                    HttpStatus.NOT_FOUND.value()
                                            )
                                    )
                            ));
                }

                            @Operation(summary = "Refresh a user's token", description = "Generates a new token using the expired token and user ID")
                            @ApiResponses(value = {
                                    @ApiResponse(responseCode = "200", description = "Token successfully refreshed",
                                            content = @Content(schema = @Schema(implementation = UserDto.class))),
                                    @ApiResponse(responseCode = "401", description = "Unauthorized - Token is invalid or user not found"),
                                    @ApiResponse(responseCode = "404", description = "User not found")
                            })
                            @PostMapping("/refresh-token/{userId}")
                            public Mono<ResponseEntity<CustomApiResponse<UserDto>>> refreshToken(@PathVariable String userId, @RequestHeader(HttpHeaders.AUTHORIZATION) String expiredToken) {
                                String tokenToUse = null;
                                if (expiredToken != null && expiredToken.startsWith("Bearer ")) {
                                    tokenToUse = expiredToken.substring(7);
                                } else {
                                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                                            new CustomApiResponse<>(
                                                    "error",
                                                    "El token no tiene el formato correcto. Asegúrate de incluir 'Bearer ' seguido del token.",
                                                    null,
                                                    HttpStatus.BAD_REQUEST.value()
                                            )
                                    ));
                                }

                                final String finalToken = tokenToUse;

                                return userRepository.findById(userId)
                                        .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado con ID: " + userId)))
                                        .flatMap(user -> {
                                            // Intentar renovar el token
                                            return jwtUtil.renewToken(finalToken)
                                                    .flatMap(newToken -> {
                                                        // Si se renueva, actualizar el campo refreshToken del usuario
                                                        user.setRefreshToken(newToken);
                                                        return userRepository.save(user)
                                                                .flatMap(savedUser -> {
                                                                    UserDto userDto = new UserDto(
                                                                            savedUser.getName(),
                                                                            savedUser.getEmail(),
                                                                            null, // No incluimos la contraseña por seguridad
                                                                            savedUser.getToken(), // Mantener el token original
                                                                            newToken // Nuevo refresh token
                                                                    );
                                                                    CustomApiResponse<UserDto> response = new CustomApiResponse<>(
                                                                            "success",
                                                                            "Token renovado con éxito",
                                                                            userDto,
                                                                            HttpStatus.OK.value()
                                                                    );
                                                                    return Mono.just(ResponseEntity.ok(response));
                                                                })
                                                                .onErrorResume(e -> {
                                                                    // Error al guardar el usuario con el nuevo refresh token
                                                                    String message = "Error al guardar el usuario con el nuevo refresh token: " + e.getMessage();
                                                                    log.error(message);
                                                                    CustomApiResponse<UserDto> errorResponse = new CustomApiResponse<>(
                                                                            "error",
                                                                            message,
                                                                            null,
                                                                            HttpStatus.INTERNAL_SERVER_ERROR.value()
                                                                    );
                                                                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                                                                });
                                                    })
                                                    .onErrorResume(e -> {
                                                        // Error al renovar el token (puede ser porque no se puede acceder a los claims o es un token inválido)
                                                        String message = "Error al renovar el token: " + e.getMessage();
                                                        log.error(message);
                                                        CustomApiResponse<UserDto> errorResponse = new CustomApiResponse<>(
                                                                "error",
                                                                message,
                                                                null,
                                                                HttpStatus.UNAUTHORIZED.value()
                                                        );
                                                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
                                                    });
                                        })
                                        .onErrorResume(e -> {
                                            // Manejar cualquier otro error inesperado
                                            String message = "Error inesperado durante la renovación del token: " + e.getMessage();
                                            log.error(message);
                                            CustomApiResponse<UserDto> errorResponse = new CustomApiResponse<>(
                                                    "error",
                                                    message,
                                                    null,
                                                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                                            );
                                            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                                        });
                            }
                                                    @Operation(summary = "Get all users with their notifications", description = "Retrieve a list of users along with their notifications")
                                                    @ApiResponses(value = {
                                                            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of users",
                                                                    content = @Content(schema = @Schema(implementation = User.class))),
                                                            @ApiResponse(responseCode = "404", description = "No users found")
                                                    })
                                                    @GetMapping
                                                    public Mono<ResponseEntity<CustomApiResponse<List<User>>>> getAllUsers() {
                                                        return userService.getAllUsersWithNotificationMessages()  // Llama al servicio para obtener los usuarios con notificaciones
                                                                .collectList()
                                                                .flatMap(users -> {
                                                                    if (users.isEmpty()) {
                                                                        return ResponseUtil.createErrorResponse("No se encontraron usuarios.", HttpStatus.NOT_FOUND);
                                                                    }
                                                                    return ResponseUtil.createSuccessResponse("Usuarios encontrados", users);
                                                                });
                                                    }

                                                                    @Operation(summary = "Save a new user", description = "Create and save a new user in the system")
                                                                    @ApiResponses(value = {
                                                                            @ApiResponse(responseCode = "201", description = "User successfully saved",
                                                                                    content = @Content(schema = @Schema(implementation = User.class))),
                                                                            @ApiResponse(responseCode = "500", description = "Error saving the user")
                                                                    })
                                                                    @PostMapping
                                                                    public Mono<ResponseEntity<CustomApiResponse<User>>> saveUser(@Valid @RequestBody UserDto userDto) {
                                                                        User user = new User();
                                                                        user.setName(userDto.getName());
                                                                        user.setEmail(userDto.getEmail());
                                                                        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

                                                                        return userService.saveUser(user)
                                                                                .flatMap(savedUser -> jwtUtil.generateToken(savedUser.getEmail())
                                                                                        .flatMap(token -> {
                                                                                            savedUser.setToken(token); // Establecemos el token en el usuario

                                                                                            // Guardar nuevamente el usuario actualizado con el token
                                                                                            return userService.saveUser(savedUser)
                                                                                                    .flatMap(updatedUser -> {
                                                                                                        CustomApiResponse<User> response = new CustomApiResponse<>(
                                                                                                                "success",
                                                                                                                "Usuario guardado con éxito",
                                                                                                                updatedUser,
                                                                                                                HttpStatus.OK.value()
                                                                                                        );
                                                                                                        return Mono.just(ResponseEntity.ok(response));
                                                                                                    });
                                                                                        }))
                                                                                .onErrorResume(e -> {
                                                                                    CustomApiResponse<User> errorResponse = new CustomApiResponse<>(
                                                                                            "error",
                                                                                            "Error al guardar el usuario: " + e.getMessage(),
                                                                                            null,  // Mantener `User` como tipo, con `data` igual a `null`
                                                                                            HttpStatus.INTERNAL_SERVER_ERROR.value()
                                                                                    );
                                                                                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                                                                                });
                                                                    }

                                                                                    @Operation(summary = "Delete a user by ID", description = "Delete a user from the system by their ID")
                                                                                    @ApiResponses(value = {
                                                                                            @ApiResponse(responseCode = "200", description = "User successfully deleted"),
                                                                                            @ApiResponse(responseCode = "404", description = "User not found")
                                                                                    })
                                                                                    @DeleteMapping("/{id}")
                                                                                    public Mono<ResponseEntity<CustomApiResponse<Void>>> deleteUser(@PathVariable String id) {
                                                                                        return userService.deleteUserById(id)
                                                                                                .flatMap(successMessage -> ResponseUtil.createSuccessResponse(successMessage, (Void) null))
                                                                                                .onErrorResume(e -> ResponseUtil.createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND));
                                                                                    }
    }