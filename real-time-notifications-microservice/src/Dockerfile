# Usar una imagen ligera de OpenJDK 17
FROM openjdk:17-jdk-slim

# Crear un volumen temporal para guardar logs u otros datos
VOLUME /tmp

# Copiar el archivo JAR generado al contenedor
COPY target/*.jar app.jar


# Establecer el comando de inicio del contenedor
ENTRYPOINT ["java", "-jar", "/app.jar"]
