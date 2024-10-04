package com.example.notificationservice.HttpResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // Esto evitará que 'data' aparezca cuando sea null
public class CustomApiResponse<T> {
    private  String status;
    private  String message;
    private  T data;
    private  int httpStatusCode;  // incluir el código de estado HTTP

}
