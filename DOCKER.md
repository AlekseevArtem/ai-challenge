# Docker Deployment Guide

Это руководство описывает, как запустить веб-версию приложения с помощью Docker.

## 📋 Требования

- Docker Engine 20.10+
- Docker Compose 2.0+
- Минимум 4GB RAM для сборки
- ~2GB свободного места на диске

## 🚀 Быстрый старт

### Kotlin/Wasm

**Самый современный и быстрый вариант**

```bash
# Собрать и запустить
docker-compose up --build

# Или в фоновом режиме
docker-compose up -d --build
```

Приложение будет доступно по адресу: **http://localhost:8080**


## 📦 Что входит в конфигурацию

### Dockerfile (Wasm)
- **Base image**: gradle:8.10-jdk17 для сборки
- **Web server**: nginx:1.25-alpine
- **Build target**: wasmJsBrowserDistribution
- **Multi-stage build**: оптимизированный финальный образ ~50MB

### Dockerfile.js (JS Fallback)
- **Base image**: gradle:8.10-jdk17 для сборки
- **Web server**: nginx:1.25-alpine
- **Build target**: jsBrowserDistribution
- **Совместимость**: работает в старых браузерах

### nginx.conf
- ✅ Правильный MIME type для WASM файлов
- ✅ Gzip компрессия
- ✅ Security headers
- ✅ CORS настройка
- ✅ SPA routing (все роуты идут на index.html)
- ✅ Кеширование статических ресурсов
- ✅ Health check endpoint на /health

## 🔧 Управление контейнерами

### Просмотр логов
```bash
# Все логи
docker-compose logs -f

# Только последние 100 строк
docker-compose logs --tail=100 -f
```

### Проверка статуса
```bash
# Статус контейнеров
docker-compose ps

# Health check
curl http://localhost:8080/health
```

### Остановка
```bash
# Остановить (сохранить контейнеры)
docker-compose stop

# Остановить и удалить контейнеры
docker-compose down

# Остановить и удалить всё (включая образы)
docker-compose down --rmi all
```

### Перезапуск
```bash
# Перезапустить без пересборки
docker-compose restart

# Полная пересборка
docker-compose up --build --force-recreate
```

## 🎯 Продакшн деплой

### 1. Сборка production образа
```bash
# Build
docker build -t myapplication-web:1.0.0 .

# Tag for registry
docker tag myapplication-web:1.0.0 your-registry.com/myapplication-web:1.0.0

# Push
docker push your-registry.com/myapplication-web:1.0.0
```

### 2. Запуск в продакшне
```bash
# С использованием docker-compose
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Или напрямую
docker run -d \
  --name myapplication-web \
  --restart=always \
  -p 80:80 \
  your-registry.com/myapplication-web:1.0.0
```

### 3. Рекомендации для production

#### HTTPS (с Let's Encrypt)
Создайте `docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  web:
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.web.rule=Host(`your-domain.com`)"
      - "traefik.http.routers.web.tls=true"
      - "traefik.http.routers.web.tls.certresolver=letsencrypt"

  traefik:
    image: traefik:v2.10
    command:
      - "--api.insecure=true"
      - "--providers.docker=true"
      - "--entrypoints.web.address=:80"
      - "--entrypoints.websecure.address=:443"
      - "--certificatesresolvers.letsencrypt.acme.email=your@email.com"
      - "--certificatesresolvers.letsencrypt.acme.storage=/letsencrypt/acme.json"
      - "--certificatesresolvers.letsencrypt.acme.httpchallenge.entrypoint=web"
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - "/var/run/docker.sock:/var/run/docker.sock:ro"
      - "./letsencrypt:/letsencrypt"
```

#### Environment Variables
Создайте `.env` файл:

```env
# App Configuration
APP_VERSION=1.0.0
NGINX_PORT=80

# Resource Limits
MEMORY_LIMIT=512M
CPU_LIMIT=1
```

Обновите `docker-compose.yml`:

```yaml
services:
  web:
    deploy:
      resources:
        limits:
          cpus: '${CPU_LIMIT}'
          memory: ${MEMORY_LIMIT}
    environment:
      - APP_VERSION=${APP_VERSION}
```

## 🐛 Отладка

### Зайти внутрь контейнера
```bash
docker-compose exec web sh
```

