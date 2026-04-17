<template>
  <div class="page-grid">
    <section class="hero-card stack">
      <span class="eyebrow">Admin dashboard</span>
      <div class="toolbar">
        <div class="stack-sm">
          <h1 class="title-lg">Operate tournaments from one control room.</h1>
          <p class="text-soft">
            Create tournaments and roles, add rounds, push announcements, assign jury reviews, and export rankings.
          </p>
        </div>
        <button class="btn-tonal" type="button" @click="loadAdminData">Refresh data</button>
      </div>
    </section>

    <div v-if="loading" class="panel">Loading admin data…</div>
    <div v-else-if="errorMessage" class="error-box">{{ errorMessage }}</div>
    <template v-else>
      <section class="split">
        <SectionBlock title="Create tournament" description="Define the registration window and team constraints." eyebrow="Setup">
          <form class="stack" @submit.prevent="createTournament">
            <div class="field-grid">
              <div class="field">
                <label>Title</label>
                <input v-model="tournamentForm.title" type="text" required />
              </div>
              <div class="field">
                <label>Start at</label>
                <input v-model="tournamentForm.startAt" type="datetime-local" />
              </div>
              <div class="field">
                <label>Registration start</label>
                <input v-model="tournamentForm.registrationStartAt" type="datetime-local" required />
              </div>
              <div class="field">
                <label>Registration end</label>
                <input v-model="tournamentForm.registrationEndAt" type="datetime-local" required />
              </div>
              <div class="field">
                <label>Max teams</label>
                <input v-model.number="tournamentForm.maxTeams" type="number" min="1" />
              </div>
              <div class="field">
                <label>Minimum rounds</label>
                <input v-model.number="tournamentForm.minimumRounds" type="number" min="1" required />
              </div>
              <div class="field">
                <label>Min team members</label>
                <input v-model.number="tournamentForm.teamMinMembers" type="number" min="1" required />
              </div>
              <div class="field">
                <label>Max team members</label>
                <input v-model.number="tournamentForm.teamMaxMembers" type="number" min="1" required />
              </div>
              <div class="field">
                <label>Rules</label>
                <textarea v-model="tournamentForm.rules"></textarea>
              </div>
              <div class="field">
                <label>Description</label>
                <textarea v-model="tournamentForm.description" required></textarea>
              </div>
            </div>

            <div class="field">
              <label>
                <input v-model="tournamentForm.hideTeamsUntilRegistrationEnds" type="checkbox" />
                Hide teams until registration ends
              </label>
            </div>

            <div class="success-box" v-if="messages.createTournament">{{ messages.createTournament }}</div>
            <div class="error-box" v-if="errors.createTournament">{{ errors.createTournament }}</div>

            <div class="btn-row">
              <button class="btn" type="submit" :disabled="submitting.createTournament">
                {{ submitting.createTournament ? "Creating…" : "Create tournament" }}
              </button>
            </div>
          </form>
        </SectionBlock>

        <SectionBlock title="Create user" description="Provision jury, organizer, team, or admin accounts." eyebrow="Roles">
          <form class="stack" @submit.prevent="createUser">
            <div class="field-grid">
              <div class="field">
                <label>First name</label>
                <input v-model="userForm.name" type="text" required />
              </div>
              <div class="field">
                <label>Last name</label>
                <input v-model="userForm.lastName" type="text" required />
              </div>
              <div class="field">
                <label>Email</label>
                <input v-model="userForm.email" type="email" required />
              </div>
              <div class="field">
                <label>Password</label>
                <input v-model="userForm.password" type="password" minlength="8" required />
              </div>
              <div class="field">
                <label>Role</label>
                <select v-model="userForm.role">
                  <option>JURY</option>
                  <option>TEAM</option>
                  <option>ORGANIZER</option>
                  <option>ADMIN</option>
                </select>
              </div>
            </div>

            <div class="success-box" v-if="messages.createUser">{{ messages.createUser }}</div>
            <div class="error-box" v-if="errors.createUser">{{ errors.createUser }}</div>

            <div class="btn-row">
              <button class="btn" type="submit" :disabled="submitting.createUser">
                {{ submitting.createUser ? "Creating…" : "Create user" }}
              </button>
            </div>
          </form>
        </SectionBlock>
      </section>

      <SectionBlock title="Tournament control" description="Pick a tournament and manage everything under it." eyebrow="Operations">
        <div class="toolbar">
          <div class="field" style="min-width: 280px">
            <label>Selected tournament</label>
            <select v-model.number="selectedTournamentId" @change="loadSelectedTournamentData">
              <option :value="null">Select tournament</option>
              <option v-for="tournament in tournaments" :key="tournament.id" :value="tournament.id">
                {{ tournament.title }} · {{ tournament.status }}
              </option>
            </select>
          </div>
          <div class="btn-row" v-if="selectedTournament">
            <RouterLink class="btn-tonal" :to="`/tournaments/${selectedTournament.id}`">Open public page</RouterLink>
            <button class="btn-ghost" type="button" @click="downloadLeaderboard">Export leaderboard CSV</button>
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
                <label>Update status</label>
                <select v-model="tournamentStatus">
                  <option>DRAFT</option>
                  <option>REGISTRATION</option>
                  <option>RUNNING</option>
                  <option>FINISHED</option>
                </select>
              </div>
              <div class="btn-row">
                <button class="btn" type="button" @click="updateTournamentStatus" :disabled="submitting.updateStatus">
                  {{ submitting.updateStatus ? "Updating…" : "Apply status" }}
                </button>
              </div>
            </div>

            <div class="panel stack">
              <h3 class="title-sm">Create announcement</h3>
              <form class="stack" @submit.prevent="createAnnouncement">
                <div class="field">
                  <label>Title</label>
                  <input v-model="announcementForm.title" type="text" required />
                </div>
                <div class="field">
                  <label>Content</label>
                  <textarea v-model="announcementForm.content" required></textarea>
                </div>
                <button class="btn" type="submit" :disabled="submitting.announcement">
                  {{ submitting.announcement ? "Publishing…" : "Publish" }}
                </button>
              </form>
            </div>
          </div>

          <div class="split">
            <div class="panel stack">
              <h3 class="title-sm">Create schedule event</h3>
              <form class="stack" @submit.prevent="createScheduleEvent">
                <div class="field-grid">
                  <div class="field">
                    <label>Title</label>
                    <input v-model="scheduleForm.title" type="text" required />
                  </div>
                  <div class="field">
                    <label>Link</label>
                    <input v-model="scheduleForm.link" type="url" />
                  </div>
                  <div class="field">
                    <label>Start</label>
                    <input v-model="scheduleForm.startAt" type="datetime-local" required />
                  </div>
                  <div class="field">
                    <label>End</label>
                    <input v-model="scheduleForm.endAt" type="datetime-local" required />
                  </div>
                  <div class="field">
                    <label>Description</label>
                    <textarea v-model="scheduleForm.description"></textarea>
                  </div>
                </div>
                <button class="btn" type="submit" :disabled="submitting.schedule">
                  {{ submitting.schedule ? "Saving…" : "Create event" }}
                </button>
              </form>
            </div>

            <div class="panel stack">
              <h3 class="title-sm">Create task</h3>
              <form class="stack" @submit.prevent="createTask">
                <div class="field-grid">
                  <div class="field">
                    <label>Title</label>
                    <input v-model="taskForm.title" type="text" required />
                  </div>
                  <div class="field">
                    <label>Materials URL</label>
                    <input v-model="taskForm.additionalMaterialsUrl" type="url" />
                  </div>
                  <div class="field">
                    <label>Start</label>
                    <input v-model="taskForm.startAt" type="datetime-local" required />
                  </div>
                  <div class="field">
                    <label>Deadline</label>
                    <input v-model="taskForm.deadlineAt" type="datetime-local" required />
                  </div>
                  <div class="field">
                    <label>Technology requirements</label>
                    <textarea v-model="taskForm.technologyRequirements"></textarea>
                  </div>
                  <div class="field">
                    <label>Description</label>
                    <textarea v-model="taskForm.description" required></textarea>
                  </div>
                  <div class="field">
                    <label>Must-have criteria (one per line)</label>
                    <textarea v-model="taskForm.mustHaveText"></textarea>
                  </div>
                </div>
                <button class="btn" type="submit" :disabled="submitting.task">
                  {{ submitting.task ? "Creating…" : "Create task" }}
                </button>
              </form>
            </div>
          </div>

          <SectionBlock title="Task operations" description="Change round status, assign jury, or finish evaluation." eyebrow="Rounds">
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
                    <label>Status</label>
                    <select v-model="taskStatusMap[task.id]">
                      <option>DRAFT</option>
                      <option>ACTIVE</option>
                      <option>SUBMISSION_CLOSED</option>
                      <option>EVALUATED</option>
                    </select>
                  </div>
                  <div class="field">
                    <label>Evaluators per submission</label>
                    <input v-model.number="assignmentMap[task.id].evaluatorsPerSubmission" type="number" min="1" />
                  </div>
                  <div class="field">
                    <label>Max assignments per jury</label>
                    <input v-model.number="assignmentMap[task.id].maxAssignmentsPerJury" type="number" min="1" />
                  </div>
                </div>

                <div class="btn-row">
                  <button class="btn-tonal" type="button" @click="updateTaskStatus(task.id)" :disabled="submitting[`task-status-${task.id}`]">
                    {{ submitting[`task-status-${task.id}`] ? "Updating…" : "Update status" }}
                  </button>
                  <button class="btn-tonal" type="button" @click="assignEvaluations(task.id)" :disabled="submitting[`assign-${task.id}`]">
                    {{ submitting[`assign-${task.id}`] ? "Assigning…" : "Assign evaluations" }}
                  </button>
                  <button class="btn-ghost" type="button" @click="finishEvaluation(task.id)" :disabled="submitting[`finish-${task.id}`]">
                    {{ submitting[`finish-${task.id}`] ? "Finishing…" : "Finish evaluation" }}
                  </button>
                </div>
              </article>
            </div>
            <div v-else class="empty-box">No tasks loaded yet.</div>
          </SectionBlock>

          <SectionBlock title="Submissions" description="Current tournament submissions across tasks." eyebrow="Inbound">
            <div v-if="submissions.length" class="table-like">
              <div class="table-row" v-for="submission in submissions" :key="submission.id">
                <strong>{{ submission.teamName }}</strong>
                <span>{{ submission.taskTitle }}</span>
                <a :href="submission.githubUrl" target="_blank" rel="noreferrer">Repository</a>
                <span>{{ submission.status }}</span>
              </div>
            </div>
            <div v-else class="empty-box">No submissions yet.</div>
          </SectionBlock>
        </template>
      </SectionBlock>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import SectionBlock from "../components/SectionBlock.vue";
