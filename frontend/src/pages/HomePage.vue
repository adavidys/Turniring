<template>
  <div class="page-grid">
    <section class="hero-grid">
      <article class="hero-card stack">
        <span class="eyebrow">Tournament Control Center</span>
        <h1 class="title-xl">Run a coding tournament without spreadsheet chaos.</h1>
        <p class="text-soft">
          Turniring handles registration windows, rounds, submissions, jury review, rankings,
          and role-specific workspaces in one place.
        </p>
        <div class="btn-row">
          <RouterLink class="btn" to="/auth/register" v-if="!authStore.isLoggedIn.value">Start as a team</RouterLink>
          <RouterLink class="btn" to="/team" v-else-if="authStore.hasRole('TEAM', 'USER', 'ADMIN')">Open team workspace</RouterLink>
          <RouterLink class="btn" to="/jury" v-else-if="authStore.hasRole('JURY')">Review assignments</RouterLink>
          <RouterLink class="btn" to="/admin" v-else-if="authStore.hasRole('ADMIN', 'ORGANIZER')">Open admin panel</RouterLink>
          <RouterLink class="btn-ghost" to="/profile" v-if="authStore.isLoggedIn.value">Profile</RouterLink>
          <RouterLink class="btn-ghost" to="/auth/login" v-else>Sign in</RouterLink>
        </div>
        <div class="stat-row">
          <span class="stat-chip">{{ home.registrationOpen.length }} registration-ready tournaments</span>
          <span class="stat-chip">{{ home.running.length }} running now</span>
          <span class="stat-chip">{{ home.finished.length }} archived finals</span>
        </div>
      </article>

      <aside class="spot-card">
        <span class="eyebrow">Platform map</span>
        <h2 class="title-lg">One backend, three focused workspaces.</h2>
        <div class="pill-row">
          <span class="pill">Admin: orchestration</span>
          <span class="pill">Team: delivery</span>
          <span class="pill">Jury: evaluation</span>
        </div>
        <p class="text-soft">
          The frontend talks directly to the Spring API and supports token auth plus cookie session fallback.
        </p>
      </aside>
    </section>

    <div v-if="loading" class="panel">Loading tournaments…</div>
    <div v-else class="page-grid">
      <SectionBlock
        title="Registration Open"
        description="Teams can register right now. These tournaments are actively taking rosters."
        eyebrow="Now"
      >
        <div v-if="home.registrationOpen.length" class="grid-auto">
          <TournamentCard v-for="tournament in home.registrationOpen" :key="tournament.id" :tournament="tournament" />
        </div>
        <div v-else class="empty-box">No active registration windows right now.</div>
      </SectionBlock>

      <SectionBlock
        title="Running Tournaments"
        description="Rounds are live, submissions are flowing, and jury work is in motion."
        eyebrow="Live"
      >
        <div v-if="home.running.length" class="grid-auto">
          <TournamentCard v-for="tournament in home.running" :key="tournament.id" :tournament="tournament" />
        </div>
        <div v-else class="empty-box">No tournaments are currently marked as running.</div>
      </SectionBlock>

      <SectionBlock
        title="Finished Tournaments"
        description="Browse past events, inspect leaderboards, and review what strong teams delivered."
        eyebrow="Archive"
      >
        <div v-if="home.finished.length" class="grid-auto">
          <TournamentCard v-for="tournament in home.finished" :key="tournament.id" :tournament="tournament" />
        </div>
        <div v-else class="empty-box">No finished tournaments yet.</div>
      </SectionBlock>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import TournamentCard from "../components/TournamentCard.vue";
import SectionBlock from "../components/SectionBlock.vue";
import { api } from "../services/api";
import { authStore } from "../services/auth";
import { notifier } from "../services/notify";
import { getErrorMessage } from "../services/formatters";

const loading = ref(true);
const home = reactive({
  registrationOpen: [],
  running: [],
  finished: []
});

async function loadHome() {
  loading.value = true;
  try {
    Object.assign(home, await api.public.home());
  } catch (error) {
    notifier.pushNotification(getErrorMessage(error), "error");
  } finally {
    loading.value = false;
  }
}

onMounted(loadHome);
</script>
