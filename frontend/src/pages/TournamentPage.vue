<template>
  <div class="page-grid">
      <div v-if="loading" class="panel">{{ t("common.loadingTournament") }}</div>
    <div v-else-if="errorMessage" class="error-box">{{ errorMessage }}</div>
    <template v-else-if="tournament">
      <section class="hero-card stack">
        <div class="toolbar">
          <div class="stack-sm">
              <span class="eyebrow">{{ t("tournament.eyebrow") }}</span>
            <h1 class="title-lg">{{ tournament.title }}</h1>
            <p class="text-soft">{{ tournament.description }}</p>
          </div>
          <div class="stack-sm">
            <StatusBadge :status="tournament.status" />
            <RouterLink v-if="canEditTournament" class="btn-tonal" :to="`/admin?tournamentId=${tournament.id}`">
              {{ t("admin.editTournament") }}
            </RouterLink>
          </div>
        </div>

        <div class="stat-row">
          <span class="stat-chip">{{ t("tournament.registrationLabel") }} {{ tournament.registrationOpen ? t("tournament.registrationOpen") : t("tournament.registrationClosed") }}</span>
          <span class="stat-chip">{{ tournament.registeredTeams }} {{ t("tournament.teams") }}</span>
          <span class="stat-chip">{{ tournament.minimumRounds }} {{ t("tournament.rounds") }}</span>
          <span class="stat-chip">{{ tournament.teamMinMembers }}–{{ tournament.teamMaxMembers }} {{ tx("учасників", "members") }}</span>
        </div>

        <div class="split">
          <div class="panel stack-sm">
              <h2 class="title-sm">{{ t("tournament.rules") }}</h2>
              <p class="text-soft">{{ tournament.rules || t("common.none") }}</p>
          </div>
          <div class="panel stack-sm">
              <h2 class="title-sm">{{ t("tournament.schedule") }}</h2>
              <p class="text-soft">{{ t("tournament.registrationWindow") }}: {{ formatDateTime(tournament.registrationStartAt) }} → {{ formatDateTime(tournament.registrationEndAt) }}</p>
              <p class="text-soft">{{ t("tournament.start") }}: {{ formatDateTime(tournament.startAt) }}</p>
          </div>
        </div>
      </section>

      <SectionBlock
        v-if="authStore.isLoggedIn.value && tournament.registrationOpen"
        :title="t('tournament.registerTeam')"
        :description="t('tournament.registerTeamDesc')"
        :eyebrow="tx('Командна участь', 'Team participation')"
      >
        <div class="stack">
          <div v-if="loadingMyTeams" class="panel">{{ t("common.loading") }}</div>

          <div v-else-if="alreadyJoinedTeam" class="success-box">
            {{ tx("Ви вже приєднали команду", "You already joined team") }} «{{ alreadyJoinedTeam.name }}» {{ tx("до цієї олімпіади.", "to this olympiad.") }}
          </div>

          <template v-else-if="joinableTeams.length">
            <div class="field">
              <label>{{ t("nav.team") }}</label>
              <select v-model.number="selectedTeamIdForJoin">
                <option :value="null">{{ tx("Оберіть команду", "Select team") }}</option>
                <option v-for="team in joinableTeams" :key="team.id" :value="team.id">
                  {{ team.name }}
                </option>
              </select>
            </div>
            <div class="btn-row">
              <button class="btn" type="button" :disabled="registeringTeam || !selectedTeamIdForJoin" @click="handleTeamRegistration">
                {{ registeringTeam ? t("tournament.registering") : t("tournament.register") }}
              </button>
              <RouterLink class="btn-ghost" to="/team">{{ t("nav.team") }}</RouterLink>
            </div>
          </template>

          <div v-else class="empty-box">
            {{ tx("У вас немає вільних команд. Створіть команду, а потім приєднайте її до олімпіади.", "You have no available teams. Create a team, then join it to the olympiad.") }}
          </div>

          <div class="success-box" v-if="registrationMessage">{{ registrationMessage }}</div>
          <div class="error-box" v-if="registrationError">{{ registrationError }}</div>
        </div>
      </SectionBlock>

      <SectionBlock :title="t('tournament.tasks')" :description="t('tournament.tasksDesc')" :eyebrow="tx('Раунди', 'Rounds')">
        <div v-if="tasks.length" class="grid-auto">
          <article v-for="task in tasks" :key="task.id" class="panel stack">
            <div class="toolbar">
              <h3 class="title-md">{{ task.title }}</h3>
              <StatusBadge :status="task.status" />
            </div>
            <p class="text-soft">{{ task.description }}</p>
            <div class="meta-list">
              <div class="meta-row">
                <span class="text-soft">{{ t("tournament.window") }}</span>
                <strong>{{ formatDateTime(task.startAt) }} → {{ formatDateTime(task.deadlineAt) }}</strong>
              </div>
              <div class="meta-row">
                <span class="text-soft">{{ t("tournament.technology") }}</span>
                <strong>{{ task.technologyRequirements || t("common.none") }}</strong>
              </div>
            </div>
            <div class="stack-sm" v-if="task.mustHaveCriteria?.length">
              <strong>{{ t("tournament.mustHave") }}</strong>
              <ul class="stack-sm">
                <li v-for="criterion in task.mustHaveCriteria" :key="criterion">{{ criterion }}</li>
              </ul>
            </div>
          </article>
        </div>
        <div v-else class="empty-box">{{ t("common.none") }}</div>
      </SectionBlock>

      <section class="split">
        <SectionBlock :title="t('tournament.announcements')" :description="t('tournament.announcementsDesc')" :eyebrow="tx('Новини', 'News')">
          <div v-if="announcements.length" class="stack">
            <article v-for="announcement in announcements" :key="announcement.id" class="panel stack-sm">
              <div class="toolbar">
                <h3 class="title-sm">{{ announcement.title }}</h3>
                <span class="text-soft">{{ formatDateTime(announcement.createdAt) }}</span>
              </div>
              <p class="text-soft">{{ announcement.content }}</p>
            </article>
          </div>
          <div v-else class="empty-box">{{ t("common.none") }}</div>
        </SectionBlock>

        <SectionBlock :title="t('tournament.scheduleSection')" :description="t('tournament.scheduleDesc')" :eyebrow="tx('Календар', 'Calendar')">
          <div v-if="schedule.length" class="stack">
            <article v-for="event in schedule" :key="event.id" class="panel stack-sm">
              <div class="toolbar">
                <h3 class="title-sm">{{ event.title }}</h3>
                <span class="text-soft">{{ formatDateTime(event.startAt) }}</span>
              </div>
              <p class="text-soft">{{ event.description || t("common.none") }}</p>
              <a
                v-if="toSafeExternalUrl(event.link)"
                class="btn-tonal"
                :href="toSafeExternalUrl(event.link)"
                target="_blank"
                rel="noreferrer"
              >
                {{ t("tournament.openLink") }}
              </a>
            </article>
          </div>
          <div v-else class="empty-box">{{ t("common.none") }}</div>
        </SectionBlock>
      </section>

      <section class="split">
        <SectionBlock
          :title="t('tournament.teamsTitle')"
          :description="tournament.teamsVisible ? t('tournament.teamsVisible') : t('tournament.teamsHidden')"
          :eyebrow="tx('Склад', 'Roster')"
        >
          <div v-if="tournament.teamsVisible && teams.length" class="stack">
            <article v-for="team in teams" :key="team.id" class="panel stack-sm">
              <div class="toolbar">
                <h3 class="title-sm">{{ team.name }}</h3>
                <span class="text-soft">{{ team.captainEmail }}</span>
              </div>
              <p class="text-soft">{{ team.organization || t("common.none") }}</p>
            </article>
          </div>
          <div v-else class="empty-box">
            {{ tournament.teamsVisible ? t("tournament.noTeams") : t("tournament.hiddenTeams") }}
          </div>
        </SectionBlock>

        <SectionBlock :title="t('tournament.leaderboard')" :description="t('tournament.leaderboardDesc')" :eyebrow="tx('Результати', 'Results')">
          <div v-if="leaderboard.entries?.length" class="stack">
            <article v-for="entry in leaderboard.entries" :key="entry.teamId" class="panel stack-sm">
              <div class="toolbar">
                <div>
                  <h3 class="title-sm">#{{ entry.position }} · {{ entry.teamName }}</h3>
                  <p class="text-soft">{{ entry.captainEmail }}</p>
                </div>
                <strong>{{ entry.totalScore.toFixed(2) }}</strong>
              </div>
            </article>
          </div>
          <div v-else class="empty-box">{{ t("tournament.leaderboardEmpty") }}</div>
        </SectionBlock>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import SectionBlock from "../components/SectionBlock.vue";
