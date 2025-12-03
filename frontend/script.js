const input = document.getElementById("messageInput");
const btn = document.getElementById("send-btn");
const messages = document.getElementById("messages");

function addMessage(text, type = "user") {
    const div = document.createElement("div");
    div.classList.add("message", type);
    div.textContent = text;
    messages.appendChild(div);
    messages.scrollTop = messages.scrollHeight;
}

btn.addEventListener("click", async function () {
    const text = input.value.trim();
    if (!text) return;

    addMessage(text, "user");
    input.value = "";

    const reply = await sendMessageToBot(text);

    addMessage(reply, "bot");
});

async function sendMessageToBot(text) {
    const res = await fetch("http://localhost:3000/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message: text })
    });

    const data = await res.json();
    return data.bot;
}

input.addEventListener("keypress", e => {
    if (e.key === "Enter") btn.click();
});