import { api, currentUser, logout } from "./api.js";

const $ = (selector, root = document) => root.querySelector(selector);
const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (character) => ({
  "&": "&amp;",
  "<": "&lt;",
  ">": "&gt;",
  '"': "&quot;",
  "'": "&#39;",
})[character]);

function showMessage(message, kind = "error") {
  const element = $("#message");
  if (!element) return;
  element.textContent = message;
  element.className = `notice notice-${kind}`;
  element.hidden = false;
}

function showFieldErrors(fields = {}) {
  document.querySelectorAll("[data-error]").forEach((element) => {
    element.textContent = fields[element.dataset.error] || "";
  });
}

function setBusy(form, busy, label) {
  const button = $('button[type="submit"]', form);
  if (!button) return;
  button.disabled = busy;
  button.setAttribute("aria-busy", String(busy));
  if (busy) {
    button.dataset.originalLabel = button.textContent;
    button.textContent = label;
  } else if (button.dataset.originalLabel) {
    button.textContent = button.dataset.originalLabel;
    delete button.dataset.originalLabel;
  }
}

function formatDate(value, options = { day: "numeric", month: "short", year: "numeric" }) {
  return new Intl.DateTimeFormat(undefined, options).format(new Date(`${value}${value?.length === 10 ? "T12:00:00" : ""}`));
}

function formatTime(value) {
  return new Intl.DateTimeFormat(undefined, {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function relativeExpiry(days) {
  if (days < 0) return `${Math.abs(days)} days overdue`;
  if (days === 0) return "Today";
  return `${days} days`;
}

function setAuthNoticeFromQuery() {
  const params = new URLSearchParams(window.location.search);
  if (params.has("registered")) showMessage("Account created. Sign in to get started.", "success");
  if (params.has("logout")) showMessage("You have been signed out.", "neutral");
  if (params.has("error")) showMessage("Email or password wasn't recognized.");
}

async function loginPage() {
  setAuthNoticeFromQuery();
  $("#login-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    setBusy(form, true, "Signing in…");
    try {
      await api("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ email: data.get("email"), password: data.get("password") }),
      });
      window.location.assign("/dashboard.html");
    } catch (error) {
      showMessage(error.message);
    } finally {
      setBusy(form, false);
    }
  });
}

async function registerPage() {
  $("#register-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    showFieldErrors();
    const form = event.currentTarget;
    const data = new FormData(form);
    setBusy(form, true, "Creating account…");
    try {
      await api("/api/auth/register", {
        method: "POST",
        body: JSON.stringify({
          name: data.get("name"),
          email: data.get("email"),
          password: data.get("password"),
        }),
      });
      window.location.assign("/?registered");
    } catch (error) {
      showFieldErrors(error.fields);
      showMessage(error.message);
    } finally {
      setBusy(form, false);
    }
  });
}

async function requireUser() {
  try {
    return await currentUser();
  } catch (error) {
    if (error.status === 401) {
      window.location.replace("/");
      return null;
    }
    throw error;
  }
}

