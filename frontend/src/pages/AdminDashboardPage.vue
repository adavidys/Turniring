<template>
  <div class="page-grid">
    <section class="hero-card stack">
      <span class="eyebrow">{{ t("admin.dashboard") }}</span>
      <div class="toolbar">
        <div class="stack-sm">
          <h1 class="title-lg">{{ tx("Керуйте турнірами з однієї панелі керування.", "Manage tournaments from a single control panel.") }}</h1>
          <p class="text-soft">{{ t("admin.copy") }}</p>
        </div>
        <div class="btn-row">
          <RouterLink v-if="showUserInviteTools" class="btn-tonal" to="/admin/users/create">{{ t("admin.createUser") }}</RouterLink>
          <RouterLink v-if="showJuryInviteTools" class="btn-tonal" to="/jury/add">{{ t("nav.addJury") }}</RouterLink>
          <button class="btn-tonal" type="button" @click="loadAdminData">{{ t("admin.refresh") }}</button>
        </div>
      </div>
    </section>

    <div v-if="loading" class="panel">{{ t("admin.loading") }}</div>
    <div v-else-if="errorMessage" class="error-box">{{ errorMessage }}</div>
    <template v-else>
      <SectionBlock
        :title="tx('Інструменти адміністратора', 'Admin tools')"
        :description="tx('Операційний огляд, швидка фільтрація турнірів і перехід до керування.', 'Operational overview, quick tournament filtering, and management shortcuts.')"
        :eyebrow="tx('Контроль', 'Control')"
      >
        <div class="stat-row">
          <span v-for="item in administrationStats" :key="item.label" class="stat-chip">
            {{ item.label }}: {{ item.value }}
          </span>
        </div>

        <div class="field-grid">
          <div class="field">
            <label>{{ tx("Пошук турніру", "Tournament search") }}</label>
            <input v-model.trim="instrumentQuery" type="text" :placeholder="tx('Назва турніру', 'Tournament title')" />
          </div>
          <div class="field">
            <label>{{ tx("Статус", "Status") }}</label>
            <select v-model="instrumentStatusFilter">
              <option value="ALL">{{ tx("Усі", "All") }}</option>
              <option value="DRAFT">DRAFT</option>
              <option value="REGISTRATION">REGISTRATION</option>
              <option value="RUNNING">RUNNING</option>
              <option value="FINISHED">FINISHED</option>
            </select>
          </div>
        </div>

        <div v-if="instrumentPreviewTournaments.length" class="grid-auto">
          <article v-for="tournament in instrumentPreviewTournaments" :key="tournament.id" class="glass-card stack-sm admin-instrument-card">
            <div class="toolbar">
              <div class="stack-sm">
                <h3 class="title-sm">{{ tournament.title }}</h3>
                <p class="text-soft">#{{ tournament.id }} · {{ tournament.status }}</p>
              </div>
              <StatusBadge :status="tournament.status" />
            </div>
            <div class="btn-row">
              <button class="btn-tonal" type="button" @click="focusTournament(tournament.id)">{{ tx("Керувати", "Manage") }}</button>
              <button class="btn-danger" type="button" :disabled="submitting[`deleteTournamentQuick-${tournament.id}`]" @click="openDeleteDialog(tournament)">
                {{ submitting[`deleteTournamentQuick-${tournament.id}`] ? tx("Видалення…", "Deleting…") : t("admin.deleteAction") }}
              </button>
              <RouterLink class="btn-ghost" :to="`/tournaments/${tournament.id}`">{{ t("admin.openPublic") }}</RouterLink>
            </div>
          </article>
        </div>
        <div v-else class="empty-box">{{ tx("За поточними фільтрами турнірів не знайдено.", "No tournaments found with current filters.") }}</div>
        <p class="text-soft" v-if="hiddenInstrumentCount > 0">
          {{ tx("Ще", "Another") }} {{ hiddenInstrumentCount }} {{ tx("турнір(ів) доступно за поточними фільтрами.", "tournament(s) are available with current filters.") }}
        </p>
      </SectionBlock>

      <SectionBlock
        :title="tx('UUID-запрошення', 'UUID invites')"
        :description="inviteSectionDescription"
        :eyebrow="tx('Доступ', 'Access')"
      >
        <div :class="showJuryInviteTools && showUserInviteTools ? 'split' : 'stack'">
          <div v-if="showJuryInviteTools" class="panel stack">
            <h3 class="title-sm">{{ tx("Запрошення для журі", "Jury invite") }}</h3>
            <p class="text-soft">{{ tx("Користувач за цим посиланням може перейти в роль JURY.", "A user can switch to JURY role using this link.") }}</p>
            <div class="btn-row">
              <button class="btn" type="button" :disabled="submitting.createJuryInvite" @click="createJuryInvite">
                {{ submitting.createJuryInvite ? tx("Генерація…", "Generating…") : t("jury.inviteGenerate") }}
              </button>
            </div>
            <div v-if="juryInviteLink" class="field">
              <label>{{ t("jury.inviteLink") }}</label>
              <input :value="juryInviteLink" readonly />
              <div class="btn-row">
                <button class="btn-ghost" type="button" @click="copyInviteLink(juryInviteLink)">{{ t("jury.copyLink") }}</button>
              </div>
            </div>
          </div>

          <div v-if="showUserInviteTools" class="panel stack">
            <h3 class="title-sm">{{ tx("Запрошення до команди", "Team invite") }}</h3>
            <p class="text-soft">{{ tx("Оберіть турнір і команду, після чого згенеруйте UUID-посилання.", "Choose a tournament and team, then generate a UUID invite link.") }}</p>
            <div class="field">
              <label>{{ t("nav.team") }}</label>
              <select v-model.number="inviteTeamId" :disabled="!selectedTournamentId || !adminTeams.length">
                <option :value="null">{{ tx("Оберіть команду", "Select team") }}</option>
                <option v-for="team in adminTeams" :key="team.id" :value="team.id">
                  #{{ team.id }} · {{ team.name }}
                </option>
              </select>
            </div>
            <div class="btn-row">
              <button class="btn" type="button" :disabled="submitting.createTeamInvite || !inviteTeamId" @click="createTeamInvite">
                {{ submitting.createTeamInvite ? tx("Генерація…", "Generating…") : t("jury.inviteGenerate") }}
              </button>
            </div>
            <div v-if="teamInviteLink" class="field">
              <label>{{ t("jury.inviteLink") }}</label>
              <input :value="teamInviteLink" readonly />
              <div class="btn-row">
                <button class="btn-ghost" type="button" @click="copyInviteLink(teamInviteLink)">{{ t("jury.copyLink") }}</button>
              </div>
            </div>
          </div>
        </div>
      </SectionBlock>

      <SectionBlock :title="t('admin.tournamentControl')" :description="tx('Оберіть турнір і керуйте всім, що в ньому відбувається.', 'Choose a tournament and manage everything inside it.')" :eyebrow="tx('Операції', 'Operations')">
        <div class="toolbar">
          <div class="field admin-tournament-selector">
            <label>{{ tx("Обраний турнір", "Selected tournament") }}</label>
            <select v-model.number="selectedTournamentId" @change="handleTournamentSelectionChange">
              <option :value="null">{{ t("admin.selectTournament") }}</option>
              <option v-for="tournament in tournaments" :key="tournament.id" :value="tournament.id">
                {{ tournament.title }} · {{ tournament.status }}
              </option>
            </select>
          </div>
          <div class="btn-row" v-if="selectedTournament">
            <RouterLink class="btn-tonal" :to="`/tournaments/${selectedTournament.id}`">{{ t("admin.openPublic") }}</RouterLink>
            <button class="btn-ghost" type="button" @click="downloadLeaderboard">{{ t("admin.exportLeaderboard") }}</button>
          </div>
        </div>

        <template v-if="selectedTournament">
          <div class="split">
            <div class="panel stack">
              <div class="toolbar">
                <h3 class="title-sm">{{ selectedTournament.title }}</h3>
                <StatusBadge :status="selectedTournament.status" />
              </div>
              <div class="field">
                <label>{{ t("admin.updateStatus") }}</label>
                <select v-model="tournamentStatus">
                  <option>DRAFT</option>
                  <option>REGISTRATION</option>
                  <option>RUNNING</option>
                  <option>FINISHED</option>
                </select>
              </div>
              <div class="btn-row">
                <button class="btn" type="button" @click="updateTournamentStatus" :disabled="submitting.updateStatus">
                  {{ submitting.updateStatus ? tx("Оновлення…", "Updating…") : t("admin.applyStatus") }}
                </button>
              </div>
            </div>

            <div class="panel stack">
              <h3 class="title-sm">{{ t("admin.editTournament") }}</h3>
              <form class="stack" @submit.prevent="updateTournamentDetails">
                <div class="field-grid">
                  <div class="field">
                    <label>{{ t("olympiad.form.title") }}</label>
                    <input v-model="editForm.title" type="text" required />
                  </div>
                  <div class="field">
                    <label>{{ t("olympiad.form.start") }}</label>
                    <input v-model="editForm.startAt" type="datetime-local" />
                  </div>
                  <div class="field">
                    <label>{{ t("olympiad.form.registrationStart") }}</label>
                    <input v-model="editForm.registrationStartAt" type="datetime-local" required />
                  </div>
                  <div class="field">
                    <label>{{ t("olympiad.form.registrationEnd") }}</label>
                    <input v-model="editForm.registrationEndAt" type="datetime-local" required />
                  </div>
                  <div class="field">
                    <label>{{ t("olympiad.form.maxTeams") }}</label>
                    <input v-model.number="editForm.maxTeams" type="number" min="1" />
                  </div>
                  <div class="field">
                    <label>{{ t("olympiad.form.minimumRounds") }}</label>
                    <input v-model.number="editForm.minimumRounds" type="number" min="1" required />
                  </div>
                  <div class="field">
                    <label>{{ t("olympiad.form.teamMin") }}</label>
                    <input v-model.number="editForm.teamMinMembers" type="number" min="1" required />
                  </div>
                  <div class="field">
                    <label>{{ t("olympiad.form.teamMax") }}</label>
                    <input v-model.number="editForm.teamMaxMembers" type="number" min="1" required />
                  </div>
                  <div class="field">
                    <label>{{ t("olympiad.form.rules") }}</label>
                    <textarea v-model="editForm.rules"></textarea>
                  </div>
                  <div class="field">
                    <label>{{ t("olympiad.form.description") }}</label>
                    <textarea v-model="editForm.description" required></textarea>
                  </div>
                </div>

                <div class="field">
                  <label>
                    <input v-model="editForm.hideTeamsUntilRegistrationEnds" type="checkbox" />
                    {{ t("olympiad.form.hideTeams") }}
                  </label>
                </div>

                <div class="btn-row">
                  <button class="btn" type="submit" :disabled="submitting.updateTournament">
                    {{ submitting.updateTournament ? tx("Збереження…", "Saving…") : t("admin.saveChanges") }}
                  </button>
                </div>
              </form>

              <div class="admin-danger-zone stack">
                <h4 class="title-sm">{{ t("admin.deleteDangerTitle") }}</h4>
                <p class="text-soft">{{ t("admin.deleteDangerCopy") }}</p>
                <div class="field">
                  <label>{{ t("admin.deleteConfirmLabel") }}</label>
                  <input v-model.trim="deleteConfirmationText" type="text" :placeholder="t('admin.deleteTypeTitle')" />
                </div>
                <div class="error-box" v-if="errors.deleteTournament">{{ errors.deleteTournament }}</div>
                <div class="btn-row">
                  <button
                    class="btn-danger"
                    type="button"
                    :disabled="submitting.deleteTournament || !canDeleteSelectedTournament"
                    @click="deleteTournament"
                  >
                    {{ submitting.deleteTournament ? tx("Видалення…", "Deleting…") : t("admin.deleteAction") }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="split">
            <div class="panel stack">
              <h3 class="title-sm">{{ t("admin.announcement") }}</h3>
              <form class="stack" @submit.prevent="createAnnouncement">
                <div class="field">
                  <label>{{ t("olympiad.form.title") }}</label>
                  <input v-model="announcementForm.title" type="text" required />
                </div>
                <div class="field">
                  <label>{{ tx("Вміст", "Content") }}</label>
                  <textarea v-model="announcementForm.content" required></textarea>
                </div>
                <button class="btn" type="submit" :disabled="submitting.announcement">
                  {{ submitting.announcement ? tx("Публікація…", "Publishing…") : t("admin.publish") }}
                </button>
              </form>
            </div>
          </div>

          <div class="split">
            <div class="panel stack">
              <h3 class="title-sm">{{ t("admin.scheduleEvent") }}</h3>
              <form class="stack" @submit.prevent="createScheduleEvent">
                <div class="field-grid">
                  <div class="field">
                    <label>{{ t("olympiad.form.title") }}</label>
                    <input v-model="scheduleForm.title" type="text" required />
                  </div>
                  <div class="field">
                    <label>{{ t("jury.inviteLink") }}</label>
                    <input v-model="scheduleForm.link" type="url" />
                  </div>
                  <div class="field">
                    <label>{{ t("olympiad.form.start") }}</label>
                    <input v-model="scheduleForm.startAt" type="datetime-local" required />
                  </div>
                  <div class="field">
                    <label>{{ tx("Кінець", "End") }}</label>
                    <input v-model="scheduleForm.endAt" type="datetime-local" required />
                  </div>
                  <div class="field">
                    <label>{{ t("olympiad.form.description") }}</label>
                    <textarea v-model="scheduleForm.description"></textarea>
                  </div>
                </div>
                <button class="btn" type="submit" :disabled="submitting.schedule">
                  {{ submitting.schedule ? tx("Збереження…", "Saving…") : tx("Створити подію", "Create event") }}
                </button>
              </form>
            </div>

            <div class="panel stack">
              <h3 class="title-sm">{{ t("admin.createTask") }}</h3>
              <form class="stack" @submit.prevent="createTask">
                <div class="field-grid">
                  <div class="field">
                    <label>{{ t("olympiad.form.title") }}</label>
                    <input v-model="taskForm.title" type="text" required />
                  </div>
                  <div class="field">
                    <label>{{ tx("Посилання на матеріали", "Materials link") }}</label>
                    <input v-model="taskForm.additionalMaterialsUrl" type="url" />
                  </div>
                  <div class="field">
                    <label>{{ t("olympiad.form.start") }}</label>
                    <input v-model="taskForm.startAt" type="datetime-local" required />
                  </div>
                  <div class="field">
                    <label>{{ t("team.deadline") }}</label>
                    <input v-model="taskForm.deadlineAt" type="datetime-local" required />
                  </div>
                  <div class="field">
                    <label>{{ tx("Технологічні вимоги", "Technology requirements") }}</label>
                    <textarea v-model="taskForm.technologyRequirements"></textarea>
                  </div>
                  <div class="field">
                    <label>{{ t("olympiad.form.description") }}</label>
                    <textarea v-model="taskForm.description" required></textarea>
                  </div>
                  <div class="field">
                    <label>{{ tx("Обов'язкові критерії (по одному в рядку)", "Must-have criteria (one per line)") }}</label>
                    <textarea v-model="taskForm.mustHaveText"></textarea>
                  </div>
                </div>
                <button class="btn" type="submit" :disabled="submitting.task">
                  {{ submitting.task ? tx("Створення…", "Creating…") : t("admin.createTask") }}
                </button>
              </form>
            </div>
          </div>

          <SectionBlock :title="t('admin.taskOperations')" :description="tx('Змінюйте статус раунду, призначайте журі або завершуйте оцінювання.', 'Update round status, assign jury, or finish evaluation.')" :eyebrow="tx('Раунди', 'Rounds')">
            <div v-if="tasks.length" class="stack">
              <article v-for="task in tasks" :key="task.id" class="panel stack">
                <div class="toolbar">
                  <div>
                    <h3 class="title-sm">{{ task.title }}</h3>
                    <p class="text-soft">{{ task.description }}</p>
                  </div>
                  <StatusBadge :status="task.status" />
                </div>

                <div class="field-grid">
                  <div class="field">
                    <label>{{ tx("Статус", "Status") }}</label>
                    <select v-model="taskStatusMap[task.id]">
                      <option>DRAFT</option>
                      <option>ACTIVE</option>
                      <option>SUBMISSION_CLOSED</option>
                      <option>EVALUATED</option>
                    </select>
                  </div>
                  <div class="field">
                    <label>{{ tx("Оцінювачів на подання", "Evaluators per submission") }}</label>
                    <input v-model.number="assignmentMap[task.id].evaluatorsPerSubmission" type="number" min="1" />
                  </div>
                  <div class="field">
                    <label>{{ tx("Максимум призначень на члена журі", "Max assignments per jury member") }}</label>
                    <input v-model.number="assignmentMap[task.id].maxAssignmentsPerJury" type="number" min="1" />
                  </div>
                </div>

                <div class="btn-row">
                  <button class="btn-tonal" type="button" @click="updateTaskStatus(task.id)" :disabled="submitting[`task-status-${task.id}`]">
                    {{ submitting[`task-status-${task.id}`] ? tx("Оновлення…", "Updating…") : tx("Оновити статус", "Update status") }}
                  </button>
                  <button class="btn-tonal" type="button" @click="assignEvaluations(task.id)" :disabled="submitting[`assign-${task.id}`]">
                    {{ submitting[`assign-${task.id}`] ? tx("Призначення…", "Assigning…") : tx("Призначити оцінювання", "Assign evaluations") }}
                  </button>
                  <button class="btn-ghost" type="button" @click="finishEvaluation(task.id)" :disabled="submitting[`finish-${task.id}`]">
                    {{ submitting[`finish-${task.id}`] ? tx("Завершення…", "Finishing…") : tx("Завершити оцінювання", "Finish evaluation") }}
                  </button>
                </div>
              </article>
            </div>
            <div v-else class="empty-box">{{ tx("Поки що завдання не завантажено.", "No tasks loaded yet.") }}</div>
          </SectionBlock>

          <SectionBlock :title="t('admin.submissions')" :description="tx('Поточні подання турніру за всіма завданнями.', 'Current tournament submissions for all tasks.')" :eyebrow="tx('Вхідні', 'Incoming')">
            <div v-if="submissions.length" class="table-like">
              <div class="table-row" v-for="submission in submissions" :key="submission.id">
                <strong>{{ submission.teamName }}</strong>
                <span>{{ submission.taskTitle }}</span>
                <a
                  v-if="toSafeExternalUrl(submission.githubUrl)"
                  :href="toSafeExternalUrl(submission.githubUrl)"
                  target="_blank"
                  rel="noreferrer"
                >
                  {{ t("jury.repository") }}
                </a>
                <span>{{ submission.status }}</span>
              </div>
            </div>
            <div v-else class="empty-box">{{ tx("Поки що немає подань.", "No submissions yet.") }}</div>
          </SectionBlock>
        </template>
      </SectionBlock>
    </template>

    <div v-if="deleteDialog.open" class="admin-delete-modal-backdrop" @click="closeDeleteDialog">
      <section class="admin-delete-modal panel stack" @click.stop>
        <h3 class="title-sm">{{ t("admin.deleteDangerTitle") }}</h3>
        <p class="text-soft">{{ t("admin.deleteDangerCopy") }}</p>
        <p class="text-soft">
          {{ tx("Турнір", "Tournament") }}:
          <strong>{{ deleteDialog.tournament?.title }}</strong>
        </p>
        <div class="field">
          <label>{{ t("admin.deleteConfirmLabel") }}</label>
          <input v-model.trim="deleteDialog.confirmationText" type="text" :placeholder="t('admin.deleteTypeTitle')" />
        </div>
        <div class="error-box" v-if="deleteDialog.errorMessage">{{ deleteDialog.errorMessage }}</div>
        <div class="btn-row">
          <button class="btn-ghost" type="button" @click="closeDeleteDialog">{{ tx("Скасувати", "Cancel") }}</button>
          <button class="btn-danger" type="button" :disabled="!canConfirmDeleteDialog || submitting[`deleteTournamentQuick-${deleteDialog.tournament?.id}`]" @click="submitDeleteDialog">
            {{
              submitting[`deleteTournamentQuick-${deleteDialog.tournament?.id}`]
                ? tx("Видалення…", "Deleting…")
                : t("admin.deleteAction")
            }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";
import SectionBlock from "../components/SectionBlock.vue";
import StatusBadge from "../components/StatusBadge.vue";
import { api } from "../services/api";
import { authStore } from "../services/auth";
import { notifier } from "../services/notify";
import { getErrorMessage } from "../services/formatters";
import { t, tx } from "../services/i18n";
import { toSafeExternalUrl } from "../services/security";

const loading = ref(true);
const errorMessage = ref("");
const tournaments = ref([]);
const tasks = ref([]);
const submissions = ref([]);
const adminTeams = ref([]);
const selectedTournamentId = ref(null);
const tournamentStatus = ref("DRAFT");
const route = useRoute();
const instrumentQuery = ref("");
const instrumentStatusFilter = ref("ALL");
const inviteTeamId = ref(null);
const juryInviteLink = ref("");
const teamInviteLink = ref("");
const deleteConfirmationText = ref("");

const submitting = reactive({});
const messages = reactive({});
const errors = reactive({});
const taskStatusMap = reactive({});
const assignmentMap = reactive({});
const deleteDialog = reactive({
  open: false,
  tournament: null,
  confirmationText: "",
  errorMessage: ""
});
const editForm = reactive({
  title: "",
  description: "",
  rules: "",
  startAt: "",
  registrationStartAt: "",
  registrationEndAt: "",
  maxTeams: null,
  minimumRounds: 1,
  teamMinMembers: 1,
  teamMaxMembers: 1,
  hideTeamsUntilRegistrationEnds: false
});

const announcementForm = reactive({
  title: "",
  content: ""
});

const scheduleForm = reactive({
  title: "",
  description: "",
  startAt: defaultDateTime(24),
  endAt: defaultDateTime(26),
  link: ""
});

const taskForm = reactive({
  title: "",
  description: "",
  technologyRequirements: "",
  mustHaveText: "",
  additionalMaterialsUrl: "",
  startAt: defaultDateTime(96),
  deadlineAt: defaultDateTime(144)
});

const selectedTournament = computed(
  () => tournaments.value.find((tournament) => tournament.id === selectedTournamentId.value) || null
);

const tournamentsByStatus = computed(() =>
  tournaments.value.reduce(
    (acc, tournament) => {
      acc[tournament.status] = (acc[tournament.status] || 0) + 1;
      return acc;
    },
    { DRAFT: 0, REGISTRATION: 0, RUNNING: 0, FINISHED: 0 }
  )
);

const administrationStats = computed(() => [
  { label: tx("Турнірів", "Tournaments"), value: tournaments.value.length },
  { label: tx("Чернетки", "Drafts"), value: tournamentsByStatus.value.DRAFT || 0 },
  { label: tx("Реєстрація", "Registration"), value: tournamentsByStatus.value.REGISTRATION || 0 },
  { label: tx("Тривають", "Running"), value: tournamentsByStatus.value.RUNNING || 0 },
  { label: tx("Завершені", "Finished"), value: tournamentsByStatus.value.FINISHED || 0 },
  { label: tx("Раунди у вибраному", "Rounds in selected"), value: tasks.value.length },
  { label: tx("Сабміти у вибраному", "Submissions in selected"), value: submissions.value.length }
]);

const filteredInstrumentTournaments = computed(() => {
  const query = instrumentQuery.value.toLowerCase();
  return tournaments.value.filter((tournament) => {
    const title = (tournament.title || "").toLowerCase();
    const matchesStatus = instrumentStatusFilter.value === "ALL" || tournament.status === instrumentStatusFilter.value;
    const matchesQuery = !query || title.includes(query);
    return matchesStatus && matchesQuery;
  });
});

const instrumentPreviewTournaments = computed(() => filteredInstrumentTournaments.value.slice(0, 6));
const hiddenInstrumentCount = computed(() => Math.max(0, filteredInstrumentTournaments.value.length - instrumentPreviewTournaments.value.length));
const canDeleteSelectedTournament = computed(
  () => Boolean(selectedTournament.value) && deleteConfirmationText.value.trim() === selectedTournament.value.title
);
const showJuryInviteTools = computed(() => authStore.hasRole("ADMIN"));
const showUserInviteTools = computed(() => !showJuryInviteTools.value);
const inviteSectionDescription = computed(() =>
  showJuryInviteTools.value
    ? tx("Генеруйте одноразові посилання для ролі журі.", "Generate one-time links for jury role.")
    : tx("Генеруйте одноразові посилання для входу в конкретну команду.", "Generate one-time links for joining a specific team.")
);
const canConfirmDeleteDialog = computed(
  () =>
    Boolean(deleteDialog.tournament) &&
    deleteDialog.confirmationText.trim() === deleteDialog.tournament.title
);

function defaultDateTime(hoursFromNow = 0) {
  const date = new Date(Date.now() + hoursFromNow * 60 * 60 * 1000);
  const pad = (value) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

async function withSubmission(key, callback) {
  submitting[key] = true;
  errors[key] = "";
  try {
    await callback();
  } catch (error) {
    errors[key] = getErrorMessage(error);
    notifier.pushNotification(getErrorMessage(error), "error");
  } finally {
    submitting[key] = false;
  }
}

async function loadAdminData() {
  loading.value = true;
  errorMessage.value = "";
  try {
    tournaments.value = await api.public.tournaments();
    if (!tournaments.value.length) {
      selectedTournamentId.value = null;
      await loadSelectedTournamentData();
      return;
    }
    const queryTournamentId = getQueryTournamentId();
    if (queryTournamentId && tournaments.value.some((tournament) => tournament.id === queryTournamentId)) {
      selectedTournamentId.value = queryTournamentId;
    } else if (selectedTournamentId.value && !tournaments.value.some((tournament) => tournament.id === selectedTournamentId.value)) {
      selectedTournamentId.value = null;
    }
    await loadSelectedTournamentData();
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
  } finally {
    loading.value = false;
  }
}

function getQueryTournamentId() {
  const rawValue = Array.isArray(route.query.tournamentId) ? route.query.tournamentId[0] : route.query.tournamentId;
  const parsed = Number(rawValue);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

async function loadSelectedTournamentData() {
  adminTeams.value = [];
  inviteTeamId.value = null;
  deleteConfirmationText.value = "";
  if (!selectedTournamentId.value) {
    tasks.value = [];
    submissions.value = [];
    return;
  }
  tasks.value = [];
  submissions.value = [];
  try {
    const [submissionsData, publicTasks, tournamentData, teamsData] = await Promise.all([
      api.admin.listSubmissions(selectedTournamentId.value),
      api.public.tasks(selectedTournamentId.value),
      api.public.tournament(selectedTournamentId.value),
      api.admin.teams(selectedTournamentId.value)
    ]);
    submissions.value = submissionsData;
    tasks.value = publicTasks;
    adminTeams.value = teamsData;
    if (inviteTeamId.value === null || !adminTeams.value.some((team) => team.id === inviteTeamId.value)) {
      inviteTeamId.value = adminTeams.value[0]?.id ?? null;
    }
    tournamentStatus.value = tournamentData.status;
    editForm.title = tournamentData.title;
    editForm.description = tournamentData.description;
    editForm.rules = tournamentData.rules || "";
    editForm.startAt = tournamentData.startAt || "";
    editForm.registrationStartAt = tournamentData.registrationStartAt || "";
    editForm.registrationEndAt = tournamentData.registrationEndAt || "";
    editForm.maxTeams = tournamentData.maxTeams ?? null;
    editForm.minimumRounds = tournamentData.minimumRounds;
    editForm.teamMinMembers = tournamentData.teamMinMembers;
    editForm.teamMaxMembers = tournamentData.teamMaxMembers;
    editForm.hideTeamsUntilRegistrationEnds = tournamentData.hideTeamsUntilRegistrationEnds;

    const index = tournaments.value.findIndex((tournament) => tournament.id === tournamentData.id);
    if (index >= 0) {
      tournaments.value[index] = tournamentData;
    }

    tasks.value.forEach((task) => {
      taskStatusMap[task.id] = task.status;
      assignmentMap[task.id] = assignmentMap[task.id] || {
        evaluatorsPerSubmission: 2,
        maxAssignmentsPerJury: 5
      };
    });
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
    notifier.pushNotification(errorMessage.value, "error");
    throw error;
  }
}

async function handleTournamentSelectionChange() {
  try {
    await loadSelectedTournamentData();
  } catch {
    // loadSelectedTournamentData already reports the error
  }
}

async function focusTournament(tournamentId) {
  selectedTournamentId.value = tournamentId;
  await handleTournamentSelectionChange();
}

function openDeleteDialog(tournament) {
  deleteDialog.open = true;
  deleteDialog.tournament = { id: tournament.id, title: tournament.title };
  deleteDialog.confirmationText = "";
  deleteDialog.errorMessage = "";
}

function closeDeleteDialog() {
  deleteDialog.open = false;
  deleteDialog.tournament = null;
  deleteDialog.confirmationText = "";
  deleteDialog.errorMessage = "";
}

async function submitDeleteDialog() {
  if (!deleteDialog.tournament || !canConfirmDeleteDialog.value) {
    return;
  }

  const { id, title } = deleteDialog.tournament;
  deleteDialog.errorMessage = "";

  await withSubmission(`deleteTournamentQuick-${id}`, async () => {
    await api.admin.deleteTournament(id, { confirmationText: title });
    if (selectedTournamentId.value === id) {
      selectedTournamentId.value = null;
    }
    deleteConfirmationText.value = "";
    notifier.pushNotification(t("admin.deleteSuccess"), "success");
    await loadAdminData();
  });

  const requestError = errors[`deleteTournamentQuick-${id}`];
  if (requestError) {
    deleteDialog.errorMessage = requestError;
    return;
  }

  closeDeleteDialog();
}

async function updateTournamentDetails() {
  if (!selectedTournament.value) {
    return;
  }
  await withSubmission("updateTournament", async () => {
    await api.admin.updateTournament(selectedTournament.value.id, {
      ...editForm,
      maxTeams: editForm.maxTeams || null
    });
    notifier.pushNotification(tx("Дані турніру оновлено.", "Tournament data updated."), "success");
    await loadAdminData();
  });
}

async function updateTournamentStatus() {
  if (!selectedTournament.value) {
    return;
  }
  await withSubmission("updateStatus", async () => {
    await api.admin.updateTournamentStatus(selectedTournament.value.id, tournamentStatus.value);
    notifier.pushNotification(t("admin.statusUpdated"), "success");
    await loadAdminData();
  });
}

async function deleteTournament() {
  if (!selectedTournament.value || !canDeleteSelectedTournament.value) {
    return;
  }
  await withSubmission("deleteTournament", async () => {
    await api.admin.deleteTournament(selectedTournament.value.id, {
      confirmationText: deleteConfirmationText.value.trim()
    });
    deleteConfirmationText.value = "";
    notifier.pushNotification(t("admin.deleteSuccess"), "success");
    await loadAdminData();
  });
}

async function createAnnouncement() {
  if (!selectedTournament.value) {
    return;
  }
  await withSubmission("announcement", async () => {
    await api.admin.createAnnouncement(selectedTournament.value.id, announcementForm);
    notifier.pushNotification(tx("Оголошення опубліковано.", "Announcement published."), "success");
    announcementForm.title = "";
    announcementForm.content = "";
  });
}

async function createScheduleEvent() {
  if (!selectedTournament.value) {
    return;
  }
  await withSubmission("schedule", async () => {
    await api.admin.createScheduleEvent(selectedTournament.value.id, scheduleForm);
    notifier.pushNotification(t("admin.scheduleCreated"), "success");
  });
}

async function createTask() {
  if (!selectedTournament.value) {
    return;
  }
  await withSubmission("task", async () => {
    const task = await api.admin.createTask(selectedTournament.value.id, {
      title: taskForm.title,
      description: taskForm.description,
      technologyRequirements: taskForm.technologyRequirements,
      mustHaveCriteria: taskForm.mustHaveText
        .split("\n")
        .map((entry) => entry.trim())
        .filter(Boolean),
      additionalMaterialsUrl: taskForm.additionalMaterialsUrl,
      startAt: taskForm.startAt,
      deadlineAt: taskForm.deadlineAt
    });
    tasks.value.unshift(task);
    taskStatusMap[task.id] = task.status;
    assignmentMap[task.id] = { evaluatorsPerSubmission: 2, maxAssignmentsPerJury: 5 };
    notifier.pushNotification(tx("Завдання створено.", "Task created."), "success");
  });
}

async function updateTaskStatus(taskId) {
  await withSubmission(`task-status-${taskId}`, async () => {
    const task = await api.admin.updateTaskStatus(taskId, taskStatusMap[taskId]);
    const index = tasks.value.findIndex((item) => item.id === taskId);
    if (index >= 0) {
      tasks.value[index] = task;
    }
    notifier.pushNotification(tx("Статус завдання оновлено.", "Task status updated."), "success");
  });
}

async function assignEvaluations(taskId) {
  await withSubmission(`assign-${taskId}`, async () => {
    const result = await api.admin.assignEvaluations(taskId, assignmentMap[taskId]);
    notifier.pushNotification(tx(`Створено ${result.length} призначень журі.`, `Created ${result.length} jury assignments.`), "success");
  });
}

async function finishEvaluation(taskId) {
  await withSubmission(`finish-${taskId}`, async () => {
    await api.admin.finishEvaluation(taskId);
    notifier.pushNotification(tx("Завдання позначено як оцінене.", "Task marked as evaluated."), "success");
    await loadSelectedTournamentData();
  });
}

async function downloadLeaderboard() {
  if (!selectedTournament.value) {
    return;
  }
  try {
    const file = await api.admin.exportLeaderboard(selectedTournament.value.id);
    const url = URL.createObjectURL(file.blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = file.filename;
    link.click();
    URL.revokeObjectURL(url);
    notifier.pushNotification(t("admin.leaderboardStarted"), "success");
  } catch (error) {
    notifier.pushNotification(getErrorMessage(error), "error");
  }
}

function toAbsoluteInviteLink(invitePath) {
  return new URL(invitePath, window.location.origin).toString();
}

async function createJuryInvite() {
  await withSubmission("createJuryInvite", async () => {
    const response = await api.admin.createJuryInvite();
    juryInviteLink.value = toAbsoluteInviteLink(response.invitePath);
    notifier.pushNotification(t("jury.addSuccess"), "success");
  });
}

async function createTeamInvite() {
  if (!inviteTeamId.value) {
    notifier.pushNotification(tx("Спочатку оберіть команду.", "Select a team first."), "error");
    return;
  }
  await withSubmission("createTeamInvite", async () => {
    const response = await api.admin.createTeamInvite(inviteTeamId.value);
    teamInviteLink.value = toAbsoluteInviteLink(response.invitePath);
    notifier.pushNotification(tx("Запрошення до команди створено.", "Team invite created."), "success");
  });
}

async function copyInviteLink(link) {
  try {
    await navigator.clipboard.writeText(link);
    notifier.pushNotification(t("jury.copySuccess"), "success");
  } catch (error) {
    notifier.pushNotification(getErrorMessage(error), "error");
  }
}

onMounted(loadAdminData);
</script>
