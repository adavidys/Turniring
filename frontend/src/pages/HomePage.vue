<template>
  <div class="page-grid">
    <section class="hero-grid">
      <article class="hero-card stack">
        <span class="eyebrow">{{ t("home.eyebrow") }}</span>
        <h1 class="title-xl">{{ t("home.title") }}</h1>
        <p class="text-soft">{{ t("home.copy") }}</p>
        <div class="btn-row">
          <RouterLink class="btn" to="/auth/register" v-if="!authStore.isLoggedIn.value">{{ t("home.startTeam") }}</RouterLink>
          <RouterLink class="btn" :to="workspaceRoute" v-else>{{ workspaceLabel }}</RouterLink>
          <RouterLink class="btn-ghost" to="/profile" v-if="authStore.isLoggedIn.value">{{ t("home.profile") }}</RouterLink>
          <RouterLink class="btn-ghost" to="/auth/login" v-else>{{ t("home.signIn") }}</RouterLink>
        </div>
        <div class="stat-row">
          <span class="stat-chip">{{ home.registrationOpen.length }} {{ t("home.stats.registration") }}</span>
          <span class="stat-chip">{{ home.running.length }} {{ t("home.stats.running") }}</span>
          <span class="stat-chip">{{ home.finished.length }} {{ t("home.stats.finished") }}</span>
        </div>
      </article>

      <aside class="spot-card">
        <span class="eyebrow">{{ t("home.platformEyebrow") }}</span>
        <h2 class="title-lg">{{ t("home.platformTitle") }}</h2>
        <div class="pill-row">
          <span class="pill">{{ t("home.platformAdmin") }}</span>
          <span class="pill">{{ t("home.platformTeam") }}</span>
          <span class="pill">{{ t("home.platformJury") }}</span>
        </div>
        <p class="text-soft">{{ t("home.platformCopy") }}</p>
      </aside>
    </section>

    <section class="panel stack-sm">
      <div class="toolbar">
        <h2 class="title-sm">{{ t("home.filtersTitle") }}</h2>
      </div>
      <div class="btn-row">
        <button class="btn-ghost" :class="{ 'is-active': filters.registration }" type="button" @click="filters.registration = !filters.registration">
          {{ t("home.filterRegistration") }}
        </button>
        <button class="btn-ghost" :class="{ 'is-active': filters.running }" type="button" @click="filters.running = !filters.running">
          {{ t("home.filterRunning") }}
        </button>
        <button class="btn-ghost" :class="{ 'is-active': filters.finished }" type="button" @click="filters.finished = !filters.finished">
          {{ t("home.filterFinished") }}
        </button>
      </div>
    </section>

    <SectionBlock
      v-if="showTeamSnapshot"
      :title="t('home.quickTitle')"
      :description="t('home.quickDesc')"
      :eyebrow="t('home.quickEyebrow')"
    >
      <template v-if="teamSnapshot.team">
        <div class="stat-row">
          <span class="stat-chip">{{ t("home.quickTeam") }}: {{ teamSnapshot.team.name }}</span>
          <span class="stat-chip">
            {{ t("home.quickTournament") }}:
            {{ teamSnapshot.team.tournamentId ? `#${teamSnapshot.team.tournamentId}` : tx("не приєднано", "not joined") }}
          </span>
        </div>
        <div class="empty-box" v-if="!teamSnapshot.team.tournamentId">
          {{ tx("Команда ще не приєднана до олімпіади.", "Team is not joined to an olympiad yet.") }}
        </div>
        <div class="meta-list" v-if="teamSnapshot.task">
          <div class="meta-row">
            <span class="text-soft">{{ t("home.quickTask") }}</span>
            <strong>{{ teamSnapshot.task.title }}</strong>
          </div>
          <div class="meta-row">
            <span class="text-soft">{{ t("home.quickDeadline") }}</span>
            <strong>{{ formatDateTime(teamSnapshot.task.deadlineAt) }}</strong>
          </div>
          <div class="meta-row" v-if="safeSubmissionGithubUrl">
            <span class="text-soft">{{ t("home.quickSubmission") }}</span>
            <a :href="safeSubmissionGithubUrl" target="_blank" rel="noreferrer">{{ teamSnapshot.submission.githubUrl }}</a>
          </div>
        </div>
        <div v-else-if="teamSnapshot.team.tournamentId" class="empty-box">{{ t("common.none") }}</div>
        <div class="empty-box" v-if="teamSnapshot.task && !teamSnapshot.submission">{{ t("home.quickMissing") }}</div>
        <div class="btn-row">
          <RouterLink class="btn-tonal" to="/team">{{ t("home.openWorkspace") }}</RouterLink>
        </div>
      </template>
      <div v-else class="empty-box">{{ t("home.quickNoTeams") }}</div>
    </SectionBlock>

    <div v-if="loading" class="panel">{{ t("home.loading") }}</div>
    <div v-else class="page-grid">
      <SectionBlock
        :title="tx('Рекомендовані олімпіади', 'Recommended olympiads')"
        :description="tx('Добірка на основі лайків, активності команд і найближчих стартів.', 'Picked by likes, team activity, and upcoming starts.')"
        :eyebrow="tx('Рекомендації', 'Recommendations')"
      >
        <div v-if="home.recommended.length" class="grid-auto">
          <TournamentCard
            v-for="tournament in home.recommended"
            :key="tournament.id"
            :tournament="tournament"
            @like-change="updateTournament"
          />
        </div>
        <div v-else class="empty-box">{{ tx("Поки що недостатньо даних для рекомендацій.", "Not enough data for recommendations yet.") }}</div>
      </SectionBlock>

      <SectionBlock
        v-if="filters.registration"
        :title="t('home.section.registrationTitle')"
        :description="t('home.section.registrationDesc')"
        :eyebrow="t('home.section.registrationEyebrow')"
      >
        <div v-if="filteredRegistration.length" class="grid-auto">
          <TournamentCard
            v-for="tournament in filteredRegistration"
            :key="tournament.id"
            :tournament="tournament"
            @like-change="updateTournament"
          />
        </div>
        <div v-else class="empty-box">{{ t("empty.registration") }}</div>
      </SectionBlock>

      <SectionBlock
        v-if="filters.running"
        :title="t('home.section.runningTitle')"
        :description="t('home.section.runningDesc')"
        :eyebrow="t('home.section.runningEyebrow')"
      >
        <div v-if="filteredRunning.length" class="grid-auto">
          <TournamentCard
            v-for="tournament in filteredRunning"
            :key="tournament.id"
            :tournament="tournament"
            @like-change="updateTournament"
          />
        </div>
        <div v-else class="empty-box">{{ t("empty.running") }}</div>
      </SectionBlock>

      <SectionBlock
        v-if="filters.finished"
        :title="t('home.section.finishedTitle')"
        :description="t('home.section.finishedDesc')"
        :eyebrow="t('home.section.finishedEyebrow')"
      >
        <div v-if="filteredFinished.length" class="grid-auto">
          <TournamentCard
            v-for="tournament in filteredFinished"
            :key="tournament.id"
            :tournament="tournament"
            @like-change="updateTournament"
          />
        </div>
        <div v-else class="empty-box">{{ t("empty.finished") }}</div>
      </SectionBlock>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import TournamentCard from "../components/TournamentCard.vue";