async function dashboardPage() {
  const user = await requireUser();
  if (!user) return;
  $("#account-name").textContent = user.name;
  $("#greeting-name").textContent = user.name;
  try {
    const dashboard = await api("/api/dashboard");
    $("#today").textContent = formatDate(dashboard.today, { weekday: "long", day: "numeric", month: "long", year: "numeric" });
    $("#expired-count").textContent = dashboard.expiredCount;
    $("#urgent-count").textContent = dashboard.urgentCount;
    $("#due-count").textContent = dashboard.dueWithin30DaysCount;
    $("#document-count").textContent = dashboard.documents.length;
    $("#document-count-mark").textContent = dashboard.documents.length;

    const body = $("#documents-body");
    body.innerHTML = dashboard.documents.map((document) => `
      <tr>
        <td data-label="Document"><a class="document-name" href="/document.html?id=${encodeURIComponent(document.id)}">${escapeHtml(document.title)}</a>${document.referenceNumber ? `<span class="document-reference">${escapeHtml(document.referenceNumber)}</span>` : ""}</td>
        <td data-label="Category"><span class="category-label">${escapeHtml(document.categoryLabel)}</span></td>
        <td data-label="Expiry date" class="date-cell">${escapeHtml(formatDate(document.expiryDate))}</td>
        <td data-label="Time left" class="remaining-cell">${escapeHtml(relativeExpiry(document.daysUntilExpiry))}</td>
        <td data-label="Status"><span class="status-pill ${escapeHtml(document.status)}">${escapeHtml(document.statusLabel)}</span></td>
        <td class="row-action"><a class="arrow-link" href="/document.html?id=${encodeURIComponent(document.id)}" aria-label="View document">&#8594;</a></td>
      </tr>`).join("");
    const hasDocuments = dashboard.documents.length > 0;
    $("#documents-empty").hidden = hasDocuments;
    $("#documents-table-wrap").hidden = !hasDocuments;

    const reminderList = $("#reminder-list");
    reminderList.innerHTML = dashboard.reminders.map((reminder) => `
      <li class="reminder-row ${reminder.readAt ? "" : "reminder-unread"}">
        <span class="reminder-dot" aria-hidden="true"></span>
        <div class="reminder-copy">
          <p><a href="/document.html?id=${encodeURIComponent(reminder.document.id)}">${escapeHtml(reminder.document.title)}</a>
            <span>${reminder.daysBefore === 0 ? "expires today" : `expires in ${reminder.daysBefore} days`}</span></p>
          <time datetime="${escapeHtml(reminder.createdAt)}">${escapeHtml(formatTime(reminder.createdAt))}</time>
        </div>
        ${reminder.readAt ? '<span class="read-label">Read</span>' : `<button class="text-button" type="button" data-mark-read="${encodeURIComponent(reminder.id)}">Mark read</button>`}
      </li>`).join("");
    $("#reminders-empty").hidden = dashboard.reminders.length > 0;
    const unread = $("#unread-count");
    unread.textContent = dashboard.unreadReminders;
    unread.hidden = dashboard.unreadReminders === 0;
    reminderList.onclick = async (event) => {
      const button = event.target.closest("[data-mark-read]");
      if (!button) return;
      button.disabled = true;
      try {
        await api(`/api/reminders/${button.dataset.markRead}/read`, { method: "POST" });
        await dashboardPage();
      } catch (error) {
        button.disabled = false;
        showMessage(error.message);
      }
    };
  } catch (error) {
    showMessage(error.message);
  }
}

async function loadCategories(select, selectedValue) {
  const categories = await api("/api/documents/categories");
  for (const category of categories) {
    const option = document.createElement("option");
    option.value = category.value;
    option.textContent = category.label;
    option.selected = category.value === selectedValue;
    select.append(option);
  }
}

async function documentFormPage() {
  const user = await requireUser();
  if (!user) return;
  const form = $("#document-form");
  const id = new URLSearchParams(window.location.search).get("id");
  const editing = Boolean(id);
  const title = $("#form-title");
  const back = `/document.html?id=${encodeURIComponent(id || "")}`;
  if (editing) {
    title.textContent = "Edit document";
    $("#back-link").href = back;
    $("#cancel-link").href = back;
  }
  const params = new URLSearchParams(window.location.search);
  if (params.has("renewed")) showMessage("Renewal saved to your history.", "success");
  if (params.has("added")) showMessage("Document added to your records.", "success");
  try {
    await loadCategories($("#category"));
    if (editing) {
      const { document } = await api(`/api/documents/${encodeURIComponent(id)}`);
      for (const key of ["title", "category", "referenceNumber", "issueDate", "expiryDate", "notes"]) {
        const field = $(`[name="${key}"]`, form);
        field.value = document[key] || "";
      }
    }
  } catch (error) {
    showMessage(error.message);
    return;
  }
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    showFieldErrors();
    const data = new FormData(form);
    const issueDate = data.get("issueDate");
    const expiryDate = data.get("expiryDate");
    if (issueDate && expiryDate && issueDate > expiryDate) {
      showFieldErrors({ expiryDate: "Expiry date must be on or after the issue date." });
      return;
    }
    const payload = Object.fromEntries(data.entries());
    payload.issueDate = payload.issueDate || null;
    payload.referenceNumber = payload.referenceNumber || null;
    payload.notes = payload.notes || null;
    setBusy(form, true, "Saving…");
    try {
      const saved = await api(editing ? `/api/documents/${encodeURIComponent(id)}` : "/api/documents", {
        method: editing ? "PUT" : "POST",
        body: JSON.stringify(payload),
      });
      window.location.assign(`/document.html?id=${encodeURIComponent(saved.id)}${editing ? "" : "&added"}`);
    } catch (error) {
      showFieldErrors(error.fields);
      showMessage(error.message);
    } finally {
      setBusy(form, false);
    }
  });
}