import StatusBadge from "../components/StatusBadge.vue";
import { api } from "../services/api";
import { authStore } from "../services/auth";
import { notifier } from "../services/notify";
import { formatDateTime, getErrorMessage } from "../services/formatters";
import { t, tx } from "../services/i18n";
import { toSafeExternalUrl } from "../services/security";

const props = defineProps({
  id: {
    type: [String, Number],
    required: true
  }
});

const loading = ref(true);
const errorMessage = ref("");
const registeringTeam = ref(false);
const loadingMyTeams = ref(false);
const registrationError = ref("");
const registrationMessage = ref("");
const myTeams = ref([]);
const selectedTeamIdForJoin = ref(null);
const tournament = ref(null);
const teams = ref([]);
const tasks = ref([]);
const announcements = ref([]);
const schedule = ref([]);
const leaderboard = reactive({ entries: [] });

const alreadyJoinedTeam = computed(() => myTeams.value.find((team) => team.tournamentId === Number(props.id)) || null);
const joinableTeams = computed(() => myTeams.value.filter((team) => !team.tournamentId));
const canEditTournament = computed(() => authStore.hasRole("ADMIN", "ORGANIZER"));

async function loadTournament() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const tournamentId = Number(props.id);
    const [tournamentData, teamsData, tasksData, announcementsData, scheduleData, leaderboardData] =
      await Promise.all([
        api.public.tournament(tournamentId),
        api.public.teams(tournamentId),
        api.public.tasks(tournamentId),
        api.public.announcements(tournamentId),
        api.public.schedule(tournamentId),
        api.public.leaderboard(tournamentId)
      ]);

    tournament.value = tournamentData;
    teams.value = teamsData;
    tasks.value = tasksData;
    announcements.value = announcementsData;
    schedule.value = scheduleData;
    Object.assign(leaderboard, leaderboardData);
    await loadMyTeams();
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
  } finally {
    loading.value = false;
  }
}

async function loadMyTeams() {
  if (!authStore.isLoggedIn.value) {
    myTeams.value = [];
    selectedTeamIdForJoin.value = null;
    return;
  }
  loadingMyTeams.value = true;
  try {
    myTeams.value = await api.team.myTeams();
    selectedTeamIdForJoin.value = myTeams.value.find((team) => !team.tournamentId)?.id || null;
  } catch (error) {
    myTeams.value = [];
    notifier.pushNotification(getErrorMessage(error), "error");
  } finally {
    loadingMyTeams.value = false;
  }
}

async function handleTeamRegistration() {
  if (!selectedTeamIdForJoin.value) {
    return;
  }
  registeringTeam.value = true;
  registrationError.value = "";
  registrationMessage.value = "";
  try {
    await api.team.joinTeam(selectedTeamIdForJoin.value, Number(props.id));
    registrationMessage.value = t("tournament.registeredMessage");
    notifier.pushNotification(t("tournament.registered"), "success");
    await loadTournament();
  } catch (error) {
    registrationError.value = getErrorMessage(error);
  } finally {
    registeringTeam.value = false;
  }
}

watch(() => props.id, loadTournament);
onMounted(loadTournament);
</script>
