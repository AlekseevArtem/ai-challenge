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

btn.onclick = () => {
    const text = input.value.trim();
    if (!text) return;

    addMessage(text, "user");
    input.value = "";

    setTimeout(() => {
        addMessage("Bot: " + text, "bot");
    }, 500);
};

input.addEventListener("keypress", e => {
    if (e.key === "Enter") btn.click();
});