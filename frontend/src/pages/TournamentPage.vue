<template>
  <div class="page-grid">
    <div v-if="loading" class="panel">Loading tournament…</div>
    <div v-else-if="errorMessage" class="error-box">{{ errorMessage }}</div>
    <template v-else-if="tournament">
      <section class="hero-card stack">
        <div class="toolbar">
          <div class="stack-sm">
            <span class="eyebrow">Tournament</span>
            <h1 class="title-lg">{{ tournament.title }}</h1>
            <p class="text-soft">{{ tournament.description }}</p>
          </div>
          <StatusBadge :status="tournament.status" />
        </div>

        <div class="stat-row">
          <span class="stat-chip">Registration {{ tournament.registrationOpen ? "open" : "closed" }}</span>
          <span class="stat-chip">{{ tournament.registeredTeams }} registered teams</span>
          <span class="stat-chip">{{ tournament.minimumRounds }} planned rounds</span>
          <span class="stat-chip">{{ tournament.teamMinMembers }}–{{ tournament.teamMaxMembers }} members</span>
        </div>

        <div class="split">
          <div class="panel stack-sm">
            <h2 class="title-sm">Rules</h2>
            <p class="text-soft">{{ tournament.rules || "No additional rules provided." }}</p>
          </div>
          <div class="panel stack-sm">
            <h2 class="title-sm">Schedule window</h2>
            <p class="text-soft">Registration: {{ formatDateTime(tournament.registrationStartAt) }} → {{ formatDateTime(tournament.registrationEndAt) }}</p>
            <p class="text-soft">Tournament start: {{ formatDateTime(tournament.startAt) }}</p>
          </div>
        </div>
      </section>

      <SectionBlock
        v-if="authStore.isLoggedIn.value && authStore.hasRole('TEAM', 'USER', 'ADMIN') && tournament.registrationOpen"
        title="Register a Team"
        description="You can register directly from the tournament page while the registration window is open."
        eyebrow="Team entry"
      >
        <form class="stack" @submit.prevent="handleTeamRegistration">
          <div class="field-grid">
            <div class="field">
              <label>Team name</label>
              <input v-model="teamForm.name" type="text" required minlength="2" />
            </div>
            <div class="field">
              <label>City</label>
              <input v-model="teamForm.city" type="text" />
            </div>
            <div class="field">
              <label>Organization</label>
              <input v-model="teamForm.organization" type="text" />
            </div>
            <div class="field">
              <label>Telegram / Discord</label>
              <input v-model="teamForm.contactHandle" type="text" />
            </div>
          </div>

          <div class="stack">
            <div class="toolbar">
              <h3 class="title-sm">Additional members</h3>
              <button class="btn-tonal" type="button" @click="addMember">Add member</button>
            </div>
            <div v-for="(member, index) in teamForm.members" :key="index" class="panel stack-sm">
              <div class="field-grid">
                <div class="field">
                  <label>Full name</label>
                  <input v-model="member.fullName" type="text" required />
                </div>
                <div class="field">
                  <label>Email</label>
                  <input v-model="member.email" type="email" required />
                </div>
              </div>
              <div class="btn-row">
                <button class="btn-ghost" type="button" @click="removeMember(index)" :disabled="teamForm.members.length === 1">
                  Remove member
                </button>
              </div>
            </div>
          </div>

          <div class="success-box" v-if="registrationMessage">{{ registrationMessage }}</div>
          <div class="error-box" v-if="registrationError">{{ registrationError }}</div>

          <div class="btn-row">
            <button class="btn" type="submit" :disabled="registeringTeam">
              {{ registeringTeam ? "Registering…" : "Register team" }}
            </button>
            <RouterLink class="btn-ghost" to="/team">Open workspace</RouterLink>
          </div>
        </form>
      </SectionBlock>

      <SectionBlock title="Tasks" description="Visible rounds and task briefs for this tournament." eyebrow="Rounds">
        <div v-if="tasks.length" class="grid-auto">
          <article v-for="task in tasks" :key="task.id" class="panel stack">
            <div class="toolbar">
              <h3 class="title-md">{{ task.title }}</h3>
              <StatusBadge :status="task.status" />
            </div>
            <p class="text-soft">{{ task.description }}</p>
            <div class="meta-list">
              <div class="meta-row">
                <span class="text-soft">Window</span>
                <strong>{{ formatDateTime(task.startAt) }} → {{ formatDateTime(task.deadlineAt) }}</strong>
              </div>
              <div class="meta-row">
                <span class="text-soft">Technology</span>
                <strong>{{ task.technologyRequirements || "Open choice" }}</strong>
              </div>
            </div>
            <div class="stack-sm" v-if="task.mustHaveCriteria?.length">
              <strong>Must have</strong>
              <ul class="stack-sm">
                <li v-for="criterion in task.mustHaveCriteria" :key="criterion">{{ criterion }}</li>
              </ul>
            </div>
          </article>
        </div>
        <div v-else class="empty-box">No visible tasks yet.</div>
      </SectionBlock>

      <section class="split">
        <SectionBlock title="Announcements" description="Updates from tournament organizers." eyebrow="News">
          <div v-if="announcements.length" class="stack">
            <article v-for="announcement in announcements" :key="announcement.id" class="panel stack-sm">
              <div class="toolbar">
                <h3 class="title-sm">{{ announcement.title }}</h3>
                <span class="text-soft">{{ formatDateTime(announcement.createdAt) }}</span>
              </div>
              <p class="text-soft">{{ announcement.content }}</p>
            </article>
          </div>
          <div v-else class="empty-box">No announcements yet.</div>
        </SectionBlock>

        <SectionBlock title="Schedule" description="Events, consultations, and milestone timing." eyebrow="Calendar">
          <div v-if="schedule.length" class="stack">
            <article v-for="event in schedule" :key="event.id" class="panel stack-sm">
              <div class="toolbar">
                <h3 class="title-sm">{{ event.title }}</h3>
                <span class="text-soft">{{ formatDateTime(event.startAt) }}</span>
              </div>
              <p class="text-soft">{{ event.description || "No description." }}</p>
              <a v-if="event.link" class="btn-tonal" :href="event.link" target="_blank" rel="noreferrer">Open link</a>
            </article>
          </div>
          <div v-else class="empty-box">No schedule events yet.</div>
        </SectionBlock>
      </section>

      <section class="split">
        <SectionBlock title="Teams" :description="tournament.teamsVisible ? 'Registered teams currently visible for this tournament.' : 'The team list is hidden until registration ends.'" eyebrow="Roster">
          <div v-if="tournament.teamsVisible && teams.length" class="stack">
            <article v-for="team in teams" :key="team.id" class="panel stack-sm">
              <div class="toolbar">
                <h3 class="title-sm">{{ team.name }}</h3>
                <span class="text-soft">{{ team.captainEmail }}</span>
              </div>
              <p class="text-soft">{{ team.organization || "No organization specified" }}</p>
            </article>
          </div>
          <div v-else class="empty-box">
            {{ tournament.teamsVisible ? "No teams registered yet." : "Team roster is still hidden." }}
          </div>
        </SectionBlock>

        <SectionBlock title="Leaderboard" description="Current ranking based on available jury scores." eyebrow="Results">
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
          <div v-else class="empty-box">Leaderboard data will appear after evaluation starts.</div>
        </SectionBlock>
      </section>
    </template>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import SectionBlock from "../components/SectionBlock.vue";
