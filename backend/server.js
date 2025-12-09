const express = require("express");
const axios = require("axios");
const cors = require("cors");
require("dotenv").config();

const app = express();
app.use(express.json());
app.use(cors());

// Claude API ключ хранится в .env
const CLAUDE_API_KEY = process.env.ANTHROPIC_API_KEY;
if (!CLAUDE_API_KEY) {
    console.error("Ошибка: ANTHROPIC_API_KEY не установлен!");
    process.exit(1);
}

let savedTemperature = null;

// --- Отправка запроса к Claude ---
async function sendMessageToClaude(message) {
    // Добавляем новое сообщение пользователя

    let messagesToSend = [];
    messagesToSend.push({
        role: "user",
        content: message
    });

    // Формируем тело запроса для Claude
    const body = {
        model: "claude-opus-4-5-20251101", // можно claude-3 если есть доступ
        messages: messagesToSend,
        max_tokens_to_sample: 500
    };

    if (savedTemperature !== null) {
        body.temperature = savedTemperature;
    }

    console.log("body", body)

    const response = await axios.post(
        "https://api.anthropic.com/v1/chat/completions",
        body,
        {
            headers: {
                "X-API-Key": CLAUDE_API_KEY,
                "Content-Type": "application/json",
                "Anthropic-Version": "2025-11-01",
            }
        }
    );

    const output = response.data.choices?.[0].message.content;

    return output;
}

// --- Роуты ---
app.post("/chat", async (req, res) => {
    try {
        const { message } = req.body;
        if (!message) return res.status(400).json({ error: "message is required" });

        const answer = await sendMessageToClaude(message);

        console.log("Ответ Claude:", answer);
        res.json({ bot: answer });
    } catch (err) {
        console.error("Claude API error:", err.response?.data || err.message);
        res.status(500).json({ error: "Claude API error", details: err.response?.data || err.message });
    }
});

app.post("/set-temperature", (req, res) => {
    const { temperature } = req.body;
    if (temperature === undefined) return res.status(400).json({ error: "temperature is required" });

    savedTemperature = Number(temperature);
    console.log("Температура обновлена:", savedTemperature);

    res.json({ ok: true });
});

// Запуск сервера
app.listen(3000, () => console.log("Claude proxy running on port 3000"));
