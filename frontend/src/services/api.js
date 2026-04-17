const TOKEN_KEY = "turniring.jwt";

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

async function parseResponse(response) {
  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    return response.json();
  }
  if (contentType.includes("text/")) {
    return response.text();
  }
  return null;
}

async function request(path, options = {}) {
  const { method = "GET", body, headers } = options;
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || ""}${path}`, {
    method,
    credentials: "include",
    headers: buildHeaders(body, headers),
    body: body && !(body instanceof FormData) ? JSON.stringify(body) : body
  });

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
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || ""}${path}`, {
    credentials: "include",
    headers: buildHeaders(null)
  });

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
    tournament: (id) => request(`/api/public/tournaments/${id}`),
    teams: (id) => request(`/api/public/tournaments/${id}/teams`),
    tasks: (id) => request(`/api/public/tournaments/${id}/tasks`),
    announcements: (id) => request(`/api/public/tournaments/${id}/announcements`),
    schedule: (id) => request(`/api/public/tournaments/${id}/schedule`),
    leaderboard: (id) => request(`/api/public/tournaments/${id}/leaderboard`)
  },
  profile: {
    me: () => request("/api/profile/me")
  },
  team: {
    myTeams: () => request("/api/team/teams/my"),
    registerTeam: (tournamentId, body) =>
      request(`/api/team/tournaments/${tournamentId}/registration`, { method: "POST", body }),
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
    updateTournamentStatus: (id, status) =>
      request(`/api/admin/tournaments/${id}/status/${status}`, { method: "POST" }),
    createTask: (tournamentId, body) =>
      request(`/api/admin/tournaments/${tournamentId}/tasks`, { method: "POST", body }),
    updateTaskStatus: (taskId, status) => request(`/api/admin/tasks/${taskId}/status/${status}`, { method: "POST" }),
    listSubmissions: (tournamentId) => request(`/api/admin/tournaments/${tournamentId}/submissions`),
    assignEvaluations: (taskId, body) => request(`/api/admin/tasks/${taskId}/assignments`, { method: "POST", body }),
    finishEvaluation: (taskId) => request(`/api/admin/tasks/${taskId}/finish-evaluation`, { method: "POST" }),
    exportLeaderboard: (tournamentId) => download(`/api/admin/tournaments/${tournamentId}/leaderboard/export`),
    createAnnouncement: (tournamentId, body) =>
      request(`/api/admin/tournaments/${tournamentId}/announcements`, { method: "POST", body }),
    createScheduleEvent: (tournamentId, body) =>
      request(`/api/admin/tournaments/${tournamentId}/schedule`, { method: "POST", body }),
    createUser: (body) => request("/api/admin/users", { method: "POST", body })
  }
};

export { TOKEN_KEY };
