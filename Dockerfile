FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN mkdir -p /app/uploads/menu-images

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod
ENV APP_UPLOAD_MENU_IMAGE_DIR=/app/uploads/menu-images

ENTRYPOINT ["java", "-jar", "app.jar"]
