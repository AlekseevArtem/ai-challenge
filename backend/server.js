const express = require("express");
const axios = require("axios");
const fs = require("fs");
const cors = require("cors");
const { randomUUID } = require('crypto');

const app = express();
app.use(express.json());
app.use(cors());

// Загружаем сертификаты
const httpsAgent = new (require("https").Agent)({
    ca: fs.readFileSync("./cert/russiantrustedca2024.pem"),
    rejectUnauthorized: false
});

// Конфиг
const CLIENT_ID = process.env.GIGACHAT_CLIENT_ID;
const CLIENT_SECRET = process.env.GIGACHAT_CLIENT_SECRET;

let cachedToken = null;
let tokenExpires = 0;

// Получение токена
async function getToken() {
    const now = Date.now();

    // Если токен ещё действует — возвращаем
    if (cachedToken && now < tokenExpires) {
        console.log('cachedToken:', cachedToken);
        return cachedToken;
    }

    const authString = Buffer.from(`${CLIENT_ID}:${CLIENT_SECRET}`).toString("base64");

    const res = await axios.post(
        "https://ngw.devices.sberbank.ru:9443/api/v2/oauth",
        "scope=GIGACHAT_API_PERS",
        {
            headers: {
                Authorization: `Basic ${authString}`,
                Accept: "application/json",
                RqUID: `${randomUUID()}`,
                "Content-Type": "application/x-www-form-urlencoded"
            },
            httpsAgent
        }
    );

    cachedToken = res.data.access_token;
    tokenExpires = now + res.data.expires_in * 1000;

    return cachedToken;
}

// Маршрут для отправки текста в GigaChat
app.post("/chat", async (req, res) => {
    try {
        const { message } = req.body;

        const token = await getToken();

        console.error('Token:', token);

        const response = await axios.post(
            "https://gigachat.devices.sberbank.ru/api/v1/chat/completions",
            {
                model: "GigaChat",
                messages: [
                    {
                        role: "system",
                        content: "Ты отвечаешь только в формате JSON {\"answer\": \"text\"}"
                    },
                    {
                        role: "user",
                        content: message
                    }
                ]
            },
            {
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json"
                },
                httpsAgent
            }
        );

        console.log('Ответ:', response.data.choices[0].message.content);

        // Парсим ответ от GigaChat
        const parsedResponse = JSON.parse(response.data.choices[0].message.content);
        const answer = parsedResponse.answer;

        res.json({ bot: answer }); // Возвращаем ответ

    } catch (err) {
        let logData = {};

        if (err.response) {
            logData = {
                statusCode: err.response.status,
                data: err.response.data,
                headers: err.response.headers,
                statusText: err.response.statusText,
            };
        } else {
            logData = { message: err.message };
        }

        console.error('Error:', logData);
        res.status(logData.statusCode || 500).json({
            error: "GigaChat API error",
            details: logData
        });
    }
});

// Запускаем сервер
app.listen(3000, () => console.log("GigaChat proxy running"));