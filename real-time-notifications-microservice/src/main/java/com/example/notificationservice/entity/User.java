package com.example.notificationservice.entity;

import lombok.*;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
@Getter
@Setter

public class User {
    @Id
    private String id;

    private String name;
    private String email;
    private String password;
    private String token;
    private String refreshToken;

    private List<String> notifications = new ArrayList<>();



}
