package com.example.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.notificationservice.entity.Notification;


import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

//        private String id;
        private String userReferenceId;
        private String name;
        private String message;
        private Instant timestamp;
        private boolean read;

        //Este constructor toma una Notification y un nombre de usuario, y asigna los valores correspondientes a los campos del DTO
    public NotificationResponseDto(Notification notification, String name) {
     //   this.id = notification.getId();
        this.userReferenceId = notification.getUserReferenceId();
        this.name = name;
        this.message = notification.getMessage();
        this.timestamp = notification.getTimestamp();
        this.read = notification.isRead();
    }

}
