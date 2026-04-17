<template>
  <div class="page-grid">
    <section class="hero-card stack">
      <span class="eyebrow">Team workspace</span>
      <div class="toolbar">
        <div class="stack-sm">
          <h1 class="title-lg">Captain desk</h1>
          <p class="text-soft">
            Manage your roster, inspect active tasks, and submit GitHub/demo links before deadlines close.
          </p>
        </div>
        <RouterLink class="btn-tonal" to="/profile">Profile</RouterLink>
      </div>
    </section>

    <div v-if="loading" class="panel">Loading team workspace…</div>
    <div v-else-if="errorMessage" class="error-box">{{ errorMessage }}</div>
    <template v-else>
      <SectionBlock
        v-if="!teams.length"
        title="No teams yet"
        description="Your account exists, but you have not registered a team. Pick a tournament with open registration and enter the bracket."
        eyebrow="Get started"
      >
        <div v-if="openTournaments.length" class="grid-auto">
          <TournamentCard v-for="tournament in openTournaments" :key="tournament.id" :tournament="tournament" />
        </div>
        <div v-else class="empty-box">No open registration windows right now.</div>
      </SectionBlock>

      <template v-else>
        <section class="split">
          <SectionBlock title="Your teams" description="Select a roster to work on submissions." eyebrow="Roster">
            <div class="stack">
              <button
                v-for="team in teams"
                :key="team.id"
                class="btn-ghost"
                type="button"
                @click="selectTeam(team)"
              >
                {{ team.name }} · tournament #{{ team.tournamentId }}
              </button>
            </div>
          </SectionBlock>

          <SectionBlock
            v-if="selectedTeam"
            title="Edit roster"
            description="Updates are allowed until the registration window closes unless an admin overrides it."
            eyebrow="Manage"
          >
            <form class="stack" @submit.prevent="saveTeam">
              <div class="field-grid">
                <div class="field">
                  <label>Team name</label>
                  <input v-model="teamForm.name" type="text" required />
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

              <div class="toolbar">
                <h3 class="title-sm">Additional members</h3>
                <button class="btn-tonal" type="button" @click="teamForm.members.push({ fullName: '', email: '' })">Add member</button>
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
                  <button class="btn-ghost" type="button" @click="removeEditableMember(index)" :disabled="teamForm.members.length === 1">
                    Remove
                  </button>
                </div>
              </div>

              <div class="success-box" v-if="teamMessage">{{ teamMessage }}</div>
              <div class="error-box" v-if="teamError">{{ teamError }}</div>

              <div class="btn-row">
                <button class="btn" type="submit" :disabled="savingTeam">
                  {{ savingTeam ? "Saving…" : "Save roster" }}
                </button>
              </div>
            </form>
          </SectionBlock>
        </section>

        <SectionBlock
          v-if="selectedTeam"
          title="Tasks and submissions"
          description="Each task keeps its own submission record. Drafts are overwritten by the latest save."
          eyebrow="Delivery"
        >
          <div v-if="tasks.length" class="stack">
            <article v-for="task in tasks" :key="task.id" class="panel stack">
              <div class="toolbar">
                <div class="stack-sm">
                  <h3 class="title-md">{{ task.title }}</h3>
                  <p class="text-soft">{{ task.description }}</p>
                </div>
                <StatusBadge :status="task.status" />
              </div>

              <div class="stat-row">
                <span class="stat-chip">Starts {{ formatDateTime(task.startAt) }}</span>
                <span class="stat-chip">Deadline {{ formatDateTime(task.deadlineAt) }}</span>
              </div>

              <form class="stack" @submit.prevent="saveSubmission(task.id)">
                <div class="field-grid">
                  <div class="field">
                    <label>GitHub repository</label>
                    <input v-model="submissionForms[task.id].githubUrl" type="url" required />
                  </div>
                  <div class="field">
                    <label>Demo video URL</label>
                    <input v-model="submissionForms[task.id].demoVideoUrl" type="url" required />
                  </div>
                  <div class="field">
                    <label>Live demo URL</label>
                    <input v-model="submissionForms[task.id].liveDemoUrl" type="url" />
                  </div>
                  <div class="field">
                    <label>Summary</label>
                    <textarea v-model="submissionForms[task.id].summary"></textarea>
                  </div>
                </div>

                <div class="success-box" v-if="submissionMessages[task.id]">{{ submissionMessages[task.id] }}</div>
                <div class="error-box" v-if="submissionErrors[task.id]">{{ submissionErrors[task.id] }}</div>

                <div class="btn-row">
                  <button class="btn" type="submit" :disabled="savingSubmission[task.id]">
                    {{ savingSubmission[task.id] ? "Saving…" : "Save submission" }}
                  </button>
                </div>
              </form>
            </article>
          </div>
          <div v-else class="empty-box">No visible tasks for the selected tournament.</div>
        </SectionBlock>
      </template>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import SectionBlock from "../components/SectionBlock.vue";
