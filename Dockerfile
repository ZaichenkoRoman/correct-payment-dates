FROM eclipse-temurin:21.0.5_11-jre-nanoserver-1809
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