### Проверить содержимое собранных файлов
```bash
# Список файлов
docker-compose exec web ls -la /usr/share/nginx/html/

# Содержимое index.html
docker-compose exec web cat /usr/share/nginx/html/index.html
```

### Проверить nginx конфигурацию
```bash
docker-compose exec web nginx -t
```

### Проверить логи nginx
```bash
docker-compose exec web cat /var/log/nginx/error.log
docker-compose exec web cat /var/log/nginx/access.log
```

### Локальная сборка без Docker Compose
```bash
# Сборка образа
docker build -t myapp-test .

# Запуск
docker run -p 8080:80 myapp-test

# Проверка
curl http://localhost:8080
```

## 📊 Оптимизация

### Кеширование слоев сборки

Docker автоматически кеширует слои. Для максимальной эффективности:

1. Gradle dependencies скачиваются отдельным слоем
2. Исходный код копируется после зависимостей
3. Используется multi-stage build

### Уменьшение размера образа

Текущий размер финального образа: **~50-80MB**

Если нужно еще меньше:
```dockerfile
# Используйте nginx:alpine-slim
FROM nginx:alpine-slim

# Удалите ненужные файлы после сборки
RUN rm -rf /usr/share/nginx/html/*.map
```

### BuildKit для быстрой сборки
```bash
# Включить BuildKit
export DOCKER_BUILDKIT=1

# Или добавить в docker-compose.yml
COMPOSE_DOCKER_CLI_BUILD=1 DOCKER_BUILDKIT=1 docker-compose up --build
```

## 🔒 Безопасность

### 1. Сканирование уязвимостей
```bash
# С помощью Trivy
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
  aquasec/trivy image myapplication-web:latest

# С помощью Docker Scout
docker scout cves myapplication-web:latest
```

### 2. Запуск от непривилегированного пользователя

Обновите Dockerfile:
```dockerfile
# Создать пользователя nginx
RUN addgroup -g 101 -S nginx && \
    adduser -S -D -H -u 101 -h /var/cache/nginx -s /sbin/nologin -G nginx -g nginx nginx

USER nginx
```

### 3. Read-only файловая система
```yaml
services:
  web:
    read_only: true
    tmpfs:
      - /var/run
      - /var/cache/nginx
      - /tmp
```

## 🌐 Сравнение Wasm vs JS

| Характеристика | Wasm (по умолчанию) | JS (fallback) |
|---------------|---------------------|---------------|
| **Скорость** | ⚡ Быстрее в 2-3 раза | 🐌 Медленнее |
| **Размер бандла** | 📦 Меньше (~30%) | 📦 Больше |
| **Совместимость** | 🌐 Chrome 91+, Firefox 89+, Safari 15+ | 🌐 Все браузеры |
| **Загрузка** | ⏱️ Быстрее парсинг | ⏱️ Медленнее |
| **Рекомендуется для** | Современные браузеры | Максимальная совместимость |

## 📝 Примеры использования

### CI/CD в GitHub Actions
```yaml
name: Build and Push Docker Image

on:
  push:
    branches: [main]

jobs:
  docker:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: your-registry/myapplication-web:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

### Docker Swarm
```bash
docker stack deploy -c docker-compose.yml myapp
```

### Kubernetes
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapplication-web
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: web
        image: your-registry.com/myapplication-web:1.0.0
        ports:
        - containerPort: 80
        resources:
          limits:
            memory: "512Mi"
            cpu: "1"
```

## ❓ FAQ

**Q: Почему сборка долгая?**
A: Gradle скачивает все зависимости. Используйте кеш слоев Docker.

**Q: Приложение не отвечает**
A: Проверьте `docker-compose logs -f` и health check endpoint.

**Q: Нужно ли собирать заново при изменении кода?**
A: Да, запустите `docker-compose up --build`.

**Q: Как изменить порт?**
A: Измените `8080:80` на нужный в docker-compose.yml.

**Q: Wasm не работает в браузере**
A: Используйте `docker-compose.js.yml` для JS версии.

## 📚 Полезные ссылки

- [Docker Documentation](https://docs.docker.com/)
- [Nginx Documentation](https://nginx.org/en/docs/)
- [Kotlin/Wasm](https://kotlinlang.org/docs/wasm-overview.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)

## 🆘 Поддержка

При возникновении проблем:
1. Проверьте логи: `docker-compose logs -f`
2. Проверьте health: `curl http://localhost:8080/health`
3. Создайте issue с полным выводом логов