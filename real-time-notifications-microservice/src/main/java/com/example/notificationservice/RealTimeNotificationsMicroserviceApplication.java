package com.example.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling //habilita la funcionalidad de las tareas programadas
public class RealTimeNotificationsMicroserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RealTimeNotificationsMicroserviceApplication.class, args);
	}

}
