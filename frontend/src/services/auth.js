import { computed, reactive } from "vue";
import { api, TOKEN_KEY } from "./api";

const USER_KEY = "turniring.user";
const PROFILE_KEY = "turniring.profile";

function readCachedJson(key) {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw);
    return typeof parsed === "object" && parsed !== null ? parsed : null;
  } catch {
    return null;
  }
}

function writeCachedJson(key, value) {
  if (!value) {
    localStorage.removeItem(key);
    return;
  }
  localStorage.setItem(key, JSON.stringify(value));
}

function isAuthorizationError(error) {
  return error?.status === 401 || error?.status === 403;
}

const persistedToken = localStorage.getItem(TOKEN_KEY);
if (!persistedToken) {
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(PROFILE_KEY);
}

const state = reactive({
  token: persistedToken,
  user: persistedToken ? readCachedJson(USER_KEY) : null,
  profile: persistedToken ? readCachedJson(PROFILE_KEY) : null,
  ready: false,
  restoring: false
});

function resolveWorkspaceRoute(role) {
  if (role === "ADMIN" || role === "ORGANIZER") {
    return "/admin";
  }
  if (role === "JURY") {
    return "/jury";
  }
  if (role === "TEAM" || role === "USER") {
    return "/team";
  }
  return "/profile";
}

function setToken(token) {
  state.token = token || null;
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(PROFILE_KEY);
  }
}

function setUser(user) {
  state.user = user || null;
  writeCachedJson(USER_KEY, state.user);
}

function setProfile(profile) {
  state.profile = profile || null;
  writeCachedJson(PROFILE_KEY, state.profile);
}

async function refreshProfile() {
  setProfile(await api.profile.me());
  return state.profile;
}

async function changeRole(role) {
  setProfile(await api.profile.changeRole({ role }));
  if (state.user) {
    setUser({ ...state.user, role: state.profile.role });
  }
  return state.profile;
}

async function updateProfileData(payload) {
  setProfile(await api.profile.updateMe(payload));
  if (state.user) {
    setUser({
      ...state.user,
      name: state.profile.name,
      lastName: state.profile.lastName,
      email: state.profile.email,
      role: state.profile.role
    });
  }
  return state.profile;
}

async function restoreSession(force = false) {
  if (state.restoring) {
    return;
  }
  if (state.ready && !force) {
    return;
  }
  if (!state.token) {
    clearAuthState();
    state.ready = true;
    return;
  }

  state.restoring = true;
  try {
    const user = await api.auth.me();
    setUser(user);
    try {
      await refreshProfile();
    } catch (error) {
      if (isAuthorizationError(error)) {
        clearAuthState();
      } else if (!state.profile) {
        setProfile(null);
      }
    }
  } catch (error) {
    if (isAuthorizationError(error)) {
      clearAuthState();
    }
  } finally {
    state.ready = true;
    state.restoring = false;
  }
}

async function login(credentials) {
  const user = await api.auth.login(credentials);
  setToken(user.token);
  setUser(user);
  await refreshProfile();
  state.ready = true;
  return user;
}

async function register(payload) {
  const user = await api.auth.register(payload);
  setToken(user.token);
  setUser(user);
  await refreshProfile();
  state.ready = true;
  return user;
}

async function logout() {
  try {
    await api.auth.logout();
  } finally {
    clearAuthState();
  }
}

function clearAuthState() {
  setToken(null);
  setUser(null);
  setProfile(null);
}

const role = computed(() => state.profile?.role || state.user?.role || null);
const isLoggedIn = computed(() => Boolean(state.token && state.user));

export const authStore = {
  state,
  role,
  isLoggedIn,
  restoreSession,
  refreshProfile,
  changeRole,
  updateProfileData,
  login,
  register,
  logout,
  clearAuthState,
  resolveWorkspaceRoute,
  hasRole(...roles) {
    return roles.includes(role.value);
  }
};
