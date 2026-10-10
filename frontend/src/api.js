const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080").replace(/\/+$/, "");

async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`${apiBaseUrl}${path}`, {
      ...options,
      credentials: "include",
      headers: {
        Accept: "application/json",
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...options.headers,
      },
    });
  } catch {
    throw new Error("Couldn't reach Keepwell. Check your connection and try again.");
  }

  if (response.status === 204) return null;
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    const error = new Error(body?.error || `Request failed (${response.status}).`);
    error.status = response.status;
    error.fields = body?.fields || {};
    throw error;
  }
  return body;
}

export async function api(path, options = {}) {
  const method = (options.method || "GET").toUpperCase();
  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    const csrf = await request("/api/auth/csrf");
    options = { ...options, headers: { ...options.headers, "X-CSRF-TOKEN": csrf.token } };
  }
  return request(path, options);
}

export async function currentUser() {
  return api("/api/auth/me");
}

export async function logout() {
  return api("/api/auth/logout", { method: "POST" });
}