import StatusBadge from "../components/StatusBadge.vue";
import { api } from "../services/api";
import { authStore } from "../services/auth";
import { notifier } from "../services/notify";
import { formatDateTime, getErrorMessage } from "../services/formatters";

const props = defineProps({
  id: {
    type: [String, Number],
    required: true
  }
});

const loading = ref(true);
const errorMessage = ref("");
const registeringTeam = ref(false);
const registrationError = ref("");
const registrationMessage = ref("");
const tournament = ref(null);
const teams = ref([]);
const tasks = ref([]);
const announcements = ref([]);
const schedule = ref([]);
const leaderboard = reactive({ entries: [] });

const teamForm = reactive({
  name: "",
  city: "",
  organization: "",
  contactHandle: "",
  members: [{ fullName: "", email: "" }]
});

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
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
  } finally {
    loading.value = false;
  }
}

function addMember() {
  teamForm.members.push({ fullName: "", email: "" });
}

function removeMember(index) {
  if (teamForm.members.length > 1) {
    teamForm.members.splice(index, 1);
  }
}

async function handleTeamRegistration() {
  registeringTeam.value = true;
  registrationError.value = "";
  registrationMessage.value = "";
  try {
    await api.team.registerTeam(Number(props.id), teamForm);
    registrationMessage.value = "Team registration submitted. Open the team workspace to manage your roster and submissions.";
    notifier.pushNotification("Team registered successfully.", "success");
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
