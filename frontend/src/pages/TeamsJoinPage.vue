<template>
  <div class="page-grid">
    <section class="hero-card stack">
      <span class="eyebrow">{{ t("nav.teams") }}</span>
      <h1 class="title-lg">{{ t("team.joinListTitle") }}</h1>
      <p class="text-soft">{{ t("team.joinListCopy") }}</p>
    </section>

    <SectionBlock :title="t('team.selectOlympiad')" :description="t('team.selectDesc')" :eyebrow="tx('Фільтр', 'Filter')">
      <div v-if="loadingTournaments" class="panel">{{ tx("Завантаження олімпіад…", "Loading olympiads…") }}</div>
      <div v-else class="field">
        <label>{{ t("nav.olympiads") }}</label>
        <select v-model.number="selectedTournamentId" @change="loadTeams">
          <option :value="null">{{ t("team.selectOlympiad") }}</option>
          <option v-for="tournament in tournaments" :key="tournament.id" :value="tournament.id">
            {{ tournament.title }} · {{ formatStatus(tournament.status) }}
          </option>
        </select>
      </div>
    </SectionBlock>

    <SectionBlock :title="t('team.joinListTitle')" :description="tx('Звʼяжіться з капітаном, щоб долучитися.', 'Contact the captain to join.')" :eyebrow="t('nav.teams')">
      <div v-if="loadingTeams" class="panel">{{ tx("Завантаження команд…", "Loading teams…") }}</div>
      <div v-else-if="teams.length" class="grid-auto">
        <article v-for="team in teams" :key="team.id" class="panel stack">
          <div class="toolbar">
            <div>
              <h3 class="title-md">{{ team.name }}</h3>
              <p class="text-soft">{{ tx("Капітан", "Captain") }}: {{ team.captainName }}</p>
            </div>
            <span class="status-badge" data-tone="neutral">{{ tx("Турнір", "Tournament") }} #{{ team.tournamentId }}</span>
          </div>
          <p class="text-soft">{{ team.organization || tx("Організацію не вказано", "Organization not set") }}</p>
          <div class="stat-row">
            <span class="stat-chip">{{ t("auth.email") }}: {{ team.captainEmail }}</span>
            <span class="stat-chip" v-if="team.contactHandle">{{ tx("Контакт", "Contact") }}: {{ team.contactHandle }}</span>
          </div>
          <div class="btn-row">
            <a v-if="toSafeMailto(team.captainEmail)" class="btn-tonal" :href="toSafeMailto(team.captainEmail)">
              {{ tx("Написати капітану", "Write to captain") }}
            </a>
          </div>
        </article>
      </div>
      <div v-else class="empty-box">{{ tx("Команд для цієї олімпіади поки що немає.", "There are no teams for this olympiad yet.") }}</div>
    </SectionBlock>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import SectionBlock from "../components/SectionBlock.vue";
import { api } from "../services/api";
import { notifier } from "../services/notify";
import { formatStatus, getErrorMessage } from "../services/formatters";
import { t, tx } from "../services/i18n";
import { toSafeMailto } from "../services/security";

const tournaments = ref([]);
const teams = ref([]);
const selectedTournamentId = ref(null);
const loadingTournaments = ref(true);
const loadingTeams = ref(false);

async function loadTournaments() {
  loadingTournaments.value = true;
  try {
    tournaments.value = await api.public.tournaments();
  } catch (error) {
    notifier.pushNotification(getErrorMessage(error), "error");
  } finally {
    loadingTournaments.value = false;
  }
}

async function loadTeams() {
  if (!selectedTournamentId.value) {
    teams.value = [];
    return;
  }
  loadingTeams.value = true;
  try {
    teams.value = await api.public.teams(selectedTournamentId.value);
  } catch (error) {
    notifier.pushNotification(getErrorMessage(error), "error");
  } finally {
    loadingTeams.value = false;
  }
}

onMounted(loadTournaments);
</script>
