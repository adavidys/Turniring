import { ref } from "vue";

const THEME_STORAGE_KEY = "turniring.theme";
const LIGHT_THEME = "light";
const DARK_THEME = "dark";

const initialTheme = resolveInitialTheme();
const theme = ref(initialTheme);

applyTheme(initialTheme);

function resolveInitialTheme() {
  const storedTheme = typeof window !== "undefined" ? window.localStorage.getItem(THEME_STORAGE_KEY) : null;
  if (storedTheme === LIGHT_THEME || storedTheme === DARK_THEME) {
    return storedTheme;
  }
  return DARK_THEME;
}

function applyTheme(nextTheme) {
  document.documentElement.dataset.theme = nextTheme;
}

function setTheme(nextTheme) {
  const normalizedTheme = nextTheme === LIGHT_THEME ? LIGHT_THEME : DARK_THEME;
  theme.value = normalizedTheme;
  window.localStorage.setItem(THEME_STORAGE_KEY, normalizedTheme);
  applyTheme(normalizedTheme);
}

function toggleTheme() {
  setTheme(theme.value === DARK_THEME ? LIGHT_THEME : DARK_THEME);
}

function useTheme() {
  return theme;
}

export { toggleTheme, useTheme };
