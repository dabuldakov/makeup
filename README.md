# makeup

Бэкенд приложения (новости и видео): Java 21, Spring Boot 4.0.5, PostgreSQL, MinIO, JWT.

## Архитектура

Stateless модульный монолит с проверяемыми ArchUnit-границами
(`ModuleBoundariesTest`). Модули:

```
config/util/exception   ← база
auth                    ← пользователи, JWT; базовый модуль, не зависит от feature-модулей
security                ← адаптер безопасности над auth
media                   ← объектное хранилище (MinIO) и очередь задач
news → auth, media, video
video → auth, media
account → auth, news, video, media   ← листовой оркестратор удаления аккаунта
```

Граф модулей ацикличен, что проверяет ArchUnit (`ModuleBoundariesTest`).
Две потенциальные петли разорваны:

- **`video → news`** — ссылки на удаляемое видео снимаются событием
  `VideoDeletedEvent` (слушатель `NewsService.onVideoDeleted`), а не прямым
  вызовом `NewsRepository` из video;
- **`media → video`** — очередь задач работает через порт `MediaJobHandler`;
  генерация превью реализована в `video` (`VideoThumbnailJobHandler`), поэтому
  media ничего не знает о `Video`/`VideoRepository`.

Удаление аккаунта вынесено из `auth.UserService` в отдельный модуль `account`
(`AccountDeletionService` + `AccountController`): это оркестрация данных
нескольких bounded context, а не часть профиля.

## Горизонтальное масштабирование

Несколько нод работают с **одной общей БД** и общим MinIO. Приложение stateless
(JWT, файлы в MinIO), sticky sessions не нужны. Что учитывается:

- **Кеш пользователей** (`CustomUserDetailsService`) — по умолчанию локальный
  Caffeine. При нескольких нодах включается общий **Redis**
  (`app.cache.redis.enabled=true`, `REDIS_HOST`/`REDIS_PORT`): иначе инвалидация
  при смене профиля/роли не видна другим инстансам.
- **Фоновые задачи** (`MediaJobScheduler`) уже безопасны для нескольких воркеров:
  задачи берутся из `media_jobs` через `FOR UPDATE SKIP LOCKED`. Ноду можно
  вывести из обработки, выставив `media.jobs.scheduler.enabled=false`.
- **Flyway** берёт advisory-lock и накатывает миграции один раз; для лишних нод
  можно выставить `SPRING_FLYWAY_ENABLED=false`.
- **Пул БД** (`hikari.maximum-pool-size`) умножается на число нод; при росте —
  PgBouncer и/или read-реплики для чтения лент.

`docker-compose.yaml` готов к масштабированию: `app` не публикует порт, наружу
смотрит `nginx` (порт `8085`) и балансирует реплики (понимает большие загрузки
видео и Range-стриминг).

```bash
docker compose up -d --scale app=3      # 3 реплики
# или постоянно: APP_REPLICAS=3 в .env
```

Одна общая БД — это масштабирование. Своя БД у каждого — это отдельный
независимый инстанс.

## Требования

- JDK 21
- Docker (нужен для интеграционных тестов — PostgreSQL поднимается в Testcontainers)

Gradle ставить не нужно, используется wrapper — `./gradlew`.

## Виды тестов

| Тип | Где лежат | Суффикс | Что нужно | Команда |
|-----|-----------|---------|-----------|---------|
| Юнит-тесты | `src/test/java` | `*Test` | ничего | `./gradlew test` |
| Архитектурные | `src/test/java` (`architecture/`) | `*Test` | ничего | `./gradlew test` |
| Интеграционные | `src/integrationTest/java` | `*IT` | Docker | `./gradlew integrationTest` |
| Гейт покрытия | — | — | Docker | `./gradlew jacocoTestCoverageVerification` |

Покрытие строк — не ниже 70% (JaCoCo), отчёт объединяет unit- и
integration-тесты.

Интеграционные тесты поднимают полный Spring-контекст с MockMvc (реальные security-фильтры)
и подключаются к реальному PostgreSQL в контейнере (`postgres:17-alpine`).
MinIO и генерация превью замоканы (`@MockitoBean`), поэтому S3/ffmpeg не нужны.
Профиль `integration-test`, схема создаётся Flyway-миграциями (`ddl-auto=none`), таблицы очищаются между тестами.

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
| `APP_CACHE_REDIS_ENABLED` | общий Redis-кеш вместо локального Caffeine | `false` (в `prod` — `true`) |
| `REDIS_HOST` / `REDIS_PORT` | адрес Redis | `redis:6379` |
| `APP_REPLICAS` | число реплик `app` в compose | `1` |

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

## Автодеплой

При push в `main` GitHub Actions по SSH заходит на VPS, обновляет чекаут и пересобирает
только сервис `app` (PostgreSQL и MinIO не трогаются):

```bash
cd "$DEPLOY_PATH"                      # например, /opt/makeup
git fetch --prune origin main && git reset --hard origin/main
docker compose up -d --build app
```

Workflow — `.github/workflows/deploy.yml` (можно запустить вручную: Actions → deploy → Run workflow).

Секреты репозитория (Settings → Secrets and variables → Actions):

| Секрет | Назначение |
|--------|-----------|
| `DEPLOY_HOST` | адрес VPS |
| `DEPLOY_USER` | SSH-пользователь |
| `DEPLOY_SSH_KEY` | приватный SSH-ключ без пароля |
| `DEPLOY_PATH` | каталог чекаута на сервере, например `/opt/makeup` |

Первичная настройка сервера:

```bash
git clone git@github.com:dabuldakov/makeup.git /opt/makeup
cd /opt/makeup
# создать .env с секретами (в git не коммитится), затем:
docker compose up -d
```

Дальнейшие деплои идут автоматически при push в `main`.
