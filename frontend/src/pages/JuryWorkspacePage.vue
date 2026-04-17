<template>
  <div class="page-grid">
    <section class="hero-card stack">
      <span class="eyebrow">Jury workspace</span>
      <h1 class="title-lg">Score assigned submissions with a transparent rubric.</h1>
      <p class="text-soft">
        Every assignment includes repository links, demo video, and a structured grading form that maps to the backend evaluation model.
      </p>
    </section>

    <div v-if="loading" class="panel">Loading assignments…</div>
    <div v-else-if="errorMessage" class="error-box">{{ errorMessage }}</div>
    <SectionBlock
      v-else
      title="Assigned reviews"
      description="Submit one evaluation per assignment. Saving again overwrites the previous score for that assignment."
      eyebrow="Queue"
    >
      <div v-if="assignments.length" class="stack">
        <article v-for="assignment in assignments" :key="assignment.assignmentId" class="panel stack">
          <div class="toolbar">
            <div>
              <h2 class="title-md">{{ assignment.submission.teamName }}</h2>
              <p class="text-soft">{{ assignment.submission.taskTitle }}</p>
            </div>
            <StatusBadge :status="assignment.status" />
          </div>

          <div class="btn-row">
            <a class="btn-tonal" :href="assignment.submission.githubUrl" target="_blank" rel="noreferrer">Repository</a>
            <a class="btn-tonal" :href="assignment.submission.demoVideoUrl" target="_blank" rel="noreferrer">Demo video</a>
            <a
              v-if="assignment.submission.liveDemoUrl"
              class="btn-tonal"
              :href="assignment.submission.liveDemoUrl"
              target="_blank"
              rel="noreferrer"
            >
              Live demo
            </a>
          </div>

          <form class="stack" @submit.prevent="submitEvaluation(assignment.assignmentId)">
            <div class="field-grid">
              <div v-for="field in scoreFields" :key="field.key" class="field">
                <label>{{ field.label }}</label>
                <input v-model.number="forms[assignment.assignmentId][field.key]" type="number" min="0" max="100" required />
              </div>
              <div class="field">
                <label>Comment</label>
                <textarea v-model="forms[assignment.assignmentId].comment"></textarea>
              </div>
            </div>

            <div class="success-box" v-if="messages[assignment.assignmentId]">{{ messages[assignment.assignmentId] }}</div>
            <div class="error-box" v-if="errors[assignment.assignmentId]">{{ errors[assignment.assignmentId] }}</div>

            <div class="btn-row">
              <button class="btn" type="submit" :disabled="saving[assignment.assignmentId]">
                {{ saving[assignment.assignmentId] ? "Saving…" : "Save evaluation" }}
              </button>
              <span class="stat-chip" v-if="assignment.evaluation">Current total: {{ assignment.evaluation.totalScore.toFixed(2) }}</span>
            </div>
          </form>
        </article>
      </div>
      <div v-else class="empty-box">No assignments available.</div>
    </SectionBlock>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import SectionBlock from "../components/SectionBlock.vue";
import StatusBadge from "../components/StatusBadge.vue";
import { api } from "../services/api";
import { notifier } from "../services/notify";
import { getErrorMessage } from "../services/formatters";

const assignments = ref([]);
const loading = ref(true);
const errorMessage = ref("");
const forms = reactive({});
const errors = reactive({});
const messages = reactive({});
const saving = reactive({});

const scoreFields = [
  { key: "backendScore", label: "Backend" },
  { key: "databaseScore", label: "Database" },
  { key: "frontendScore", label: "Frontend" },
  { key: "mustHaveScore", label: "Must have" },
  { key: "functionalityScore", label: "Functionality" },
  { key: "usabilityScore", label: "Usability" }
];

function makeForm(assignment) {
  return {
    backendScore: assignment.evaluation?.backendScore ?? 0,
    databaseScore: assignment.evaluation?.databaseScore ?? 0,
    frontendScore: assignment.evaluation?.frontendScore ?? 0,
    mustHaveScore: assignment.evaluation?.mustHaveScore ?? 0,
    functionalityScore: assignment.evaluation?.functionalityScore ?? 0,
    usabilityScore: assignment.evaluation?.usabilityScore ?? 0,
    comment: assignment.evaluation?.comment ?? ""
  };
}

async function loadAssignments() {
  loading.value = true;
  errorMessage.value = "";
  try {
    assignments.value = await api.jury.assignments();
    assignments.value.forEach((assignment) => {
      forms[assignment.assignmentId] = makeForm(assignment);
    });
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
  } finally {
    loading.value = false;
  }
}

async function submitEvaluation(assignmentId) {
  saving[assignmentId] = true;
  errors[assignmentId] = "";
  messages[assignmentId] = "";
  try {
    const evaluation = await api.jury.submitEvaluation(assignmentId, forms[assignmentId]);
    const assignment = assignments.value.find((item) => item.assignmentId === assignmentId);
    if (assignment) {
      assignment.evaluation = evaluation;
      assignment.status = "COMPLETED";
    }
    messages[assignmentId] = "Evaluation saved.";
    notifier.pushNotification("Evaluation submitted.", "success");
  } catch (error) {
    errors[assignmentId] = getErrorMessage(error);
  } finally {
    saving[assignmentId] = false;
  }
}

onMounted(loadAssignments);
</script>
