const express = require("express");
const axios = require("axios");
const fs = require("fs");
const cors = require("cors");
const {randomUUID} = require('crypto');

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

let dialogHistory = [];

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

app.post("/chat", async (req, res) => {
    try {
        const {message} = req.body;

        if (!message) {
            return res.status(400).json({error: "message is required"});
        }

        // Добавляем новое сообщение пользователя
        dialogHistory.push({
            role: "user",
            content: message
        });

        const token = await getToken();

        // Добавляем system-промпт только в начале!
        const systemMessage = {
            role: "system",

            content: ""

                + " Представься как опытный фитнес-тренер, заинтересованный в успехе каждого клиента. Веди активный диалог, задавая точные вопросы которые помогут тебе достичь цели. Ты можешь задать несколько вопросов, но только такие которые можно объединить в один контекст. Диалог должен основываться на ответах клиента, позволяя постепенно раскрывать его потребности и ожидания."

                + " Вопросы должны быть ориентированы на достижение основной цели. В первую очередь выполни промежуточные цели, при их выполнении сохраняй полученную информацию и в дальнейшем используй полученную информацию для достижения основной цели. Выполненные цели больше не надо выполнять. Твои вопросы формируются предельно ясно и точно. Дополняй список промежуточный целей по ходу диалога с клиентом, на основе его пожеланий"

                + " Обязательно обращай внимание если клиент что-то расписывает подробно"

                + " Основные цели: составить индивидуальную программу тренировок, подборать оптимальную схемы питания и дать рекомендации по пищевым добавкам"

                + " Промежуточные цели: Узнай имя, узнай возраст, вес и рост клиента, узнай есть ли у клиента лишний вес, узнай какие изменения в теле хочет клиент, узнай есть ли какие-то дополнительные пожелания"

                + " Когда ты получишь достаточно информации для выполнения основных целей тогда выполни основный цели и после этого заверши встречу, выразив благодарность за сотрудничество и поинтересовавшись степенью удовлетворённости клиента консультацией. Завершай встречу полностью, это значит если тебе что-то ответит клиент после того как ты завершишь встречу ты будешь молчать и ничего не отвечать. Если клиент не удовлетворен, спроси уточняющие вопросы и выясни его пожелания, пока не достигнешь достаточной ясности для выполнения основных целей."
        };

        const messagesToSend = [
            systemMessage,
            ...dialogHistory
        ];

        const response = await axios.post(
            "https://gigachat.devices.sberbank.ru/api/v1/chat/completions",
            {
                model: "GigaChat",
                messages: messagesToSend
            },
            {
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json"
                },
                httpsAgent
            }
        );

        const output = response.data.choices[0].message.content;

        const escapedAnswer = JSON.stringify(output).slice(1, -1);

        console.log('Ответ:', output);
        console.log('Отформатированный ответ:', escapedAnswer);

        dialogHistory.push({
            role: "assistant",
            content: escapedAnswer
        });

        res.json({ bot: escapedAnswer }); // Возвращаем ответ

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
            logData = {message: err.message};
        }

        console.error("Error:", logData);

        res.status(logData.statusCode || 500).json({
            error: "GigaChat API error",
            details: logData
        });
    }
});

function isJSON(str) {
    try {
        JSON.parse(str);
        return true;
    } catch {
        return false;
    }
}

// Запускаем сервер
app.listen(3000, () => console.log("GigaChat proxy running"));