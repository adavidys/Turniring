<template>
  <div class="page-grid">
    <section class="hero-card stack">
      <span class="eyebrow">{{ t("team.workspace") }}</span>
      <div class="toolbar">
        <div class="stack-sm">
          <h1 class="title-lg">{{ t("team.captainDesk") }}</h1>
          <p class="text-soft">{{ t("team.workspaceCopy") }}</p>
        </div>
        <div class="btn-row">
          <RouterLink class="btn-tonal" to="/teams/create">{{ tx("Створити", "Create") }}</RouterLink>
          <RouterLink class="btn-ghost" to="/teams/join">{{ tx("Приєднати", "Join") }}</RouterLink>
          <RouterLink class="btn-ghost" to="/profile">{{ t("nav.profile") }}</RouterLink>
        </div>
      </div>
    </section>

    <div v-if="loading" class="panel">{{ t("common.loading") }}</div>
    <div v-else-if="errorMessage" class="error-box">{{ errorMessage }}</div>
    <template v-else>
      <SectionBlock
        v-if="!teams.length"
        :title="t('team.noTeams')"
        :description="t('team.noTeamsDesc')"
        :eyebrow="tx('Початок', 'Start')"
      >
        <div v-if="openTournaments.length" class="grid-auto">
          <TournamentCard v-for="tournament in openTournaments" :key="tournament.id" :tournament="tournament" />
        </div>
        <div v-else class="empty-box">{{ t("team.openRegistrationsEmpty") }}</div>
      </SectionBlock>

      <template v-else>
        <section class="split">
          <SectionBlock :title="t('team.yourTeams')" :description="t('team.yourTeamsDesc')" :eyebrow="tx('Склад', 'Roster')">
            <div class="stack">
              <button
                v-for="team in teams"
                :key="team.id"
                class="btn-ghost"
                type="button"
                @click="handleTeamSelection(team)"
              >
                {{ team.name }} · {{ team.tournamentId ? `${tx("турнір", "tournament")} #${team.tournamentId}` : tx("без олімпіади", "without olympiad") }}
              </button>
            </div>
          </SectionBlock>

          <SectionBlock
            v-if="selectedTeam"
            :title="t('team.editRoster')"
            :description="t('team.editRosterDesc')"
             :eyebrow="tx('Керування', 'Manage')"
            >
             <form class="stack" @submit.prevent="saveTeam">
              <div class="panel stack-sm">
                <h3 class="title-sm">{{ tx("Олімпіада", "Olympiad") }}</h3>
                <p class="text-soft" v-if="selectedTeam.tournamentId">{{ tx("Команда приєднана до турніру", "Team is joined to tournament") }} #{{ selectedTeam.tournamentId }}.</p>
                <template v-else>
                  <p class="text-soft">{{ tx("Команда ще не приєднана до олімпіади.", "Team is not joined to an olympiad yet.") }}</p>
                  <div class="field" v-if="openTournaments.length">
                    <label>{{ tx("Оберіть олімпіаду", "Select olympiad") }}</label>
                    <select v-model.number="joinTournamentId">
                      <option :value="null">{{ tx("Оберіть олімпіаду", "Select olympiad") }}</option>
                      <option v-for="tournament in openTournaments" :key="tournament.id" :value="tournament.id">
                        {{ tournament.title }} · {{ tournament.status }}
                      </option>
                    </select>
                  </div>
                  <div class="empty-box" v-else>{{ tx("Немає відкритих олімпіад для приєднання.", "No open olympiads to join.") }}</div>
                </template>
                <div class="btn-row">
                  <button class="btn-tonal" type="button" :disabled="joiningTournament || !joinTournamentId || selectedTeam.tournamentId" @click="joinTournament">
                    {{ joiningTournament ? tx("Приєднання…", "Joining…") : tx("Приєднати до олімпіади", "Join olympiad") }}
                  </button>
                  <button class="btn-ghost" type="button" :disabled="leavingTournament || !selectedTeam.tournamentId" @click="leaveTournament">
                    {{ leavingTournament ? tx("Вихід…", "Leaving…") : tx("Вийти з олімпіади", "Leave olympiad") }}
                  </button>
                  <button class="btn-danger" type="button" :disabled="deletingTeam" @click="deleteTeam">
                    {{ deletingTeam ? tx("Видалення…", "Deleting…") : tx("Вийти з командою", "Leave team") }}
                  </button>
                </div>
              </div>

              <div class="field-grid">
                <div class="field">
                  <label>{{ t("tournament.teamName") }}</label>
                  <input v-model="teamForm.name" type="text" required />
                </div>
                <div class="field">
                  <label>{{ t("tournament.city") }}</label>
                  <input v-model="teamForm.city" type="text" />
                </div>
                <div class="field">
                  <label>{{ t("tournament.org") }}</label>
                  <input v-model="teamForm.organization" type="text" />
                </div>
                <div class="field">
                  <label>{{ t("tournament.contact") }}</label>
                  <input v-model="teamForm.contactHandle" type="text" />
                </div>
              </div>

              <div class="toolbar">
                <h3 class="title-sm">{{ t("tournament.additionalMembers") }}</h3>
                <button class="btn-tonal" type="button" @click="addEditableMember">{{ t("tournament.addMember") }}</button>
              </div>

              <div v-for="(member, index) in teamForm.members" :key="index" class="panel stack-sm">
                <div class="field-grid">
                  <div class="field">
                    <label>{{ t("tournament.fullName") }}</label>
                    <input v-model="member.fullName" type="text" required />
                  </div>
                  <div class="field">
                    <label>{{ t("auth.email") }}</label>
                    <input v-model="member.email" type="email" required />
                  </div>
                </div>
                <div class="btn-row">
                  <button class="btn-ghost" type="button" @click="removeEditableMember(index)">
                    {{ t("tournament.removeMember") }}
                  </button>
                </div>
              </div>

              <div class="success-box" v-if="teamMessage">{{ teamMessage }}</div>
              <div class="error-box" v-if="teamError">{{ teamError }}</div>

              <div class="btn-row">
                <button class="btn" type="submit" :disabled="savingTeam">
                  {{ savingTeam ? t("team.saving") : t("team.saveRoster") }}
                </button>
              </div>
            </form>
          </SectionBlock>
        </section>

        <SectionBlock
          v-if="selectedTeam && selectedTeam.tournamentId"
          :title="t('team.tasks')"
          :description="t('team.tasksDesc')"
            :eyebrow="tx('Подання', 'Submissions')"
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
                <span class="stat-chip">{{ t("team.starts") }} {{ formatDateTime(task.startAt) }}</span>
                <span class="stat-chip">{{ t("team.deadline") }} {{ formatDateTime(task.deadlineAt) }}</span>
              </div>

              <form class="stack" @submit.prevent="saveSubmission(task.id)">
                <div class="field-grid">
                  <div class="field">
                    <label>{{ t("team.github") }}</label>
                    <input v-model="submissionForms[task.id].githubUrl" type="url" required />
                  </div>
                  <div class="field">
                    <label>{{ t("team.demo") }}</label>
                    <input v-model="submissionForms[task.id].demoVideoUrl" type="url" required />
                  </div>
                  <div class="field">
                    <label>{{ t("team.live") }}</label>
                    <input v-model="submissionForms[task.id].liveDemoUrl" type="url" />
                  </div>
                  <div class="field">
                    <label>{{ t("team.summary") }}</label>
                    <MarkdownEditorField
                      v-model="submissionForms[task.id].summary"
                      :label="t('team.summary')"
                      :button-text="tx('Редагувати summary у Markdown', 'Edit summary in Markdown')"
                    />
                  </div>
                </div>

                <div class="success-box" v-if="submissionMessages[task.id]">{{ submissionMessages[task.id] }}</div>
                <div class="error-box" v-if="submissionErrors[task.id]">{{ submissionErrors[task.id] }}</div>

                <div class="btn-row">
                  <button class="btn" type="submit" :disabled="savingSubmission[task.id]">
                    {{ savingSubmission[task.id] ? t("team.saving") : t("team.saveSubmission") }}
                  </button>
                </div>
              </form>
            </article>
          </div>
          <div v-else class="empty-box">{{ t("common.none") }}</div>
        </SectionBlock>
      </template>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import MarkdownEditorField from "../components/MarkdownEditorField.vue";
