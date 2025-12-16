# Руководство по интеграции Google Calendar MCP Server

## Что было реализовано

Полная интеграция MCP (Model Context Protocol) для работы с Google Calendar в вашем приложении:

### Архитектура:
1. **MCP Server** (`mcp-server/`) - отдельный процесс, работающий через stdio
2. **MCP Client** (`server/`) - клиент для взаимодействия с MCP серверами
3. **MCP Manager** (`server/`) - менеджер для управления несколькими MCP серверами
4. **Claude API Integration** - автоматическое добавление tools и обработка tool calls

### Возможности Calendar MCP:
- **list_events** - получение списка событий из календаря
- **create_event** - создание новых событий
- **update_event** - обновление существующих событий
- **delete_event** - удаление событий

### Технологии:
- Kotlin (интегрировано в ваш KMP проект)
- Google Calendar API с OAuth2
- MCP Protocol (JSON-RPC через stdio)
- Fat JAR с всеми зависимостями
- Ktor Server для HTTP endpoints

## Настройка

### 1. Настройка Google Cloud Console (ОБЯЗАТЕЛЬНО)

1. Перейдите в [Google Cloud Console](https://console.cloud.google.com/)
2. Создайте проект (или используйте существующий)
3. Включите **Google Calendar API**:
   - APIs & Services → Library → найдите "Google Calendar API" → Enable
4. Настройте **OAuth Consent Screen**:
   - APIs & Services → OAuth consent screen
   - User Type: External
   - Заполните название приложения и email
   - Добавьте scope: `https://www.googleapis.com/auth/calendar`
   - Добавьте себя как тестового пользователя
5. Создайте **OAuth 2.0 Client ID**:
   - APIs & Services → Credentials → Create Credentials → OAuth client ID
   - Application type: **Desktop app**
   - Скачайте JSON и сохраните как `google-calendar-credentials.json`

**Возможные расположения credentials файла:**

Server автоматически ищет credentials в следующих местах (в порядке приоритета):
1. `<working-dir>/server/google-calendar-credentials.json`
2. `~/google-calendar-credentials.json` (домашняя директория)
3. `./server/google-calendar-credentials.json`
4. `./google-calendar-credentials.json`

Рекомендуется поместить файл в `server/google-calendar-credentials.json` относительно корня проекта.

### 2. Проверка сборки

Проект уже собран! JAR файл находится здесь:
```
mcp-server/build/libs/mcp-server-1.0.0.jar
```

Если нужно пересобрать:
```bash
./gradlew :mcp-server:build
```

### 3. Запуск Server модуля

MCP сервер интегрирован в ваш server модуль и запускается автоматически:

```bash
# Сборка всего проекта
./gradlew build

# Запуск server модуля
./gradlew :server:run
```

При старте server автоматически:
1. Выведет текущую рабочую директорию
2. Попытается найти credentials в нескольких возможных местах
3. Попытается найти MCP server JAR
4. Зарегистрирует Google Calendar MCP клиент (если файлы найдены)
5. Запустит MCP subprocess при первом запросе к Claude API
6. Получит список доступных tools от MCP сервера
7. Добавит их к каждому запросу в Claude API

**Пример вывода при старте:**
```
Current working directory: /Users/user/project
Found Google Calendar credentials at: /Users/user/project/server/google-calendar-credentials.json
Found MCP server JAR at: /Users/user/project/mcp-server/build/libs/mcp-server-1.0.0.jar
Registered Google Calendar MCP client
```

Если файлы не найдены, вы увидите предупреждение со списком проверенных путей.

### 4. Настройка environment переменных

Установите API ключ Anthropic:

```bash
export ANTHROPIC_API_KEY="your-api-key-here"
```

### 5. Первый запуск

При первом использовании календаря через API:
1. Откроется браузер для OAuth авторизации
2. Авторизуйтесь в Google
3. Разрешите доступ к календарю
4. Токен сохранится в `server/tokens/`

### 6. Тестирование

Отправьте запрос к вашему API через HTTP клиент или frontend:

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

Claude автоматически:
1. Увидит доступные calendar tools
2. Вызовет `list_events` через MCP
3. Получит результаты от MCP сервера
4. Сформирует ответ пользователю

Другие примеры запросов:
- "Создай встречу 'Обсуждение проекта' на завтра в 14:00, длительность 1 час"
- "Какие у меня встречи на следующей неделе?"
- "Удали событие с ID xyz123"

## Структура проекта

```
project/
├── mcp-server/                              # MCP Server модуль
│   ├── src/main/kotlin/ru/alekseev/mcp/
│   │   ├── Main.kt                          # Точка входа (stdio)
│   │   ├── MCPServer.kt                     # MCP протокол
│   │   ├── MCPModels.kt                     # JSON-RPC модели
│   │   └── GoogleCalendarService.kt         # Google Calendar API
│   ├── build/libs/
│   │   └── mcp-server-1.0.0.jar            # Fat JAR (14MB)
│   └── build.gradle.kts
│
├── server/                                  # Backend Server модуль
│   ├── src/main/kotlin/ru/alekseev/myapplication/
│   │   ├── service/
│   │   │   ├── MCPClient.kt                # MCP клиент (stdio)
│   │   │   ├── MCPManager.kt               # Менеджер MCP серверов
│   │   │   └── ClaudeApiService.kt         # Claude API с MCP
│   │   ├── di/
│   │   │   └── AppModule.kt                # Koin DI конфигурация
│   │   └── Application.kt
│   ├── google-calendar-credentials.json     # OAuth credentials
│   └── tokens/                              # OAuth tokens (создается автоматически)
│
└── shared/                                  # Общие модели
    └── src/commonMain/kotlin/.../dto/
        ├── ClaudeApiDto.kt                  # Claude API models
        └── MCPDto.kt                        # MCP protocol models
```

## Устранение неполадок

### Credentials или JAR не найдены

Если при запуске вы видите предупреждение:
```
WARNING: Google Calendar credentials not found. Tried:
  - /app/server/google-calendar-credentials.json
  - ...
```

**Решение:**

1. **Проверьте рабочую директорию:**
   - Посмотрите на строку `Current working directory: ...` в логах
   - Убедитесь что credentials файл находится относительно этой директории

2. **Поместите credentials в правильное место:**
   ```bash
   # Если working directory = /app
   mkdir -p /app/server
   cp google-calendar-credentials.json /app/server/

   # Или в домашнюю директорию
   cp google-calendar-credentials.json ~/
   ```

3. **Проверьте что JAR собран:**
   ```bash
   ./gradlew :mcp-server:jar
   ls -l mcp-server/build/libs/mcp-server-1.0.0.jar
   ```

4. **Используйте абсолютный путь:**
   - Server автоматически преобразует найденные пути в абсолютные
   - Вы можете вручную поместить файл по любому из проверяемых путей

### MCP сервер не подключается

1. Проверьте что credentials и JAR найдены (смотрите выше)
2. Убедитесь что используется Java 11+
3. Проверьте логи MCP subprocess в stderr

### Ошибка при авторизации

1. Проверьте что OAuth consent screen настроен
2. Проверьте что вы добавлены как тестовый пользователь
3. Проверьте что scope `https://www.googleapis.com/auth/calendar` добавлен
4. Удалите папку `server/tokens/` и попробуйте снова

### Браузер не открывается

1. Сервер выведет URL в логи
2. Скопируйте URL и откройте вручную в браузере
3. После авторизации, сервер получит код автоматически (порт 8888)

### Как посмотреть логи

Логи MCP сервера выводятся в stderr вашего server приложения:

```bash
# Логи при запуске
./gradlew :server:run

# Вы увидите:
# Registered Google Calendar MCP client
# MCP Manager initialized
# Successfully connected to MCP client: ...
```

## Для AI Challenge

Ваша задача выполнена:

- ✅ Установлен MCP SDK/клиент (реализован протокол на Kotlin)
- ✅ Реализован инструмент для Google Calendar
- ✅ Зарегистрированы все инструменты в MCP (4 инструмента)
- ✅ Агент может вызывать инструменты через MCP
- ✅ Получение результатов работает

### Демонстрация для Challenge:

1. Покажите исходный код MCP интеграции:
   - `mcp-server/` - MCP сервер
   - `server/service/MCPClient.kt` - MCP клиент
   - `server/service/MCPManager.kt` - менеджер
   - `server/service/ClaudeApiService.kt` - интеграция с Claude API
2. Покажите вызов через API (curl или Postman)
3. Покажите логи с tool calls
4. Покажите результат выполнения

## Дополнительная информация

### Как работает MCP в вашем приложении

1. **При старте server**:
   - Koin создает MCPManager singleton
   - Регистрируются MCP клиенты (Google Calendar)
   - Credentials передаются через environment variables

2. **При первом запросе к Claude API**:
   - MCPManager запускает MCP subprocess (java -jar mcp-server.jar)
   - Отправляет `initialize` запрос
   - Получает `tools/list` от MCP сервера
   - Добавляет tools к Claude API request

3. **При tool_use от Claude**:
   - ClaudeApiService обнаруживает tool_use в response
   - MCPManager маршрутизирует вызов к нужному MCP client
   - MCPClient отправляет `tools/call` в subprocess
   - Получает результат через stdout
   - Отправляет результат обратно в Claude API

4. **Conversation loop**:
   - Процесс повторяется пока Claude не вернет финальный ответ
   - Поддерживается multi-turn conversation с tool calls

### Безопасность

- Файлы `google-calendar-credentials.json` и `tokens/` в `.gitignore`
- OAuth2 flow с refresh tokens
- Локальное хранение токенов
- Используйте "Testing" mode для OAuth consent screen

## Контакты

Если возникнут вопросы:
- Проверьте README в `mcp-server/README.md`
- Посмотрите официальную документацию MCP: https://modelcontextprotocol.io
- Проверьте Google Calendar API docs: https://developers.google.com/calendar/api