import StatusBadge from "../components/StatusBadge.vue";
import { api } from "../services/api";
import { notifier } from "../services/notify";
import { getErrorMessage } from "../services/formatters";

const loading = ref(true);
const errorMessage = ref("");
const tournaments = ref([]);
const tasks = ref([]);
const submissions = ref([]);
const selectedTournamentId = ref(null);
const tournamentStatus = ref("DRAFT");

const submitting = reactive({});
const messages = reactive({});
const errors = reactive({});
const taskStatusMap = reactive({});
const assignmentMap = reactive({});

const tournamentForm = reactive({
  title: "",
  description: "",
  rules: "",
  startAt: "",
  registrationStartAt: defaultDateTime(24),
  registrationEndAt: defaultDateTime(72),
  maxTeams: 20,
  minimumRounds: 1,
  teamMinMembers: 2,
  teamMaxMembers: 5,
  hideTeamsUntilRegistrationEnds: false
});

const userForm = reactive({
  name: "",
  lastName: "",
  email: "",
  password: "",
  role: "JURY"
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
    if (!selectedTournamentId.value && tournaments.value.length) {
      selectedTournamentId.value = tournaments.value[0].id;
    }
    if (selectedTournamentId.value) {
      await loadSelectedTournamentData();
    }
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
  } finally {
    loading.value = false;
  }
}