import SectionBlock from "../components/SectionBlock.vue";
import StatusBadge from "../components/StatusBadge.vue";
import TournamentCard from "../components/TournamentCard.vue";
import { api } from "../services/api";
import { notifier } from "../services/notify";
import { formatDateTime, getErrorMessage } from "../services/formatters";
import { t, tx } from "../services/i18n";

const loading = ref(true);
const errorMessage = ref("");
const teams = ref([]);
const openTournaments = ref([]);
const tasks = ref([]);
const selectedTeamId = ref(null);
const savingTeam = ref(false);
const joiningTournament = ref(false);
const leavingTournament = ref(false);
const deletingTeam = ref(false);
const joinTournamentId = ref(null);
const teamError = ref("");
const teamMessage = ref("");

const teamForm = reactive({
  name: "",
  city: "",
  organization: "",
  contactHandle: "",
  members: []
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
}

function addEditableMember() {
  teamForm.members.push({ fullName: "", email: "" });
}

async function selectTeam(team) {
  selectedTeamId.value = team.id;
  joinTournamentId.value = null;
  setTeamForm(team);
  teamError.value = "";
  teamMessage.value = "";
  tasks.value = [];

  if (!team.tournamentId) {
    return;
  }

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

function upsertTeam(updatedTeam) {
  const index = teams.value.findIndex((team) => team.id === updatedTeam.id);
  if (index >= 0) {
    teams.value[index] = updatedTeam;
  }
}

async function handleTeamSelection(team) {
  try {
    await selectTeam(team);
  } catch (error) {
    teamError.value = getErrorMessage(error);
    notifier.pushNotification(teamError.value, "error");
  }
}

function removeEditableMember(index) {
  if (teamForm.members.length > 0) {
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
    upsertTeam(updatedTeam);
    setTeamForm(updatedTeam);
    teamMessage.value = t("team.rosterSaved");
    notifier.pushNotification(t("team.rosterSavedToast"), "success");
  } catch (error) {
    teamError.value = getErrorMessage(error);
  } finally {
    savingTeam.value = false;
  }
}

async function joinTournament() {
  if (!selectedTeam.value || !joinTournamentId.value || selectedTeam.value.tournamentId) {
    return;
  }
  joiningTournament.value = true;
  teamError.value = "";
  teamMessage.value = "";
  try {
    const updatedTeam = await api.team.joinTeam(selectedTeam.value.id, joinTournamentId.value);
    upsertTeam(updatedTeam);
    await selectTeam(updatedTeam);
    teamMessage.value = tx("Команду приєднано до олімпіади.", "Team joined the olympiad.");
    notifier.pushNotification(tx("Команду приєднано до олімпіади.", "Team joined the olympiad."), "success");
  } catch (error) {
    teamError.value = getErrorMessage(error);
  } finally {
    joiningTournament.value = false;
  }
}

async function leaveTournament() {
  if (!selectedTeam.value || !selectedTeam.value.tournamentId) {
    return;
  }
  leavingTournament.value = true;
  teamError.value = "";
  teamMessage.value = "";
  try {
    const updatedTeam = await api.team.leaveTeam(selectedTeam.value.id);
    upsertTeam(updatedTeam);
    await selectTeam(updatedTeam);
    teamMessage.value = tx("Команда вийшла з олімпіади.", "Team left the olympiad.");
    notifier.pushNotification(tx("Команда вийшла з олімпіади.", "Team left the olympiad."), "success");
  } catch (error) {
    teamError.value = getErrorMessage(error);
  } finally {
    leavingTournament.value = false;
  }
}

async function deleteTeam() {
  if (!selectedTeam.value) {
    return;
  }

  const accepted = window.confirm(
    tx("Ви дійсно хочете вийти з командою? Команду буде видалено.", "Do you really want to leave with team? The team will be deleted.")
  );
  if (!accepted) {
    return;
  }

  deletingTeam.value = true;
  teamError.value = "";
  teamMessage.value = "";
  try {
    await api.team.deleteTeam(selectedTeam.value.id);
    notifier.pushNotification(tx("Команду видалено.", "Team deleted."), "success");
    await loadWorkspace();
  } catch (error) {
    teamError.value = getErrorMessage(error);
  } finally {
    deletingTeam.value = false;
  }
}

async function saveSubmission(taskId) {
  submissionErrors[taskId] = "";
  submissionMessages[taskId] = "";
  savingSubmission[taskId] = true;
  try {
    await api.team.saveSubmission(taskId, submissionForms[taskId]);
    submissionMessages[taskId] = t("team.submissionSaved");
    notifier.pushNotification(t("team.submissionSaved"), "success");
  } catch (error) {
    submissionErrors[taskId] = getErrorMessage(error);
  } finally {
    savingSubmission[taskId] = false;
  }
}

onMounted(loadWorkspace);
</script>
