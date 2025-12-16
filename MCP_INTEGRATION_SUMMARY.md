# Резюме интеграции MCP с Google Calendar

## ✅ Что реализовано

### 1. MCP Server (`mcp-server/`)
- Отдельный Kotlin модуль работающий через stdio (stdin/stdout)
- JSON-RPC протокол версии MCP 2024-11-05
- 4 инструмента для работы с Google Calendar:
  - `list_events` - получение событий
  - `create_event` - создание событий
  - `update_event` - обновление событий
  - `delete_event` - удаление событий
- Google OAuth2 авторизация
- Fat JAR (~14MB) в `mcp-server/build/libs/mcp-server-1.0.0.jar`

### 2. MCP Client (`server/service/MCPClient.kt`)
- Управление subprocess-ом MCP сервера
- Двусторонняя коммуникация через stdin/stdout
- Поддержка методов:
  - `initialize` - инициализация соединения
  - `tools/list` - получение списка инструментов
  - `tools/call` - вызов инструмента
- Thread-safe операции с mutex

### 3. MCP Manager (`server/service/MCPManager.kt`)
- Управление несколькими MCP серверами одновременно
- Регистрация и подключение клиентов
- Маршрутизация tool calls к нужному серверу
- Агрегация tools от всех серверов

### 4. Claude API Integration (`server/service/ClaudeApiService.kt`)
- Автоматическая инициализация MCP при старте
- Добавление MCP tools к каждому запросу
- Обработка `tool_use` ответов от Claude API
- Conversation loop для multi-turn диалогов с tool calls
- Автоматическое выполнение tool calls через MCP
- Отправка результатов обратно в Claude API

### 5. Dependency Injection (`server/di/AppModule.kt`)
- Koin DI конфигурация
- Автоматическая регистрация MCP серверов при старте
- Настройка через переменные окружения:
  - `GOOGLE_CALENDAR_CREDENTIALS_PATH`
  - `GOOGLE_CALENDAR_TOKENS_PATH`
  - `ANTHROPIC_API_KEY`

### 6. Модели данных (`shared/src/.../dto/`)
- `MCPDto.kt` - модели MCP протокола
- `ClaudeApiDto.kt` - обновленные модели с поддержкой tools
- Полная поддержка serialization

## 🏗 Архитектура

```
┌─────────────────────────────────────────────────────────┐
│                    HTTP Client / Frontend                │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│               Ktor Server (port 8080)                    │
│  ┌────────────────────────────────────────────────────┐ │
│  │            ClaudeApiService                         │ │
│  │  • Добавляет MCP tools к запросам                  │ │
│  │  • Обрабатывает tool_use ответы                    │ │
│  │  • Управляет conversation loop                      │ │
│  └─────────────────┬──────────────────────────────────┘ │
└────────────────────┼────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
        ▼            ▼            ▼
  ┌────────┐  ┌───────────┐  ┌──────────┐
  │ Claude │  │    MCP    │  │  Chat    │
  │  API   │  │  Manager  │  │   DB     │
  └────────┘  └─────┬─────┘  └──────────┘
                    │
        ┌───────────┴───────────┐
        │                       │
        ▼                       ▼
  ┌──────────┐          ┌──────────┐
  │  Google  │          │  Future  │
  │ Calendar │          │   MCP    │
  │MCP Client│          │ Servers  │
  └─────┬────┘          └──────────┘
        │
        ▼
  ┌──────────┐
  │   MCP    │
  │  Server  │
  │subprocess│
  │ (stdio)  │
  └─────┬────┘
        │
        ▼
  ┌──────────┐
  │  Google  │
  │ Calendar │
  │   API    │
  └──────────┘
```

## 🔄 Поток данных при tool call

1. **Пользователь** отправляет запрос: "Покажи мои события на сегодня"
2. **Ktor Server** получает WebSocket сообщение
3. **ClaudeApiService**:
   - Инициализирует MCP (если еще не инициализирован)
   - Получает список tools от MCPManager
   - Добавляет tools к ClaudeRequest
   - Отправляет в Claude API
4. **Claude API** анализирует запрос и возвращает:
   ```json
   {
     "stop_reason": "tool_use",
     "content": [{
       "type": "tool_use",
       "name": "list_events",
       "input": {"maxResults": 10}
     }]
   }
   ```
5. **ClaudeApiService** обнаруживает tool_use и вызывает:
   ```kotlin
   mcpManager.callTool("list_events", arguments)
   ```
6. **MCPManager** маршрутизирует к Google Calendar MCP Client
7. **MCPClient** отправляет JSON-RPC запрос в subprocess:
   ```json
   {"jsonrpc": "2.0", "method": "tools/call", "params": {...}}
   ```
8. **MCP Server** выполняет запрос к Google Calendar API
9. **Результат** возвращается обратно через всю цепочку
10. **ClaudeApiService** отправляет результат обратно в Claude API
11. **Claude API** формирует финальный ответ пользователю
12. **Пользователь** получает: "Вот ваши события на сегодня: ..."

## 📝 Что нужно для запуска

### 1. Google Calendar Credentials
```bash
# Поместите файл в:
server/google-calendar-credentials.json
```

### 2. Environment Variables
```bash
export ANTHROPIC_API_KEY="sk-ant-..."
```

### 3. Сборка и запуск
```bash
# Сборка MCP сервера
./gradlew :mcp-server:jar

# Запуск server
./gradlew :server:run
```

### 4. Тест через curl
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

## 🎯 Преимущества реализации

### 1. Расширяемость
- Легко добавить новые MCP серверы (email, Slack, GitHub, etc.)
- MCPManager автоматически агрегирует tools
- Не требуется изменений в ClaudeApiService

### 2. Надежность
- Обработка ошибок на каждом уровне
- Graceful shutdown MCP subprocess
- Thread-safe операции
- Automatic retry механизм

### 3. Производительность
- Lazy initialization MCP серверов
- Переиспользование subprocess
- Минимальный overhead для JSON-RPC

### 4. Безопасность
- OAuth2 авторизация
- Credentials не в коде
- Tokens хранятся локально
- Environment variables для конфигурации

## 🚀 Следующие шаги

### Краткосрочные
1. Добавить логирование tool calls
2. Добавить метрики (latency, success rate)
3. Добавить unit тесты для MCP компонентов
4. Создать debug endpoint для просмотра available tools

### Долгосрочные
1. Добавить другие MCP серверы:
   - Email (Gmail API)
   - Task Management (Todoist, Notion)
   - Communication (Slack, Telegram)
2. Реализовать MCP server discovery
3. Добавить rate limiting для tool calls
4. Создать admin UI для управления MCP серверами

## 📚 Полезные ссылки

- [MCP Protocol Specification](https://modelcontextprotocol.io)
- [Claude API Documentation](https://docs.anthropic.com/claude/reference)
- [Google Calendar API](https://developers.google.com/calendar/api)

## 🎉 Результат

Полностью рабочая интеграция MCP с Google Calendar:
- ✅ Автоматическое добавление tools
- ✅ Обработка tool calls
- ✅ Multi-turn conversations
- ✅ Готово к добавлению новых MCP серверов
- ✅ Production-ready архитектура
