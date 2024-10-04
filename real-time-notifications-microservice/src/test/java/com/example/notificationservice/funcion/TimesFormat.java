package com.example.notificationservice.funcion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimesFormat {

    OffsetDateTime fechaMongDB = OffsetDateTime.now(ZoneOffset.UTC);
    DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
 //   DateTimeFormatter formateador = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");


    Instant fechFor = Instant.parse(Instant.now().toString().substring(0,29));

     String timeChar = fechFor.toString().substring(0,29);




    // Crear un objeto LocalDateTime con la fecha y hora actuales
    LocalDateTime fechaHoraActual = LocalDateTime.now();

    // Definir el formato deseado
/*    DateTimeFormatter formateador = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    // Formatear la fecha y hora
    String fechaHoraFormateada = fechaHoraActual.format(formateador);

    // Parsear la cadena a TemporalAccessor
    TemporalAccessor temporalAccessor = formateador.parse(fechaHoraFormateada);

    // Convertir TemporalAccessor a Instant
    Instant timestamp = Instant.from(temporalAccessor); */

}
