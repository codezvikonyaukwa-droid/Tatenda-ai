const roleInfo = {
  general: ["General assistant", "General questions and planning."],
  coding: ["Coding specialist", "Software design, code review and debugging."],
  diagnosis: ["Diagnostics specialist", "Structured troubleshooting and safe device checks."],
  memory: ["Memory organizer", "Organizing notes and user-approved information."]
};
const $ = (id) => document.getElementById(id);
const messages = $("messages");
const promptBox = $("prompt");
const welcome = $("welcome");
const roleSelect = $("roleSelect");

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
  label.textContent = role === "user" ? "YOU" : "CHATENDER · LOCAL PREVIEW";
  const body = document.createElement("div");
  body.textContent = content;
  bubble.append(label, body);
  messages.appendChild(bubble);
  bubble.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

function localReply(text, role) {
  const clean = text.trim();
  const lower = clean.toLowerCase();
  if (/^(help|commands|what can you do)/.test(lower)) {
    return "This desktop preview supports role selection, local workspace navigation and saved model preferences. Connect a secure backend to enable cloud AI, file analysis and tool execution.";
  }
  if (lower.includes("time")) return "Your computer's local time is " + new Date().toLocaleString() + ".";
  if (lower.includes("clear") && lower.includes("chat")) {
    messages.replaceChildren();
    welcome.hidden = false;
    return "Conversation cleared.";
  }
  const name = (roleInfo[role] || roleInfo.general)[0];
  return name + " selected. Your message is visible in this local preview, but no AI provider is connected yet. The next integration step is a secure desktop backend. Do not paste an API key into this page.";
}

function sendMessage(text) {
  const clean = text.trim();
  if (!clean) return;
  const role = roleSelect.value;
  addMessage("user", clean);
  promptBox.value = "";
  window.setTimeout(() => addMessage("assistant", localReply(clean, role)), 180);
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
  messages.replaceChildren();
  welcome.hidden = false;
  promptBox.focus();
});
document.querySelectorAll(".nav-item[data-view]").forEach((button) => {
  button.addEventListener("click", () => {
    document.querySelectorAll(".nav-item[data-view]").forEach((item) => item.classList.remove("active"));
    button.classList.add("active");
    const view = button.dataset.view;
    if (roleInfo[view]) roleSelect.value = view;
    setRole(roleSelect.value);
    if (view !== "assistant") addMessage("assistant", (roleInfo[view] || roleInfo.general)[0] + " workspace selected. This section is currently a UI preview.");
  });
});

const dialog = $("settingsDialog");
const storedKeys = ["general", "coding", "diagnosis", "memory"];
$("openSettings").addEventListener("click", () => {
  $("voiceId").value = localStorage.getItem("chatender.voiceId") || "e13fa398a7f445a685316a3de6089ce7";
  storedKeys.forEach((key) => {
    const field = $("model" + key[0].toUpperCase() + key.slice(1));
    field.value = localStorage.getItem("chatender.model." + key) || "openrouter/free";
  });
  dialog.showModal();
});
$("saveSettings").addEventListener("click", () => {
  localStorage.setItem("chatender.voiceId", $("voiceId").value.trim());
  storedKeys.forEach((key) => {
    const field = $("model" + key[0].toUpperCase() + key.slice(1));
    localStorage.setItem("chatender.model." + key, field.value.trim() || "openrouter/free");
  });
});
setRole("general");
