# Day 15: Quick Start Guide

Быстрое руководство по запуску DevOps MCP сервера.

## Первый запуск

### 1. Соберите DevOps MCP сервер

```bash
./gradlew :mcp-server-devops:jar
```

### 2. Запустите Android эмулятор

```bash
emulator -avd <имя_вашего_AVD> &
```

Или запустите эмулятор через Android Studio.

### 3. Запустите DevOps MCP сервер

**Вариант A - В foreground (для тестирования):**
```bash
./start-devops-mcp.sh
```

**Вариант B - В background (для постоянного использования):**
```bash
./start-devops-mcp-background.sh
```

Для остановки background сервера:
```bash
./stop-devops-mcp.sh
```

### 4. Запустите Ktor сервер

```bash
docker-compose up server
```

### 5. Откройте приложение и протестируйте

Откройте http://localhost:8080 и попросите Claude:

- "Покажи какие Docker контейнеры запущены"
- "Собери Android приложение"
- "Задеплой приложение на эмулятор"

## Ежедневное использование

После первой настройки:

1. **Запустите DevOps MCP сервер в background:**
   ```bash
   ./start-devops-mcp-background.sh
   ```

2. **Запустите эмулятор** (если нужен)

3. **Запустите Docker:**
   ```bash
   docker-compose up
   ```

Сервер DevOps MCP будет работать в background и автоматически переподключаться.

## Проверка статуса

### DevOps MCP сервер работает?
```bash
curl http://localhost:8082/health
# Должен вернуть: OK
```

### Эмулятор запущен?
```bash
adb devices
# Должен показать устройство со статусом "device"
```

### Docker контейнеры работают?
```bash
docker-compose ps
```

## Troubleshooting

### Порт 8082 уже используется
```bash
kill $(lsof -t -i:8082)
```

### DevOps MCP сервер не запускается
Проверьте логи:
```bash
tail -f devops-mcp-server.log
```

### Ktor сервер не может подключиться
1. Убедитесь что DevOps MCP сервер запущен: `curl http://localhost:8082/health`
2. Проверьте логи Ktor сервера: `docker-compose logs server`

## Структура проекта

```
mcp-server/              - Calendar & Reminders (stdio, внутри Docker)
mcp-server-devops/       - DevOps tools (HTTP, на Mac host)
server/                  - Ktor server (внутри Docker)
```

## Полная документация

См. [DAY_15_SETUP.md](DAY_15_SETUP.md) для детальной информации.
