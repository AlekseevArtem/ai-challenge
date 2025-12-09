document.addEventListener("DOMContentLoaded", () => {
    // Элементы чата
    const input = document.getElementById("messageInput");
    const messages = document.getElementById("messages");

    // --- Функции ---
    function addMessage(text, type = "user") {
        const div = document.createElement("div");
        div.classList.add("message", type);
        div.innerHTML = text;
        messages.appendChild(div);
        messages.scrollTop = messages.scrollHeight;
    }

    async function sendMessageToBot(text) {
        try {
            const res = await fetch("http://localhost:3000/chat", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({message: text})
            });
            const data = await res.json();
            return data.bot.replace(/\\n/g, "<br>");
        } catch (e) {
            console.error("Ошибка при отправке сообщения:", e);
            return "Ошибка сервера";
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
        addMessage(reply, "bot");
    }

    // --- Обработчики ---
    document.body.addEventListener("click", async (e) => {
        if (e.target.id === "send-btn") {
            await send()
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
        await setTemperature(temp)
    });

    document.body.addEventListener("click", async (e) => {
        console.log("Клик:", e.target); // показывает элемент, по которому кликнули
    });
});