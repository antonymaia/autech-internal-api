FROM eclipse-temurin:11-jre

WORKDIR /app

COPY app.jar app.jar

EXPOSE 5000

ENTRYPOINT ["java", "-jar", "app.jar"]
