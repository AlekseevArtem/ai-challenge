# Google Calendar MCP Server (Kotlin)

MCP (Model Context Protocol) сервер для интеграции с Google Calendar, написанный на Kotlin. Предоставляет инструменты для управления событиями календаря через Claude.

## Возможности

- **list_events** - получение списка предстоящих событий
- **create_event** - создание нового события
- **update_event** - обновление существующего события
- **delete_event** - удаление события

## Требования

- JDK 17 или выше
- Gradle (встроен в проект)
- Аккаунт Google
- Claude Desktop или Claude Code CLI

## Установка и настройка

### 1. Настройка Google Cloud Console

1. Перейдите в [Google Cloud Console](https://console.cloud.google.com/)
2. Создайте новый проект или выберите существующий
3. Включите Google Calendar API:
   - Перейдите в "APIs & Services" → "Library"
   - Найдите "Google Calendar API"
   - Нажмите "Enable"
4. Создайте OAuth 2.0 credentials:
   - Перейдите в "APIs & Services" → "Credentials"
   - Нажмите "Create Credentials" → "OAuth client ID"
   - Если требуется, настройте OAuth consent screen:
     - Выберите "External" (если не используете Google Workspace)
     - Заполните обязательные поля (название приложения, email)
     - Добавьте scope: `https://www.googleapis.com/auth/calendar`
     - Добавьте себя как тестового пользователя
   - Вернитесь к созданию OAuth client ID
   - Выберите тип приложения: "Desktop app"
   - Укажите имя (например, "Google Calendar MCP Server")
   - Нажмите "Create"
5. Скачайте JSON файл с credentials:
   - Нажмите на иконку скачивания рядом с созданным OAuth client
   - Сохраните файл как `google-calendar-credentials.json` в папку `server/mcp-server/`

### 2. Сборка проекта

Из корня проекта выполните:

```bash
./gradlew :mcp-server:build
```

Это создаст fat JAR в `mcp-server/build/libs/mcp-server-1.0.0.jar`

### 3. Настройка Claude Desktop

Отредактируйте конфигурационный файл Claude Desktop:

**MacOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
**Linux**: `~/.config/Claude/claude_desktop_config.json`

Добавьте следующую конфигурацию:

```json
{
  "mcpServers": {
    "google-calendar": {
      "command": "java",
      "args": [
        "-jar",
        "/полный/путь/до/проекта/mcp-server/build/libs/mcp-server-1.0.0.jar"
      ]
    }
  }
}
```

**Важно**: Замените `/полный/путь/до/проекта/` на реальный абсолютный путь к вашему проекту!

Например:
```json
{
  "mcpServers": {
    "google-calendar": {
      "command": "java",
      "args": [
        "-jar",
        "/Users/artemalekseev/AndroidStudioProjects/MyApplication4/mcp-server/build/libs/mcp-server-1.0.0.jar"
      ]
    }
  }
}
```

### 4. Настройка Claude Code CLI (альтернатива)

Если вы используете Claude Code CLI вместо Claude Desktop, добавьте в `.claude/settings.json`:

```json
{
  "mcp": {
    "servers": {
      "google-calendar": {
        "command": "java",
        "args": [
          "-jar",
          "/полный/путь/до/проекта/mcp-server/build/libs/mcp-server-1.0.0.jar"
        ]
      }
    }
  }
}
```

### 5. Первый запуск и авторизация

1. Перезапустите Claude Desktop (или перезагрузите Claude Code CLI)
2. При первом использовании любого инструмента календаря:
   - Откроется браузер с запросом на авторизацию Google
   - Выберите ваш Google аккаунт
   - Разрешите доступ к календарю
   - После успешной авторизации токен будет сохранен в `server/mcp-server/tokens/`

## Использование

После настройки вы можете использовать инструменты в Claude:

### Примеры запросов:

**Получить список событий:**
```
Покажи мои события на сегодня
```
```
Какие у меня встречи на следующей неделе?
```

**Создать событие:**
```
Создай событие "Встреча с командой" на завтра в 14:00, длительность 1 час
```
```
Добавь в календарь "Обед с клиентом" на 20 декабря с 12:00 до 13:30
```

**Обновить событие:**
```
Перенеси событие с ID abc123 на 15:00
```

**Удалить событие:**
```
Удали событие с ID abc123
```

## Разработка

### Структура проекта

```
mcp-server/
├── src/main/kotlin/ru/alekseev/mcp/
│   ├── Main.kt                    # Точка входа
│   ├── MCPServer.kt               # MCP протокол
│   ├── MCPModels.kt               # Модели данных
│   └── GoogleCalendarService.kt   # Google Calendar API
├── build.gradle.kts
└── README.md
```

### Локальная разработка

```bash
# Сборка
./gradlew :mcp-server:build

# Запуск (для отладки)
java -jar mcp-server/build/libs/mcp-server-1.0.0.jar

# Пересборка при изменениях
./gradlew :mcp-server:build --continuous
```

### Отладка

Для отладки можно запустить сервер вручную и отправлять JSON-RPC запросы через stdin:

```bash
java -jar mcp-server/build/libs/mcp-server-1.0.0.jar
```

Пример запроса:
```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}
```

## Устранение неполадок

### Ошибка "credentials not found"

Убедитесь, что файл `google-calendar-credentials.json` находится в папке `server/mcp-server/`.

### Ошибка авторизации

1. Удалите папку `server/mcp-server/tokens/`
2. Перезапустите Claude Desktop/CLI
3. Повторите процесс авторизации

### MCP сервер не подключается

1. Проверьте правильность пути к JAR файлу в конфигурационном файле
2. Убедитесь, что проект собран (`./gradlew :mcp-server:build`)
3. Проверьте логи Claude Desktop
4. Убедитесь, что у вас установлена JDK 17+

### Браузер не открывается при авторизации

1. Скопируйте URL из логов
2. Откройте его вручную в браузере
3. После авторизации сервер автоматически получит токен

### Ошибка сборки

Убедитесь, что у вас установлена JDK 17 или выше:
```bash
java -version
```

## Интеграция с существующим проектом

Этот MCP сервер является отдельным модулем Kotlin Multiplatform проекта. Он использует те же зависимости и инструменты, что и основной проект.

### Общие зависимости:
- Kotlin Serialization для JSON
- Kotlinx Coroutines (если потребуется в будущем)
- Google API Client для работы с Calendar API

## Безопасность

- Файлы `google-calendar-credentials.json` и `tokens/` содержат конфиденциальную информацию
- Они автоматически добавлены в `.gitignore`
- Не публикуйте эти файлы в публичных репозиториях
- Используйте OAuth consent screen в режиме "Testing" для личного использования

## Лицензия

MIT

## Архитектура

MCP сервер работает следующим образом:

1. **Транспорт**: JSON-RPC через stdio (стандартный ввод/вывод)
2. **Протокол**: MCP (Model Context Protocol) версии 2024-11-05
3. **Методы**:
   - `initialize` - инициализация сервера
   - `tools/list` - получение списка доступных инструментов
   - `tools/call` - вызов инструмента
4. **Google Calendar API**: OAuth2 авторизация с сохранением токена

Claude Desktop/CLI запускает JAR как подпроцесс и общается с ним через stdio.
