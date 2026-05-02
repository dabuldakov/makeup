# Build stage
FROM gradle:9.4-jdk21 AS build

# Устанавливаем FFmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/* && \
    ffmpeg -version

WORKDIR /app
# Копируем весь проект
COPY . .

RUN gradle build --no-daemon

# Runtime stage
FROM amazoncorretto:21-alpine

# Устанавливаем FFmpeg в runtime stage (критически важно!)
RUN apk add --no-cache ffmpeg

WORKDIR /app
# Копируем собранный JAR-файл
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]