import SectionBlock from "../components/SectionBlock.vue";
import { api } from "../services/api";
import { authStore } from "../services/auth";
import { notifier } from "../services/notify";
import { formatDateTime, getErrorMessage } from "../services/formatters";
import { t, tx } from "../services/i18n";
import { toSafeExternalUrl } from "../services/security";

const loading = ref(true);
const home = reactive({
  recommended: [],
  registrationOpen: [],
  running: [],
  finished: []
});

const filters = reactive({
  registration: true,
  running: true,
  finished: true
});

const teamSnapshot = reactive({
  team: null,
  task: null,
  submission: null
});

const filteredRegistration = computed(() => home.registrationOpen);
const filteredRunning = computed(() => home.running);
const filteredFinished = computed(() => home.finished);
const showTeamSnapshot = computed(() => authStore.isLoggedIn.value && authStore.hasRole("TEAM", "USER"));
const workspaceRoute = computed(() => authStore.resolveWorkspaceRoute(authStore.role.value));
const safeSubmissionGithubUrl = computed(() => toSafeExternalUrl(teamSnapshot.submission?.githubUrl));
const workspaceLabel = computed(() => {
  if (authStore.hasRole("ADMIN", "ORGANIZER")) {
    return t("home.openAdmin");
  }
  if (authStore.hasRole("JURY")) {
    return t("home.openJury");
  }
  return t("home.openTeam");
});

async function loadTeamSnapshot() {
  teamSnapshot.team = null;
  teamSnapshot.task = null;
  teamSnapshot.submission = null;

  if (!showTeamSnapshot.value) {
    return;
  }

  const teams = await api.team.myTeams();
  if (!teams.length) {
    return;
  }

  const team = teams.find((item) => item.tournamentId) || teams[0];
  teamSnapshot.team = team;
  if (!team.tournamentId) {
    return;
  }

  const tasks = await api.team.tasks(team.tournamentId);
  if (!tasks.length) {
    return;
  }

  const activeTask = tasks.find((task) => task.status === "ACTIVE") || tasks[0];
  teamSnapshot.task = activeTask;
  try {
    teamSnapshot.submission = await api.team.getSubmission(activeTask.id);
  } catch (error) {
    if (error.status !== 404) {
      throw error;
    }
  }
}

async function loadHome() {
  loading.value = true;
  try {
    Object.assign(home, await api.public.home());
    await loadTeamSnapshot();
  } catch (error) {
    notifier.pushNotification(getErrorMessage(error), "error");
  } finally {
    loading.value = false;
  }
}

function updateTournament(updatedTournament) {
  for (const key of ["recommended", "registrationOpen", "running", "finished"]) {
    const index = home[key].findIndex((tournament) => tournament.id === updatedTournament.id);
    if (index >= 0) {
      home[key][index] = updatedTournament;
    }
  }
}

onMounted(loadHome);
</script>
