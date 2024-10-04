package com.example.notificationservice.repository;

import com.example.notificationservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

@DataMongoTest // Configura automáticamente los repositorios de MongoDB para los tests
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository; // Bean inyectado

    @BeforeEach
    void setUp() {
        // Limpiar la base de datos antes de cada prueba para evitar conflictos de datos
        userRepository.deleteAll().block();
    }

    @Test
    void testSaveAndFindUser() {
        // Crear usuario con propiedades y un array de notificaciones
        List<String> notifications = Arrays.asList(
                "Tu pedido sera entregado en 12hrs",
                "Tu pedido sera entregado en 1hr"
        );
        User user = new User();
        user.setId("14dadd");
        user.setName("Juan Spert");
        user.setEmail("juan.spert@example.com");
        user.setPassword("securepassword");
        user.setToken("someJwtToken");
        user.setRefreshToken("someRefreshToken");
        user.setNotifications(notifications);

        // Guardar el usuario
        Mono<User> userMono = userRepository.save(user);

        // Verificar que el usuario fue guardado correctamente
        StepVerifier.create(userMono)
                .assertNext(savedUser -> {
                    assert savedUser.getId().equals("14dadd");
                    assert savedUser.getName().equals("Juan Spert");
                    assert savedUser.getEmail().equals("juan.spert@example.com");
                    assert savedUser.getPassword().equals("securepassword");
                    assert savedUser.getToken().equals("someJwtToken");
                    assert savedUser.getRefreshToken().equals("someRefreshToken");
                    assert savedUser.getNotifications().size() == 2;
                })
                .verifyComplete();

        // Buscar el usuario por su ID y verificar sus notificaciones
        Mono<User> foundUserMono = userRepository.findById("14dadd");

        StepVerifier.create(foundUserMono)
                .assertNext(foundUser -> {
                    assert foundUser.getName().equals("Juan Spert");
                    assert foundUser.getNotifications().contains("Tu pedido sera entregado en 12hrs");
                    assert foundUser.getNotifications().contains("Tu pedido sera entregado en 1hr");
                })
                .verifyComplete();
    }

    @Test
    void testFindByEmail() {
        // Crear y guardar un usuario
        User user = new User();
        user.setId("14dadd");
        user.setName("Juan Spert");
        user.setEmail("juan.spert@example.com");
        user.setPassword("securepassword");
        user.setToken("someJwtToken");
        user.setRefreshToken("someRefreshToken");
        user.setNotifications(Arrays.asList("Notification 1", "Notification 2"));

        userRepository.save(user).block();

        // Buscar el usuario por su email
        Mono<User> foundUserMono = userRepository.findByEmail("juan.spert@example.com");

        // Verificar que el usuario fue encontrado correctamente
        StepVerifier.create(foundUserMono)
                .assertNext(foundUser -> {
                    assert foundUser.getId().equals("14dadd");
                    assert foundUser.getName().equals("Juan Spert");
                    assert foundUser.getEmail().equals("juan.spert@example.com");
                    assert foundUser.getPassword().equals("securepassword");
                    assert foundUser.getToken().equals("someJwtToken");
                    assert foundUser.getRefreshToken().equals("someRefreshToken");
                    assert foundUser.getNotifications().size() == 2;
                })
                .verifyComplete();
    }

    @Test
    void testFindById_UserNotFound() {
        // Intentar buscar un usuario que no existe
        Mono<User> foundUserMono = userRepository.findById("nonExistingId");

        // Verificar que no se encontró ningún usuario
        StepVerifier.create(foundUserMono)
                .expectNextCount(0)
                .verifyComplete();
    }
}