async function documentPage() {
  const user = await requireUser();
  if (!user) return;
  const id = new URLSearchParams(window.location.search).get("id");
  if (!id || !/^\d+$/.test(id)) {
    window.location.replace("/not-found.html");
    return;
  }
  try {
    const { document, renewals } = await api(`/api/documents/${encodeURIComponent(id)}`);
    window.document.title = `${document.title} | Keepwell`;
    $("#document-category").textContent = document.categoryLabel;
    $("#document-title").textContent = document.title;
    $("#document-status").textContent = document.statusLabel;
    $("#document-status").classList.add(document.status);
    $("#expiry-date").textContent = formatDate(document.expiryDate, { day: "numeric", month: "long", year: "numeric" });
    $("#time-left").textContent = document.daysUntilExpiry < 0
      ? `${Math.abs(document.daysUntilExpiry)} days overdue`
      : document.daysUntilExpiry === 0 ? "Expires today" : `${document.daysUntilExpiry} days remaining`;
    $("#detail-category").textContent = document.categoryLabel;
    $("#reference-number").textContent = document.referenceNumber || "Not added";
    $("#issue-date").textContent = document.issueDate ? formatDate(document.issueDate) : "Not added";
    $("#created-date").textContent = formatDate(document.createdAt.slice(0, 10));
    $("#renew-link").href = `/renewal.html?id=${encodeURIComponent(id)}`;
    $("#edit-link").href = `/document-form.html?id=${encodeURIComponent(id)}`;
    $("#document-notes").textContent = document.notes || "";
    $("#notes-block").hidden = !document.notes;
    const history = $("#history-list");
    history.innerHTML = renewals.map((renewal) => `
      <li><span class="history-date">${escapeHtml(formatDate(renewal.renewedAt.slice(0, 10)))}</span>
        <div><strong>Expiry extended</strong>
          <p>${escapeHtml(formatDate(renewal.previousExpiryDate))} <span aria-hidden="true">&#8594;</span> ${escapeHtml(formatDate(renewal.newExpiryDate))}</p>
          ${renewal.notes ? `<p class="history-note">${escapeHtml(renewal.notes)}</p>` : ""}
        </div>
      </li>`).join("");
    $("#renewal-count").textContent = renewals.length;
    $("#history-empty").hidden = renewals.length > 0;
  } catch (error) {
    if (error.status === 404) window.location.replace("/not-found.html");
    else showMessage(error.message);
  }
}

async function renewalPage() {
  const user = await requireUser();
  if (!user) return;
  const id = new URLSearchParams(window.location.search).get("id");
  if (!id || !/^\d+$/.test(id)) {
    window.location.replace("/not-found.html");
    return;
  }
  const back = `/document.html?id=${encodeURIComponent(id)}`;
  $("#back-link").href = back;
  $("#cancel-link").href = back;
  try {
    const { document } = await api(`/api/documents/${encodeURIComponent(id)}`);
    $("#document-title").textContent = document.title;
    $("#current-expiry").textContent = formatDate(document.expiryDate);
    const nextExpiry = new Date(`${document.expiryDate}T12:00:00`);
    const originalMonth = nextExpiry.getMonth();
    nextExpiry.setFullYear(nextExpiry.getFullYear() + 1);
    if (nextExpiry.getMonth() !== originalMonth) nextExpiry.setDate(0);
    $("#expiryDate").value = [
      nextExpiry.getFullYear(),
      String(nextExpiry.getMonth() + 1).padStart(2, "0"),
      String(nextExpiry.getDate()).padStart(2, "0"),
    ].join("-");
  } catch (error) {
    if (error.status === 404) window.location.replace("/not-found.html");
    else showMessage(error.message);
    return;
  }
  $("#renewal-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    showFieldErrors();
    const form = event.currentTarget;
    const data = new FormData(form);
    setBusy(form, true, "Saving…");
    try {
      await api(`/api/documents/${encodeURIComponent(id)}/renew`, {
        method: "POST",
        body: JSON.stringify({ expiryDate: data.get("expiryDate"), notes: data.get("notes") || null }),
      });
      window.location.assign(`${back}&renewed`);
    } catch (error) {
      showFieldErrors(error.fields);
      showMessage(error.message);
    } finally {
      setBusy(form, false);
    }
  });
}

async function initialize() {
  for (const button of document.querySelectorAll("[data-logout]")) {
    button.addEventListener("click", async () => {
      button.disabled = true;
      try {
        await logout();
        window.location.assign("/?logout");
      } catch (error) {
        button.disabled = false;
        showMessage(error.message);
      }
    });
  }

  const page = document.body.dataset.page;
  const pages = {
    login: loginPage,
    register: registerPage,
    dashboard: dashboardPage,
    "document-form": documentFormPage,
    document: documentPage,
    renewal: renewalPage,
  };
  if (!pages[page]) return;
  try {
    await pages[page]();
  } catch (error) {
    showMessage(error.message);
  }
}

initialize();
