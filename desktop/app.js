const roleInfo = {
  general: ["General assistant", "General questions and planning."],
  coding: ["Coding specialist", "Software design, code review and debugging."],
  diagnosis: ["Diagnostics specialist", "Structured troubleshooting and safe device checks."],
  memory: ["Memory organizer", "Organizing notes and user-approved information."]
};
const $ = (id) => document.getElementById(id);
const messageList = $("messages");
const promptBox = $("prompt");
const welcome = $("welcome");
const roleSelect = $("roleSelect");
const sendButton = $("sendButton");
const conversation = [];
const modelKeys = ["general", "coding", "diagnosis", "memory"];

function setRole(role) {
  const info = roleInfo[role] || roleInfo.general;
  $("activeRole").textContent = info[0];
  $("roleDescription").textContent = info[1];
  $("pageTitle").textContent = role === "general" ? "AI Assistant" : info[0];
}
roleSelect.addEventListener("change", () => setRole(roleSelect.value));

function addMessage(role, content) {
  welcome.hidden = true;
  const bubble = document.createElement("div");
  bubble.className = "message " + role;
  const label = document.createElement("span");
  label.className = "who";
  label.textContent = role === "user" ? "YOU" : "CHATENDER";
  const body = document.createElement("div");
  body.textContent = content;
  bubble.append(label, body);
  messageList.appendChild(bubble);
  bubble.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

function clearConversation() {
  conversation.length = 0;
  messageList.replaceChildren();
  welcome.hidden = false;
}

function modelFor(role) {
  return localStorage.getItem("chatender.model." + role) || "openrouter/free";
}

async function sendMessage(text) {
  const clean = text.trim();
  if (!clean || sendButton.disabled) return;
  const role = roleSelect.value;
  conversation.push({ role: "user", content: clean });
  addMessage("user", clean);
  promptBox.value = "";
  sendButton.disabled = true;
  sendButton.textContent = "THINKING…";
  $("activeRole").textContent = roleInfo[role][0] + " · WORKING";
  try {
    const response = await fetch("/api/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        role,
        model: modelFor(role),
        messages: conversation.slice(-24)
      })
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.error || "The local AI bridge returned an error.");
    const answer = typeof data.answer === "string" ? data.answer : "";
    if (!answer.trim()) throw new Error("The AI provider returned an empty answer.");
    conversation.push({ role: "assistant", content: answer });
    if (conversation.length > 24) conversation.splice(0, conversation.length - 24);
    addMessage("assistant", answer);
  } catch (error) {
    addMessage("assistant", "Connection problem: " + (error.message || "Unknown error") +
      "\n\nCheck that desktop/server.py is running in the terminal and that your OpenRouter account has access to the selected model.");
  } finally {
    sendButton.disabled = false;
    sendButton.innerHTML = 'SEND <span>↑</span>';
    setRole(role);
  }
}

$("composer").addEventListener("submit", (event) => {
  event.preventDefault();
  sendMessage(promptBox.value);
});
promptBox.addEventListener("keydown", (event) => {
  if (event.key === "Enter" && !event.shiftKey) {
    event.preventDefault();
    $("composer").requestSubmit();
  }
});
document.querySelectorAll(".quick-card").forEach((button) => {
  button.addEventListener("click", () => sendMessage(button.dataset.prompt || ""));
});
$("newChat").addEventListener("click", () => {
  clearConversation();
  promptBox.focus();
});
document.querySelectorAll(".nav-item[data-view]").forEach((button) => {
  button.addEventListener("click", () => {
    document.querySelectorAll(".nav-item[data-view]").forEach((item) => item.classList.remove("active"));
    button.classList.add("active");
    const view = button.dataset.view;
    if (roleInfo[view]) roleSelect.value = view;
    setRole(roleSelect.value);
  });
});

const dialog = $("settingsDialog");
$("openSettings").addEventListener("click", () => {
  $("voiceId").value = localStorage.getItem("chatender.voiceId") || "e13fa398a7f445a685316a3de6089ce7";
  modelKeys.forEach((key) => {
    const field = $("model" + key[0].toUpperCase() + key.slice(1));
    field.value = modelFor(key);
  });
  dialog.showModal();
});
$("saveSettings").addEventListener("click", () => {
  localStorage.setItem("chatender.voiceId", $("voiceId").value.trim());
  modelKeys.forEach((key) => {
    const field = $("model" + key[0].toUpperCase() + key.slice(1));
    localStorage.setItem("chatender.model." + key, field.value.trim() || "openrouter/free");
  });
});

fetch("/api/status").then((response) => {
  if (!response.ok) throw new Error("offline");
  return response.json();
}).then(() => {
  document.querySelector(".connection small").textContent = "OpenRouter bridge ready";
  document.querySelector(".connection .dot").style.background = "var(--cyan)";
}).catch(() => {
  document.querySelector(".connection small").textContent = "Start desktop/server.py";
  document.querySelector(".connection .dot").style.background = "#d69b52";
  document.querySelector(".disclaimer").textContent = "Start desktop/server.py in the project folder to connect the AI provider.";
});
setRole("general");
