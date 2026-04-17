import { computed, reactive } from "vue";
import { api, TOKEN_KEY } from "./api";

const state = reactive({
  token: localStorage.getItem(TOKEN_KEY),
  user: null,
  profile: null,
  ready: false,
  restoring: false
});

function setToken(token) {
  state.token = token || null;
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

async function refreshProfile() {
  state.profile = await api.profile.me();
  return state.profile;
}

async function restoreSession(force = false) {
  if (state.restoring) {
    return;
  }
  if (state.ready && !force) {
    return;
  }

  state.restoring = true;
  try {
    const user = await api.auth.me();
    state.user = user;
    try {
      await refreshProfile();
    } catch {
      state.profile = null;
    }
  } catch {
    clearAuthState();
  } finally {
    state.ready = true;
    state.restoring = false;
  }
}

async function login(credentials) {
  const user = await api.auth.login(credentials);
  setToken(user.token);
  state.user = user;
  await refreshProfile();
  state.ready = true;
  return user;
}

async function register(payload) {
  const user = await api.auth.register(payload);
  setToken(user.token);
  state.user = user;
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
  state.user = null;
  state.profile = null;
}

const role = computed(() => state.profile?.role || state.user?.role || null);
const isLoggedIn = computed(() => Boolean(state.user));

export const authStore = {
  state,
  role,
  isLoggedIn,
  restoreSession,
  refreshProfile,
  login,
  register,
  logout,
  clearAuthState,
  hasRole(...roles) {
    return roles.includes(role.value);
  }
};
