package com.example.notificationservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "jwtSecret")
public class JwtSecret {

    @Id
    private String id;
    private String secret;

    public JwtSecret(String secret) {
        this.secret = secret;
    }
}
