<template>
  <div>
    <header class="nav-shell" :class="{ 'is-open': isMobileMenuOpen }">
      <div class="nav-row">
        <RouterLink class="brand" to="/" @click="closeMobileMenu">
          <img class="brand-logo" :src="brandLogo" alt="Turniring logo" />
          <span>Turniring</span>
        </RouterLink>

        <button
          class="btn-ghost nav-menu-btn"
          type="button"
          aria-controls="site-nav-panel"
          :aria-expanded="isMobileMenuOpen"
          @click="toggleMobileMenu"
        >
          {{ isMobileMenuOpen ? t("nav.closeMenu") : t("nav.openMenu") }}
        </button>

        <div id="site-nav-panel" class="nav-panel" :class="{ 'is-open': isMobileMenuOpen }">
          <nav class="nav-links">
            <RouterLink v-for="item in visibleLinks" :key="item.to" class="nav-link" :to="item.to" @click="closeMobileMenu">
              {{ t(item.label) }}
            </RouterLink>
          </nav>

          <div class="nav-actions">
            <template v-if="authStore.isLoggedIn.value">
              <button class="btn-ghost nav-lang-btn" type="button" @click="toggleLang">
                {{ currentLangLabel }}
              </button>
              <button class="btn-ghost nav-theme-btn" type="button" @click="toggleTheme">
                {{ themeToggleLabel }}
              </button>
              <RouterLink class="btn-tonal nav-profile-btn" :to="workspaceRoute" @click="closeMobileMenu">
                {{ t("nav.workspace") }}
              </RouterLink>
              <RouterLink v-for="item in quickActionLinks" :key="item.to" class="btn-ghost" :to="item.to" @click="closeMobileMenu">
                {{ t(item.label) }}
              </RouterLink>
            </template>
            <template v-else>
              <button class="btn-ghost nav-lang-btn" type="button" @click="toggleLang">
                {{ currentLangLabel }}
              </button>
              <button class="btn-ghost nav-theme-btn" type="button" @click="toggleTheme">
                {{ themeToggleLabel }}
              </button>
              <RouterLink class="btn-ghost" to="/auth/login" @click="closeMobileMenu">{{ t("nav.signIn") }}</RouterLink>
              <RouterLink class="btn" to="/auth/register" @click="closeMobileMenu">{{ t("nav.createAccount") }}</RouterLink>
            </template>
          </div>
        </div>
      </div>
    </header>

    <main class="page-shell">
      <slot />
    </main>

    <ToastViewport />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { authStore } from "../services/auth";
import brandLogoDark from "../assets/turniring-logo.svg";
import brandLogoLight from "../assets/turniring-logo-light.svg";
import ToastViewport from "./ToastViewport.vue";
import { t, toggleLang, useLang } from "../services/i18n";
import { toggleTheme, useTheme } from "../services/theme";

const lang = useLang();
const theme = useTheme();
const route = useRoute();
const isMobileMenuOpen = ref(false);
let mobileViewportQuery = null;

const baseLinks = [
  { to: "/", label: "nav.home" },
  { to: "/olympiads/join", label: "nav.olympiads" },
  { to: "/teams/join", label: "nav.teams" }
];

const visibleLinks = computed(() => {
  const roleLinks = [];

  if (authStore.hasRole("TEAM", "USER")) {
    roleLinks.push({ to: "/team", label: "nav.team" });
  }
  if (authStore.hasRole("JURY")) {
    roleLinks.push({ to: "/jury", label: "nav.jury" });
  }
  if (authStore.hasRole("ADMIN", "ORGANIZER")) {
    roleLinks.push({ to: "/admin", label: "nav.admin" });
  }
  return [...baseLinks, ...roleLinks];
});

const quickActionLinks = computed(() => {
  const links = [];

  if (authStore.hasRole("ADMIN", "ORGANIZER")) {
    links.push({ to: "/olympiads/create", label: "nav.createOlympiad" });
  }

  return links;
});

const workspaceRoute = computed(() => "/profile");
const currentLangLabel = computed(() => (lang.value === "ua" ? t("nav.switchToEn") : t("nav.switchToUa")));
const themeToggleLabel = computed(() => (theme.value === "dark" ? t("nav.themeWhite") : t("nav.themeDark")));
const brandLogo = computed(() => (theme.value === "light" ? brandLogoLight : brandLogoDark));

function toggleMobileMenu() {
  isMobileMenuOpen.value = !isMobileMenuOpen.value;
}

function closeMobileMenu() {
  isMobileMenuOpen.value = false;
}

watch(
  () => route.fullPath,
  () => {
    closeMobileMenu();
  }
);

function handleViewportChange(event) {
  if (!event.matches) {
    closeMobileMenu();
  }
}

onMounted(() => {
  mobileViewportQuery = window.matchMedia("(max-width: 1100px)");
  mobileViewportQuery.addEventListener("change", handleViewportChange);
});

onBeforeUnmount(() => {
  mobileViewportQuery?.removeEventListener("change", handleViewportChange);
});
</script>
