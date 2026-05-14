<template>
  <div class="page-grid">
    <section class="hero-card stack">
      <span class="eyebrow">{{ t("jury.workspace") }}</span>
      <h1 class="title-lg">{{ t("jury.title") }}</h1>
      <p class="text-soft">{{ t("jury.copy") }}</p>
    </section>

    <div v-if="loading" class="panel">{{ t("jury.loading") }}</div>
    <div v-else-if="errorMessage" class="error-box">{{ errorMessage }}</div>
    <SectionBlock
      v-else
      :title="t('jury.assigned')"
      :description="t('jury.assignedDesc')"
      :eyebrow="tx('Черга', 'Queue')"
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
            <a
              v-if="toSafeExternalUrl(assignment.submission.githubUrl)"
              class="btn-tonal"
              :href="toSafeExternalUrl(assignment.submission.githubUrl)"
              target="_blank"
              rel="noreferrer"
            >
              {{ t("jury.repository") }}
            </a>
            <a
              v-if="toSafeExternalUrl(assignment.submission.demoVideoUrl)"
              class="btn-tonal"
              :href="toSafeExternalUrl(assignment.submission.demoVideoUrl)"
              target="_blank"
              rel="noreferrer"
            >
              {{ t("jury.demo") }}
            </a>
            <a
              v-if="toSafeExternalUrl(assignment.submission.liveDemoUrl)"
              class="btn-tonal"
              :href="toSafeExternalUrl(assignment.submission.liveDemoUrl)"
              target="_blank"
              rel="noreferrer"
            >
              {{ t("jury.live") }}
            </a>
          </div>

          <form class="stack" @submit.prevent="submitEvaluation(assignment.assignmentId)">
            <div class="field-grid">
              <div v-for="field in scoreFields" :key="field.key" class="field">
                <label>{{ field.label }}</label>
                <input v-model.number="forms[assignment.assignmentId][field.key]" type="number" min="0" max="100" required />
              </div>
              <div class="field">
                <label>{{ t("jury.comment") }}</label>
                <MarkdownEditorField
                  v-model="forms[assignment.assignmentId].comment"
                  :label="t('jury.comment')"
                  :button-text="tx('Редагувати коментар у Markdown', 'Edit comment in Markdown')"
                />
              </div>
            </div>

            <div class="success-box" v-if="messages[assignment.assignmentId]">{{ messages[assignment.assignmentId] }}</div>
            <div class="error-box" v-if="errors[assignment.assignmentId]">{{ errors[assignment.assignmentId] }}</div>

            <div class="btn-row">
              <button class="btn" type="submit" :disabled="saving[assignment.assignmentId]">
                {{ saving[assignment.assignmentId] ? t("jury.saving") : t("jury.saveEvaluation") }}
              </button>
              <span class="stat-chip" v-if="assignment.evaluation">{{ t("jury.currentTotal") }}: {{ assignment.evaluation.totalScore.toFixed(2) }}</span>
            </div>
          </form>
        </article>
      </div>
      <div v-else class="empty-box">{{ t("common.none") }}</div>
    </SectionBlock>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import MarkdownEditorField from "../components/MarkdownEditorField.vue";
import SectionBlock from "../components/SectionBlock.vue";
import StatusBadge from "../components/StatusBadge.vue";
import { api } from "../services/api";
import { notifier } from "../services/notify";
import { getErrorMessage } from "../services/formatters";
import { t, tx } from "../services/i18n";
import { toSafeExternalUrl } from "../services/security";

const assignments = ref([]);
const loading = ref(true);
const errorMessage = ref("");
const forms = reactive({});
const errors = reactive({});
const messages = reactive({});
const saving = reactive({});

const scoreFields = computed(() => [
  { key: "backendScore", label: tx("Бекенд", "Backend") },
  { key: "databaseScore", label: tx("База даних", "Database") },
  { key: "frontendScore", label: tx("Фронтенд", "Frontend") },
  { key: "mustHaveScore", label: tx("Обов'язково", "Must have") },
  { key: "functionalityScore", label: tx("Функціональність", "Functionality") },
  { key: "usabilityScore", label: tx("Зручність", "Usability") }
]);

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
    messages[assignmentId] = t("jury.evaluationSaved");
    notifier.pushNotification(t("jury.evaluationSaved"), "success");
  } catch (error) {
    errors[assignmentId] = getErrorMessage(error);
  } finally {
    saving[assignmentId] = false;
  }
}

onMounted(loadAssignments);
</script>
