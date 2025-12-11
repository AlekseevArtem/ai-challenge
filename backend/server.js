const express = require("express");
const axios = require("axios");
const cors = require("cors");
require("dotenv").config();

// Импорт сервисов
const historyService = require("./services/historyService");
const SummarizationService = require("./services/summarizationService");
const { calculateCost, formatCost } = require("./utils/pricing");

// Конфигурация
const CONFIG = {
    PORT: process.env.PORT || 3000,
    CLAUDE_API_URL: "https://api.anthropic.com/v1/messages",
    CLAUDE_MODEL: "claude-sonnet-4-5-20250929",
    ANTHROPIC_VERSION: "2023-06-01",
    MAX_TOKENS: 500,
    TEMPERATURE: {
        MIN: 0,
        MAX: 1,
        DEFAULT: null
    }
};

// Проверка API ключа
const CLAUDE_API_KEY = process.env.ANTHROPIC_API_KEY;
if (!CLAUDE_API_KEY) {
    console.error("Ошибка: ANTHROPIC_API_KEY не установлен в .env файле!");
    process.exit(1);
}

// Инициализация сервисов
const summarizationService = new SummarizationService(
    CLAUDE_API_KEY,
    CONFIG.CLAUDE_API_URL,
    CONFIG.ANTHROPIC_VERSION
);

// Инициализация Express
const app = express();
app.use(express.json());
app.use(cors());

// Состояние приложения
const state = {
    temperature: CONFIG.TEMPERATURE.DEFAULT,
    tokenStats: {
        totalInputTokens: 0,
        totalOutputTokens: 0,
        totalRequests: 0
    }
};

// Валидация температуры
function validateTemperature(temperature) {
    const temp = Number(temperature);
    if (isNaN(temp)) {
        return { valid: false, error: "temperature должна быть числом" };
    }
    if (temp < CONFIG.TEMPERATURE.MIN || temp > CONFIG.TEMPERATURE.MAX) {
        return {
            valid: false,
            error: `temperature должна быть в диапазоне ${CONFIG.TEMPERATURE.MIN}-${CONFIG.TEMPERATURE.MAX}`
        };
    }
    return { valid: true, value: temp };
}

// Отправка запроса к Claude API
async function sendMessageToClaude(message) {
    // Получаем историю сообщений для API
    const historyMessages = await historyService.getMessagesForApi();

    // Добавляем новое сообщение пользователя
    const requestBody = {
        model: CONFIG.CLAUDE_MODEL,
        messages: [
            ...historyMessages,
            {
                role: "user",
                content: message
            }
        ],
        max_tokens: CONFIG.MAX_TOKENS
    };

    // Добавляем температуру, если установлена
    if (state.temperature !== null) {
        requestBody.temperature = state.temperature;
    }

    console.log("Запрос к Claude API:", JSON.stringify({
        model: requestBody.model,
        messagesCount: requestBody.messages.length,
        temperature: requestBody.temperature
    }, null, 2));

    try {
        const response = await axios.post(
            CONFIG.CLAUDE_API_URL,
            requestBody,
            {
                headers: {
                    "X-API-Key": CLAUDE_API_KEY,
                    "Content-Type": "application/json",
                    "Anthropic-Version": CONFIG.ANTHROPIC_VERSION
                }
            }
        );

        console.log("Ответ от Claude API получен");

        // Правильная структура ответа Claude API
        const output = response.data.content?.[0]?.text;
        const usage = response.data.usage;

        if (!output) {
            throw new Error("Некорректная структура ответа от Claude API");
        }

        // Сохраняем сообщение в историю с полным ответом API
        const savedMessage = await historyService.addMessage(message, response.data);

        // Проверяем, нужно ли сжать историю
        await summarizationService.autoSummarize(historyService);

        // Подсчёт и логирование токенов
        if (usage) {
            const inputTokens = usage.input_tokens || 0;
            const outputTokens = usage.output_tokens || 0;

            // Обновление общей статистики
            state.tokenStats.totalInputTokens += inputTokens;
            state.tokenStats.totalOutputTokens += outputTokens;
            state.tokenStats.totalRequests += 1;

            console.log(`📊 Токены - Отправлено: ${inputTokens}, Получено: ${outputTokens}`);
            console.log(`📈 Всего - Отправлено: ${state.tokenStats.totalInputTokens}, Получено: ${state.tokenStats.totalOutputTokens}, Запросов: ${state.tokenStats.totalRequests}`);
        }

        return {
            text: output,
            usage: usage || { input_tokens: 0, output_tokens: 0 },
            messageId: savedMessage.id, // ID сохраненного сообщения
            fullResponse: response.data // Полный ответ API
        };
    } catch (error) {
        console.error("Ошибка Claude API:", error.response?.data || error.message);
        throw error;
    }
}

