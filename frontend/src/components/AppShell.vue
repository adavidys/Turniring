<template>
  <div>
    <header class="nav-shell">
      <div class="nav-row">
        <RouterLink class="brand" to="/">
          <span class="brand-mark">T</span>
          <span>Turniring</span>
        </RouterLink>

        <nav class="nav-links">
          <RouterLink class="nav-link" to="/">Home</RouterLink>
          <RouterLink class="nav-link" to="/team" v-if="authStore.hasRole('TEAM', 'USER', 'ADMIN')">
            Team
          </RouterLink>
          <RouterLink class="nav-link" to="/jury" v-if="authStore.hasRole('JURY')">
            Jury
          </RouterLink>
          <RouterLink class="nav-link" to="/admin" v-if="authStore.hasRole('ADMIN', 'ORGANIZER')">
            Admin
          </RouterLink>
          <RouterLink class="nav-link" to="/profile" v-if="authStore.isLoggedIn.value">
            Profile
          </RouterLink>
        </nav>

        <div class="nav-actions">
          <template v-if="authStore.isLoggedIn.value">
            <span class="status-badge" data-tone="neutral">
              {{ authStore.role.value || "User" }}
            </span>
            <button class="btn-ghost" type="button" @click="handleLogout">Log out</button>
          </template>
          <template v-else>
            <RouterLink class="btn-ghost" to="/auth/login">Sign in</RouterLink>
            <RouterLink class="btn" to="/auth/register">Create account</RouterLink>
          </template>
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
import { RouterLink, useRouter } from "vue-router";
import { authStore } from "../services/auth";
import { notifier } from "../services/notify";
import ToastViewport from "./ToastViewport.vue";

const router = useRouter();

async function handleLogout() {
  await authStore.logout();
  notifier.pushNotification("Session closed.", "success");
  router.push("/");
}
</script>
