# Google Calendar MCP - Quick Start

## Минимальная настройка для запуска

### 1. Получите Google Calendar credentials

1. Перейдите в [Google Cloud Console](https://console.cloud.google.com/)
2. Создайте проект
3. Включите Google Calendar API
4. Настройте OAuth Consent Screen (добавьте себя как test user)
5. Создайте OAuth 2.0 Client ID (Desktop app)
6. Скачайте JSON

### 2. Поместите credentials

Поместите скачанный JSON файл в один из этих путей:
- `server/google-calendar-credentials.json` (рекомендуется)
- `~/google-calendar-credentials.json`
- `./google-calendar-credentials.json`

### 3. Установите API ключ

```bash
export ANTHROPIC_API_KEY="sk-ant-..."
```

### 4. Соберите и запустите

```bash
# Сборка
./gradlew build

# Запуск
./gradlew :server:run
```

### 5. Проверьте логи

При старте вы должны увидеть:
```
Current working directory: /path/to/project
Found Google Calendar credentials at: /path/to/credentials.json
Found MCP server JAR at: /path/to/mcp-server-1.0.0.jar
Registered Google Calendar MCP client
```

### 6. Тестируйте

```bash
curl -X POST http://localhost:8080/chat/send \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [{
      "role": "user",
      "content": "Покажи мои события на сегодня"
    }]
  }'
```

## Troubleshooting

### Credentials не найдены?

Проверьте вывод:
```
WARNING: Google Calendar credentials not found. Tried:
  - /app/server/google-calendar-credentials.json
  - ~/google-calendar-credentials.json
  - ...
```

**Решение:** Поместите credentials по одному из указанных путей.

### JAR не найден?

```bash
./gradlew :mcp-server:jar
# Проверьте что файл создался
ls -l mcp-server/build/libs/mcp-server-1.0.0.jar
```

### ANTHROPIC_API_KEY не установлен?

```bash
# MacOS/Linux
export ANTHROPIC_API_KEY="sk-ant-..."

# Windows PowerShell
$env:ANTHROPIC_API_KEY="sk-ant-..."
```

## Полная документация

- [MCP_SETUP_GUIDE.md](./MCP_SETUP_GUIDE.md) - детальная инструкция
- [MCP_INTEGRATION_SUMMARY.md](./MCP_INTEGRATION_SUMMARY.md) - техническое описание

## Доступные calendar tools

После запуска Claude API автоматически получит доступ к:
- `list_events` - получение событий
- `create_event` - создание событий
- `update_event` - обновление событий
- `delete_event` - удаление событий

Вы можете просто спросить Claude на естественном языке, например:
- "Покажи мои события на сегодня"
- "Создай встречу завтра в 15:00"
- "Какие у меня встречи на следующей неделе?"
