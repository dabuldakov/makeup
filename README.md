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

## Конфигурация

Секреты задаются через переменные окружения (`.env` не коммитится):

| Переменная | Назначение | По умолчанию |
|-----------|-----------|--------------|
| `JWT_SECRET` | ключ подписи JWT (обязателен в prod) | есть dev-дефолт только в профиле `dev` |
| `APP_PUBLIC_BASE_URL` | базовый URL для абсолютных ссылок на превью/картинки | `http://localhost:8080`, в prod `http://90.188.89.63:8085` |
| `APP_CORS_ALLOWED_ORIGINS` | список origin-паттернов через запятую | `http://localhost:*` |
| `POSTGRES_*`, `MINIO_*` | доступ к БД и объектному хранилищу | — |

Публичные `GET /api/news/**` и `GET /api/videos/**` не требуют токена, загрузка/изменение — требуют.

## Структура

```
src/test/java/com/example/makeup/                 # юнит-тесты (сервисы, мапперы)
src/integrationTest/java/com/example/makeup/integration/
├── AbstractIntegrationTest.java                  # база: контекст + MockMvc + Testcontainers + очистка БД
├── containers/                                   # фабрика PostgreSQL-контейнера
└── *IT.java                                      # интеграционные тесты
src/integrationTest/resources/application-integration-test.yaml
```
