# Build stage
FROM gradle:8.5-jdk21 AS build

WORKDIR /app
# Копируем весь проект
COPY . .

# Даем права на выполнение gradlew (если используете wrapper)
RUN chmod +x gradlew
# Собираем проект
RUN ./gradlew build --no-daemon

# Runtime stage
FROM amazoncorretto:21-alpine

WORKDIR /app
# Копируем собранный JAR-файл
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]