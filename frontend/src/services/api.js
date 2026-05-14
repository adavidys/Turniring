const TOKEN_KEY = "turniring.jwt";
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";
const CSRF_COOKIE_KEY = "XSRF-TOKEN";
const CSRF_HEADER_KEY = "X-XSRF-TOKEN";
const CSRF_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);
let csrfPromise = null;

function buildHeaders(body, headers = {}) {
  const nextHeaders = { ...headers };
  if (body && !(body instanceof FormData) && !nextHeaders["Content-Type"]) {
    nextHeaders["Content-Type"] = "application/json";
  }

  const token = localStorage.getItem(TOKEN_KEY);
  if (token && !nextHeaders.Authorization) {
    nextHeaders.Authorization = `Bearer ${token}`;
  }

  return nextHeaders;
}

function readCookie(name) {
  if (typeof document === "undefined") {
    return "";
  }
  const escapedName = name.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const match = document.cookie.match(new RegExp(`(?:^|; )${escapedName}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : "";
}

function isCsrfMethod(method) {
  return CSRF_METHODS.has(String(method || "GET").toUpperCase());
}

async function requestCsrfToken() {
  if (csrfPromise) {
    return csrfPromise;
  }

  csrfPromise = (async () => {
    let response;
    try {
      response = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
        method: "GET",
        credentials: "include",
        headers: buildHeaders(null)
      });
    } catch (cause) {
      const error = new Error("Server is unavailable. Please try again.");
      error.status = 0;
      error.cause = cause;
      throw error;
    }

    const payload = await parseResponse(response);
    if (!response.ok) {
      const message =
        typeof payload === "object" && payload !== null
          ? payload.message || payload.error || "Failed to load CSRF token"
          : payload || "Failed to load CSRF token";
      const error = new Error(message);
      error.status = response.status;
      throw error;
    }

    const token = payload?.token || readCookie(CSRF_COOKIE_KEY);
    if (!token) {
      throw new Error("Failed to load CSRF token");
    }
    return token;
  })().finally(() => {
    csrfPromise = null;
  });

  return csrfPromise;
}

async function ensureCsrfToken() {
  const cookieToken = readCookie(CSRF_COOKIE_KEY);
  if (cookieToken) {
    return cookieToken;
  }
  return requestCsrfToken();
}

async function parseResponse(response) {
  if (response.status === 204 || response.status === 205) {
    return null;
  }

  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    const raw = await response.text();
    if (!raw) {
      return null;
    }
    return JSON.parse(raw);
  }
  if (contentType.includes("text/")) {
    return response.text();
  }
  return null;
}

async function request(path, options = {}) {
  const { method = "GET", body, headers } = options;
  const normalizedMethod = method.toUpperCase();
  const requestHeaders = buildHeaders(body, headers);

  if (isCsrfMethod(normalizedMethod) && !requestHeaders[CSRF_HEADER_KEY]) {
    requestHeaders[CSRF_HEADER_KEY] = await ensureCsrfToken();
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: normalizedMethod,
      credentials: "include",
      headers: requestHeaders,
      body: body && !(body instanceof FormData) ? JSON.stringify(body) : body
    });
  } catch (cause) {
    const error = new Error("Server is unavailable. Please try again.");
    error.status = 0;
    error.cause = cause;
    throw error;
  }

  const payload = await parseResponse(response);

  if (!response.ok) {
    const message =
      typeof payload === "object" && payload !== null
        ? payload.message || payload.error || "Request failed"
        : payload || "Request failed";

    const error = new Error(message);
    error.status = response.status;
    error.details = payload?.details || [];
    error.payload = payload;
    throw error;
  }

  return payload;
}

async function download(path) {
  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      credentials: "include",
      headers: buildHeaders(null)
    });
  } catch (cause) {
    const error = new Error("Server is unavailable. Please try again.");
    error.status = 0;
    error.cause = cause;
    throw error;
  }

  if (!response.ok) {
    const payload = await parseResponse(response);
    const error = new Error(payload?.message || "Download failed");
    error.status = response.status;
    throw error;
  }

  const blob = await response.blob();
  return {
    blob,
    filename:
      response.headers
        .get("content-disposition")
        ?.split("filename=")
        ?.at(1)
        ?.replace(/"/g, "") || "export.csv"
  };
}

export const api = {
  auth: {
    register: (body) => request("/api/auth/registration", { method: "POST", body }),
    login: (body) => request("/api/auth/login", { method: "POST", body }),
    me: () => request("/api/auth/me"),
    logout: () => request("/api/auth/logout", { method: "POST" })
  },
  public: {
    home: () => request("/api/public/home"),
    tournaments: (status) => request(status ? `/api/public/tournaments?status=${status}` : "/api/public/tournaments"),
    recommendedTournaments: () => request("/api/public/tournaments/recommended"),
    tournament: (id) => request(`/api/public/tournaments/${id}`),
    likeTournament: (id) => request(`/api/public/tournaments/${id}/like`, { method: "POST" }),
    unlikeTournament: (id) => request(`/api/public/tournaments/${id}/like`, { method: "DELETE" }),
    teams: (id) => request(`/api/public/tournaments/${id}/teams`),
    invite: (token) => request(`/api/public/invites/${token}`),
    tasks: (id) => request(`/api/public/tournaments/${id}/tasks`),
    announcements: (id) => request(`/api/public/tournaments/${id}/announcements`),
    schedule: (id) => request(`/api/public/tournaments/${id}/schedule`),
    leaderboard: (id) => request(`/api/public/tournaments/${id}/leaderboard`)
  },
  profile: {
    me: () => request("/api/profile/me"),
    updateMe: (body) => request("/api/profile/me", { method: "PUT", body }),
    changeRole: (body) => request("/api/profile/me/role", { method: "PUT", body }),
    acceptInvite: (token) => request(`/api/profile/invites/${token}/accept`, { method: "POST" })
  },
  team: {
    myTeams: () => request("/api/team/teams/my"),
    createTeam: (body) => request("/api/team/teams", { method: "POST", body }),
    joinTeam: (teamId, tournamentId) => request(`/api/team/teams/${teamId}/join/${tournamentId}`, { method: "POST" }),
    leaveTeam: (teamId) => request(`/api/team/teams/${teamId}/leave`, { method: "POST" }),
    deleteTeam: (teamId) => request(`/api/team/teams/${teamId}`, { method: "DELETE" }),
    updateTeam: (teamId, body) => request(`/api/team/teams/${teamId}`, { method: "PUT", body }),
    tasks: (tournamentId) => request(`/api/team/tournaments/${tournamentId}/tasks`),
    getSubmission: (taskId) => request(`/api/team/tasks/${taskId}/submission`),
    saveSubmission: (taskId, body) => request(`/api/team/tasks/${taskId}/submission`, { method: "PUT", body })
  },
  jury: {
    assignments: () => request("/api/jury/assignments"),
    submitEvaluation: (assignmentId, body) =>
      request(`/api/jury/assignments/${assignmentId}/evaluation`, { method: "POST", body })
  },
  admin: {
    createTournament: (body) => request("/api/admin/tournaments", { method: "POST", body }),
    updateTournament: (id, body) => request(`/api/admin/tournaments/${id}`, { method: "PUT", body }),
    deleteTournament: (id, body) => request(`/api/admin/tournaments/${id}`, { method: "DELETE", body }),
    updateTournamentStatus: (id, status) =>
      request(`/api/admin/tournaments/${id}/status/${status}`, { method: "POST" }),
    createTask: (tournamentId, body) =>
      request(`/api/admin/tournaments/${tournamentId}/tasks`, { method: "POST", body }),
    teams: (tournamentId) => request(`/api/admin/tournaments/${tournamentId}/teams`),
    updateTaskStatus: (taskId, status) => request(`/api/admin/tasks/${taskId}/status/${status}`, { method: "POST" }),
    listSubmissions: (tournamentId) => request(`/api/admin/tournaments/${tournamentId}/submissions`),
    assignEvaluations: (taskId, body) => request(`/api/admin/tasks/${taskId}/assignments`, { method: "POST", body }),
    finishEvaluation: (taskId) => request(`/api/admin/tasks/${taskId}/finish-evaluation`, { method: "POST" }),
    exportLeaderboard: (tournamentId) => download(`/api/admin/tournaments/${tournamentId}/leaderboard/export`),
    createAnnouncement: (tournamentId, body) =>
      request(`/api/admin/tournaments/${tournamentId}/announcements`, { method: "POST", body }),
    createScheduleEvent: (tournamentId, body) =>
      request(`/api/admin/tournaments/${tournamentId}/schedule`, { method: "POST", body }),
    createUser: (body) => request("/api/admin/users", { method: "POST", body }),
    createJuryInvite: () => request("/api/admin/invites/jury", { method: "POST" }),
    createTeamInvite: (teamId) => request(`/api/admin/invites/teams/${teamId}`, { method: "POST" })
  }
};

export { TOKEN_KEY };
