document.addEventListener("DOMContentLoaded", async () => {
    // Элементы чата
    const input = document.getElementById("messageInput");
    const messages = document.getElementById("messages");

    // --- Функции ---
    function addMessage(text, type = "user", messageId = null) {
        const div = document.createElement("div");
        div.classList.add("message", type);
        div.innerHTML = text;

        // Если это сообщение бота, добавляем data-атрибут с ID и делаем кликабельным
        if (type === "bot" && messageId) {
            div.dataset.messageId = messageId;
            div.style.cursor = "pointer";
            div.title = "Нажмите для просмотра информации о токенах";
        }

        messages.appendChild(div);
        messages.scrollTop = messages.scrollHeight;
    }

    async function loadHistory() {
        try {
            const res = await fetch("http://localhost:3000/history");
            const data = await res.json();

            if (data.history && data.history.length > 0) {
                console.log(`📚 Загружено ${data.history.length} сообщений из истории`);

                // Отображаем историю
                for (const item of data.history) {
                    if (item.type === "message") {
                        // Обычные сообщения
                        addMessage(item.user, "user");
                        addMessage(item.bot.replace(/\n/g, "<br>"), "bot", item.id);
                    } else if (item.type === "summary") {
                        // Summary объекты
                        const summaryText = `<i>📦 Краткое содержание предыдущих сообщений:<br>${item.summary.replace(/\n/g, "<br>")}</i>`;
                        addMessage(summaryText, "bot");
                    }
                }
            }
        } catch (e) {
            console.error("Ошибка загрузки истории:", e);
        }
    }

    async function sendMessageToBot(text) {
        try {
            const res = await fetch("http://localhost:3000/chat", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({message: text})
            });
            const data = await res.json();

            console.log("Ответ сервера:", data);

            return {
                text: data.bot.replace(/\n/g, "<br>"),
                messageId: data.messageId
            };
        } catch (e) {
            console.error("Ошибка при отправке сообщения:", e);
            return {
                text: "Ошибка сервера",
                messageId: null
            };
        }
    }

    async function showMessageInfo(messageId) {
        try {
            const res = await fetch(`http://localhost:3000/message/${messageId}`);
            const data = await res.json();

            if (res.ok) {
                // Заполняем модальное окно данными
                document.getElementById("modalModel").textContent = data.model || "-";
                document.getElementById("modalInputTokens").textContent = data.usage?.input_tokens || 0;
                document.getElementById("modalOutputTokens").textContent = data.usage?.output_tokens || 0;
                document.getElementById("modalTotalTokens").textContent =
                    (data.usage?.input_tokens || 0) + (data.usage?.output_tokens || 0);
                document.getElementById("modalCost").textContent = data.formattedCost || "-";
                document.getElementById("modalTimestamp").textContent =
                    new Date(data.timestamp).toLocaleString("ru-RU");

                // Показываем модальное окно
                document.getElementById("tokenModal").style.display = "flex";
            } else {
                console.error("Сообщение не найдено");
            }
        } catch (e) {
            console.error("Ошибка получения информации о сообщении:", e);
        }
    }

    async function setTemperature(temp) {
        try {
            await fetch("http://localhost:3000/set-temperature", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({temperature: temp})
            });
        } catch (e) {
            console.error("Ошибка отправки температуры:", e);
        }
    }

    async function send() {
        const text = input.value.trim();
        if (!text) return;

        addMessage(text, "user");
        input.value = "";

        const reply = await sendMessageToBot(text);
        addMessage(reply.text, "bot", reply.messageId);
    }

    // Загружаем историю при старте
    await loadHistory();

    // --- Обработчики ---
    document.body.addEventListener("click", async (e) => {
        if (e.target.id === "send-btn") {
            await send();
        }
        if (e.target.id === "settings-btn") {
            document.getElementById("settingsModal").style.display = "flex";
        }
        if (e.target.id === "closeSettings") {
            document.getElementById("settingsModal").style.display = "none";
        }
        if (e.target === document.getElementById("settingsModal")) {
            document.getElementById("settingsModal").style.display = "none";
        }

        // Обработка клика на сообщение бота
        if (e.target.classList.contains("message") && e.target.classList.contains("bot") && e.target.dataset.messageId) {
            await showMessageInfo(e.target.dataset.messageId);
        }

        // Закрытие модального окна с токенами
        if (e.target.id === "closeTokenModal") {
            document.getElementById("tokenModal").style.display = "none";
        }
        if (e.target === document.getElementById("tokenModal")) {
            document.getElementById("tokenModal").style.display = "none";
        }
    });

    // Отправка сообщения по Enter
    input.addEventListener("keypress", async (e) => {
        if (e.key === "Enter") await send();
    });


    // Элементы ползунка
    const range = document.getElementById("progressRange");
    const value = document.getElementById("progressValue");

    // Работа ползунка
    range.addEventListener("input", async () => {
        const temp = Number(range.value).toFixed(1);
        value.textContent = temp;
        console.log("Температура:", temp);
        await setTemperature(temp);
    });
});