import StatusBadge from "../components/StatusBadge.vue";
import TournamentCard from "../components/TournamentCard.vue";
import { api } from "../services/api";
import { notifier } from "../services/notify";
import { formatDateTime, getErrorMessage } from "../services/formatters";

const loading = ref(true);
const errorMessage = ref("");
const teams = ref([]);
const openTournaments = ref([]);
const tasks = ref([]);
const selectedTeamId = ref(null);
const savingTeam = ref(false);
const teamError = ref("");
const teamMessage = ref("");

const teamForm = reactive({
  name: "",
  city: "",
  organization: "",
  contactHandle: "",
  members: [{ fullName: "", email: "" }]
});

const submissionForms = reactive({});
const submissionErrors = reactive({});
const submissionMessages = reactive({});
const savingSubmission = reactive({});

const selectedTeam = computed(() => teams.value.find((team) => team.id === selectedTeamId.value) || null);

async function loadWorkspace() {
  loading.value = true;
  errorMessage.value = "";
  try {
    teams.value = await api.team.myTeams();
    openTournaments.value = (await api.public.home()).registrationOpen;
    if (teams.value.length) {
      await selectTeam(teams.value[0]);
    }
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
  } finally {
    loading.value = false;
  }
}

function setTeamForm(team) {
  teamForm.name = team.name;
  teamForm.city = team.city || "";
  teamForm.organization = team.organization || "";
  teamForm.contactHandle = team.contactHandle || "";
  teamForm.members = team.members
    .filter((member) => !member.captain)
    .map((member) => ({ fullName: member.fullName, email: member.email }));
  if (!teamForm.members.length) {
    teamForm.members = [{ fullName: "", email: "" }];
  }
}

async function selectTeam(team) {
  selectedTeamId.value = team.id;
  setTeamForm(team);
  teamError.value = "";
  teamMessage.value = "";

  tasks.value = await api.team.tasks(team.tournamentId);
  for (const task of tasks.value) {
    submissionForms[task.id] = {
      githubUrl: "",
      demoVideoUrl: "",
      liveDemoUrl: "",
      summary: ""
    };

    try {
      const submission = await api.team.getSubmission(task.id);
      submissionForms[task.id] = {
        githubUrl: submission.githubUrl || "",
        demoVideoUrl: submission.demoVideoUrl || "",
        liveDemoUrl: submission.liveDemoUrl || "",
        summary: submission.summary || ""
      };
    } catch (error) {
      if (error.status === 404) {
        continue;
      } else {
        notifier.pushNotification(getErrorMessage(error), "error");
      }
    }
  }
}

function removeEditableMember(index) {
  if (teamForm.members.length > 1) {
    teamForm.members.splice(index, 1);
  }
}

async function saveTeam() {
  if (!selectedTeam.value) {
    return;
  }
  savingTeam.value = true;
  teamError.value = "";
  teamMessage.value = "";
  try {
    const updatedTeam = await api.team.updateTeam(selectedTeam.value.id, teamForm);
    const index = teams.value.findIndex((team) => team.id === updatedTeam.id);
    if (index >= 0) {
      teams.value[index] = updatedTeam;
    }
    setTeamForm(updatedTeam);
    teamMessage.value = "Roster updated.";
    notifier.pushNotification("Team roster updated.", "success");
  } catch (error) {
    teamError.value = getErrorMessage(error);
  } finally {
    savingTeam.value = false;
  }
}

async function saveSubmission(taskId) {
  submissionErrors[taskId] = "";
  submissionMessages[taskId] = "";
  savingSubmission[taskId] = true;
  try {
    await api.team.saveSubmission(taskId, submissionForms[taskId]);
    submissionMessages[taskId] = "Submission saved.";
    notifier.pushNotification("Submission saved.", "success");
  } catch (error) {
    submissionErrors[taskId] = getErrorMessage(error);
  } finally {
    savingSubmission[taskId] = false;
  }
}

onMounted(loadWorkspace);
</script>
