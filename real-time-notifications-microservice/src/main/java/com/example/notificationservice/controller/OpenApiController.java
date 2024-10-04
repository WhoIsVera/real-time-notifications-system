package com.example.notificationservice.controller;


import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenApiController {
    // Para corroborar  edpoint de la url del yaml
    @GetMapping(value = "/openapi.yaml", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getOpenApiYml() {
        Resource resource = new ClassPathResource("static/openapi.yaml");
        return ResponseEntity.ok().body(resource);

    }
}