async function loadSelectedTournamentData() {
  if (!selectedTournamentId.value) {
    tasks.value = [];
    submissions.value = [];
    return;
  }
  const [submissionsData, publicTasks, tournamentData] = await Promise.all([
    api.admin.listSubmissions(selectedTournamentId.value),
    api.public.tasks(selectedTournamentId.value),
    api.public.tournament(selectedTournamentId.value)
  ]);
  submissions.value = submissionsData;
  tasks.value = publicTasks;
  tournamentStatus.value = tournamentData.status;

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
}

async function createTournament() {
  await withSubmission("createTournament", async () => {
    await api.admin.createTournament({
      ...tournamentForm,
      maxTeams: tournamentForm.maxTeams || null
    });
    messages.createTournament = "Tournament created.";
    notifier.pushNotification("Tournament created.", "success");
    await loadAdminData();
  });
}

async function createUser() {
  await withSubmission("createUser", async () => {
    await api.admin.createUser(userForm);
    messages.createUser = "User created.";
    notifier.pushNotification("User created.", "success");
  });
}

async function updateTournamentStatus() {
  if (!selectedTournament.value) {
    return;
  }
  await withSubmission("updateStatus", async () => {
    await api.admin.updateTournamentStatus(selectedTournament.value.id, tournamentStatus.value);
    notifier.pushNotification("Tournament status updated.", "success");
    await loadAdminData();
  });
}

async function createAnnouncement() {
  if (!selectedTournament.value) {
    return;
  }
  await withSubmission("announcement", async () => {
    await api.admin.createAnnouncement(selectedTournament.value.id, announcementForm);
    notifier.pushNotification("Announcement published.", "success");
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
    notifier.pushNotification("Schedule event created.", "success");
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
    notifier.pushNotification("Task created.", "success");
  });
}

async function updateTaskStatus(taskId) {
  await withSubmission(`task-status-${taskId}`, async () => {
    const task = await api.admin.updateTaskStatus(taskId, taskStatusMap[taskId]);
    const index = tasks.value.findIndex((item) => item.id === taskId);
    if (index >= 0) {
      tasks.value[index] = task;
    }
    notifier.pushNotification("Task status updated.", "success");
  });
}

async function assignEvaluations(taskId) {
  await withSubmission(`assign-${taskId}`, async () => {
    const result = await api.admin.assignEvaluations(taskId, assignmentMap[taskId]);
    notifier.pushNotification(`Created ${result.length} jury assignments.`, "success");
  });
}

async function finishEvaluation(taskId) {
  await withSubmission(`finish-${taskId}`, async () => {
    await api.admin.finishEvaluation(taskId);
    notifier.pushNotification("Task marked as evaluated.", "success");
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
    notifier.pushNotification("Leaderboard download started.", "success");
  } catch (error) {
    notifier.pushNotification(getErrorMessage(error), "error");
  }
}

onMounted(loadAdminData);
</script>
