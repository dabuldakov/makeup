# makeup

Бэкенд приложения (новости и видео): Java 21, Spring Boot 4.0.5, PostgreSQL, MinIO, JWT.

## Требования

- JDK 21
- Docker (нужен для интеграционных тестов — PostgreSQL поднимается в Testcontainers)

Gradle ставить не нужно, используется wrapper — `./gradlew`.

## Виды тестов

| Тип | Где лежат | Суффикс | Что нужно | Команда |
|-----|-----------|---------|-----------|---------|
| Юнит-тесты | `src/test/java` | `*Test` | ничего | `./gradlew test` |
| Интеграционные | `src/integrationTest/java` | `*IT` | Docker | `./gradlew integrationTest` |

Интеграционные тесты поднимают полный Spring-контекст с MockMvc (реальные security-фильтры)
и подключаются к реальному PostgreSQL в контейнере (`postgres:17-alpine`).
MinIO и генерация превью замоканы (`@MockitoBean`), поэтому S3/ffmpeg не нужны.
Профиль `integration-test`, схема создаётся Hibernate (`ddl-auto`), таблицы очищаются между тестами.

## Запуск

```bash
# только юнит-тесты
./gradlew test

# только интеграционные (нужен Docker)
./gradlew integrationTest

# всё вместе
./gradlew test integrationTest
```

Запуск конкретного класса или метода:

```bash
./gradlew test --tests "com.example.makeup.service.NewsServiceTest"
./gradlew test --tests "com.example.makeup.service.NewsServiceTest.*"

# один интеграционный класс
./gradlew integrationTest --tests "com.example.makeup.integration.NewsControllerIT"
```

## Отчёты

- Юнит-тесты: `build/reports/tests/test/index.html`
- Интеграционные: `build/reports/tests/integrationTest/index.html`

## Полезно

```bash
# перезапустить тесты, игнорируя кэш Gradle
./gradlew test integrationTest --rerun-tasks

# очистить результаты перед прогоном
./gradlew cleanTest cleanIntegrationTest test integrationTest
```

Если интеграционные тесты падают на старте — проверьте, что Docker запущен (`docker info`)
и что есть доступ к `postgres:17-alpine` (первый запуск скачивает образ).

## Запуск приложения

```bash
docker-compose up          # PostgreSQL + MinIO
./gradlew bootRun
```

- MinIO WebUI: http://127.0.0.1:9001
- API: http://localhost:8080

## Структура

```
src/test/java/com/example/makeup/                 # юнит-тесты (сервисы, мапперы)
src/integrationTest/java/com/example/makeup/integration/
├── AbstractIntegrationTest.java                  # база: контекст + MockMvc + Testcontainers + очистка БД
├── containers/                                   # фабрика PostgreSQL-контейнера
└── *IT.java                                      # интеграционные тесты
src/integrationTest/resources/application-integration-test.yaml
```
