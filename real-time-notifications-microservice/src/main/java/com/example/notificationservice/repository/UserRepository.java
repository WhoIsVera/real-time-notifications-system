package com.example.notificationservice.repository;

import com.example.notificationservice.entity.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {

        Mono<User> findByEmail(String email);
        Mono<User> findById(String id);
}