// --- Роуты ---

// Роут для чата с Claude
app.post("/chat", async (req, res) => {
    try {
        const { message } = req.body;

        if (!message || typeof message !== "string") {
            return res.status(400).json({
                error: "message обязателен и должен быть строкой"
            });
        }

        const result = await sendMessageToClaude(message);

        console.log("Ответ отправлен клиенту");
        res.json({
            bot: result.text,
            usage: result.usage,
            messageId: result.messageId,
            model: result.fullResponse.model,
            cost: calculateCost(
                result.fullResponse.model,
                result.usage.input_tokens,
                result.usage.output_tokens
            )
        });
    } catch (err) {
        console.error("Ошибка обработки запроса:", err.response?.data || err.message);
        res.status(500).json({
            error: "Ошибка Claude API",
            details: err.response?.data || err.message
        });
    }
});

// Роут для установки температуры
app.post("/set-temperature", (req, res) => {
    const { temperature } = req.body;

    if (temperature === undefined) {
        return res.status(400).json({ error: "temperature обязателен" });
    }

    const validation = validateTemperature(temperature);

    if (!validation.valid) {
        return res.status(400).json({ error: validation.error });
    }

    state.temperature = validation.value;
    console.log(`Температура обновлена: ${state.temperature}`);

    res.json({
        ok: true,
        temperature: state.temperature
    });
});

// Роут для получения текущей конфигурации
app.get("/config", (req, res) => {
    res.json({
        model: CONFIG.CLAUDE_MODEL,
        temperature: state.temperature,
        maxTokens: CONFIG.MAX_TOKENS
    });
});

// Роут для получения статистики использования токенов
app.get("/token-stats", (req, res) => {
    const totalTokens = state.tokenStats.totalInputTokens + state.tokenStats.totalOutputTokens;
    res.json({
        totalInputTokens: state.tokenStats.totalInputTokens,
        totalOutputTokens: state.tokenStats.totalOutputTokens,
        totalTokens: totalTokens,
        totalRequests: state.tokenStats.totalRequests,
        averageInputTokensPerRequest: state.tokenStats.totalRequests > 0
            ? Math.round(state.tokenStats.totalInputTokens / state.tokenStats.totalRequests)
            : 0,
        averageOutputTokensPerRequest: state.tokenStats.totalRequests > 0
            ? Math.round(state.tokenStats.totalOutputTokens / state.tokenStats.totalRequests)
            : 0
    });
});

// Роут для получения истории диалога
app.get("/history", async (req, res) => {
    try {
        const history = await historyService.getHistory();
        res.json({ history });
    } catch (err) {
        console.error("Ошибка получения истории:", err);
        res.status(500).json({ error: "Не удалось получить историю" });
    }
});

// Роут для получения информации о конкретном сообщении
app.get("/message/:id", async (req, res) => {
    try {
        const { id } = req.params;
        const message = await historyService.getMessageById(id);

        if (!message) {
            return res.status(404).json({ error: "Сообщение не найдено" });
        }

        // Форматируем данные для фронтенда
        const formattedCost = formatCost(message.cost);

        res.json({
            id: message.id,
            timestamp: message.timestamp,
            type: message.type,
            user: message.user,
            bot: message.bot,
            model: message.api?.model,
            usage: message.api?.usage,
            cost: message.cost,
            formattedCost
        });
    } catch (err) {
        console.error("Ошибка получения сообщения:", err);
        res.status(500).json({ error: "Не удалось получить сообщение" });
    }
});

// Роут для очистки истории (для тестирования)
app.delete("/history", async (req, res) => {
    try {
        await historyService.clear();
        res.json({ ok: true, message: "История очищена" });
    } catch (err) {
        console.error("Ошибка очистки истории:", err);
        res.status(500).json({ error: "Не удалось очистить историю" });
    }
});

// Запуск сервера
app.listen(CONFIG.PORT, async () => {
    console.log(`🚀 Claude proxy запущен на порту ${CONFIG.PORT}`);
    console.log(`📝 Модель: ${CONFIG.CLAUDE_MODEL}`);
    console.log(`🌡️  Температура: ${state.temperature ?? "по умолчанию"}`);

    // Инициализируем историю при старте
    await historyService.initialize();
    const messageCount = await historyService.getRegularMessagesCount();
    console.log(`📚 Сообщений в истории: ${messageCount}`);
});
