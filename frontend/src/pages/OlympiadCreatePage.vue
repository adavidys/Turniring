<template>
  <div class="page-grid">
    <section class="hero-card stack">
      <span class="eyebrow">{{ t("nav.olympiads") }}</span>
      <div class="toolbar">
        <div class="stack-sm">
          <h1 class="title-lg">{{ t("olympiad.createTitle") }}</h1>
          <p class="text-soft">{{ t("olympiad.createCopy") }}</p>
        </div>
        <RouterLink class="btn-tonal" to="/admin">{{ t("admin.dashboard") }}</RouterLink>
      </div>
    </section>

    <form class="stack" @submit.prevent="createOlympiad">
      <section class="panel stack-sm">
        <div class="btn-row">
          <button
            v-for="step in stepItems"
            :key="step.id"
            class="btn-ghost"
            :class="{ 'is-active': currentStep === step.id }"
            type="button"
            :disabled="!canOpenStep(step.id)"
            @click="goToStep(step.id)"
          >
            {{ t(step.eyebrow) }}
          </button>
        </div>
        <p class="text-soft">{{ tx("Редагуйте олімпіаду крок за кроком.", "Edit the olympiad step by step.") }}</p>
      </section>

      <div class="error-box" v-if="errorMessage && currentStep !== 4">{{ errorMessage }}</div>

      <SectionBlock v-if="currentStep === 1" :title="t('olympiad.step1Title')" :description="t('olympiad.step1Desc')" :eyebrow="t('olympiad.step1Eyebrow')">
        <div class="field-grid">
          <div class="field">
            <label>{{ t("olympiad.form.title") }}</label>
            <input v-model.trim="form.title" type="text" required minlength="3" maxlength="120" :class="{ 'is-invalid': fieldErrors.title }" />
            <span class="field-error" v-if="fieldErrors.title">{{ fieldErrors.title }}</span>
          </div>
          <div class="field">
            <label>{{ t("olympiad.form.description") }}</label>
            <textarea v-model.trim="form.description" required minlength="20" maxlength="4000" :class="{ 'is-invalid': fieldErrors.description }"></textarea>
            <span class="field-error" v-if="fieldErrors.description">{{ fieldErrors.description }}</span>
          </div>
          <div class="field">
            <label>{{ t("olympiad.form.rules") }}</label>
            <textarea v-model.trim="form.rules" maxlength="5000" :class="{ 'is-invalid': fieldErrors.rules }"></textarea>
            <span class="field-error" v-if="fieldErrors.rules">{{ fieldErrors.rules }}</span>
          </div>
        </div>
        <div class="btn-row">
          <button class="btn" type="button" @click="goNext(2)">
            {{ tx("Далі", "Next") }}
          </button>
        </div>
      </SectionBlock>

      <SectionBlock v-else-if="currentStep === 2" :title="t('olympiad.step2Title')" :description="t('olympiad.step2Desc')" :eyebrow="t('olympiad.step2Eyebrow')">
        <div class="field-grid">
          <div class="field">
            <label>{{ t("olympiad.form.start") }} · {{ tx("Дата", "Date") }}</label>
            <input v-model="form.startDate" type="date" required :class="{ 'is-invalid': fieldErrors.startDate }" />
            <span class="field-error" v-if="fieldErrors.startDate">{{ fieldErrors.startDate }}</span>
          </div>
          <div class="field">
            <label>{{ t("olympiad.form.start") }} · {{ tx("Час", "Time") }}</label>
            <input v-model="form.startTime" type="time" required :class="{ 'is-invalid': fieldErrors.startTime }" />
            <span class="field-error" v-if="fieldErrors.startTime">{{ fieldErrors.startTime }}</span>
          </div>
        </div>
        <div class="field-grid">
          <div class="field">
            <label>{{ t("olympiad.form.registrationStart") }} · {{ tx("Дата", "Date") }}</label>
            <input v-model="form.registrationStartDate" type="date" required :class="{ 'is-invalid': fieldErrors.registrationStartDate }" />
            <span class="field-error" v-if="fieldErrors.registrationStartDate">{{ fieldErrors.registrationStartDate }}</span>
          </div>
          <div class="field">
            <label>{{ t("olympiad.form.registrationStart") }} · {{ tx("Час", "Time") }}</label>
            <input v-model="form.registrationStartTime" type="time" required :class="{ 'is-invalid': fieldErrors.registrationStartTime }" />
            <span class="field-error" v-if="fieldErrors.registrationStartTime">{{ fieldErrors.registrationStartTime }}</span>
          </div>
        </div>
        <div class="field-grid">
          <div class="field">
            <label>{{ t("olympiad.form.registrationEnd") }} · {{ tx("Дата", "Date") }}</label>
            <input v-model="form.registrationEndDate" type="date" required :class="{ 'is-invalid': fieldErrors.registrationEndDate }" />
            <span class="field-error" v-if="fieldErrors.registrationEndDate">{{ fieldErrors.registrationEndDate }}</span>
          </div>
          <div class="field">
            <label>{{ t("olympiad.form.registrationEnd") }} · {{ tx("Час", "Time") }}</label>
            <input v-model="form.registrationEndTime" type="time" required :class="{ 'is-invalid': fieldErrors.registrationEndTime }" />
            <span class="field-error" v-if="fieldErrors.registrationEndTime">{{ fieldErrors.registrationEndTime }}</span>
          </div>
        </div>
        <div class="btn-row">
          <button class="btn-ghost" type="button" @click="goToStep(1)">{{ tx("Назад", "Back") }}</button>
          <button class="btn" type="button" @click="goNext(3)">
            {{ tx("Далі", "Next") }}
          </button>
        </div>
      </SectionBlock>

      <SectionBlock v-else-if="currentStep === 3" :title="t('olympiad.step3Title')" :description="t('olympiad.step3Desc')" :eyebrow="t('olympiad.step3Eyebrow')">
        <div class="field-grid">
          <div class="field">
            <label>{{ t("olympiad.form.maxTeams") }}</label>
            <input v-model.number="form.maxTeams" type="number" min="1" :class="{ 'is-invalid': fieldErrors.maxTeams }" />
            <span class="field-error" v-if="fieldErrors.maxTeams">{{ fieldErrors.maxTeams }}</span>
          </div>
          <div class="field">
            <label>{{ t("olympiad.form.minimumRounds") }}</label>
            <input v-model.number="form.minimumRounds" type="number" min="1" required :class="{ 'is-invalid': fieldErrors.minimumRounds }" />
            <span class="field-error" v-if="fieldErrors.minimumRounds">{{ fieldErrors.minimumRounds }}</span>
          </div>
          <div class="field">
            <label>{{ t("olympiad.form.teamMin") }}</label>
            <input v-model.number="form.teamMinMembers" type="number" min="1" required :class="{ 'is-invalid': fieldErrors.teamMinMembers }" />
            <span class="field-error" v-if="fieldErrors.teamMinMembers">{{ fieldErrors.teamMinMembers }}</span>
          </div>
          <div class="field">
            <label>{{ t("olympiad.form.teamMax") }}</label>
            <input v-model.number="form.teamMaxMembers" type="number" min="1" required :class="{ 'is-invalid': fieldErrors.teamMaxMembers }" />
            <span class="field-error" v-if="fieldErrors.teamMaxMembers">{{ fieldErrors.teamMaxMembers }}</span>
          </div>
        </div>

        <div class="field">
          <label>
            <input v-model="form.hideTeamsUntilRegistrationEnds" type="checkbox" />
            {{ t("olympiad.form.hideTeams") }}
          </label>
        </div>
        <div class="btn-row">
          <button class="btn-ghost" type="button" @click="goToStep(2)">{{ tx("Назад", "Back") }}</button>
          <button class="btn" type="button" @click="goNext(4)">
            {{ tx("До перевірки", "Review") }}
          </button>
        </div>
      </SectionBlock>

      <SectionBlock v-else :title="t('olympiad.step4Title')" :description="t('olympiad.step4Desc')" :eyebrow="t('olympiad.step4Eyebrow')">
        <div class="stat-row">
          <span class="stat-chip">{{ t("olympiad.form.title") }}: {{ form.title || "-" }}</span>
          <span class="stat-chip">{{ t("olympiad.form.registrationStart") }}: {{ dateTimeValues.registrationStartAt || "-" }}</span>
          <span class="stat-chip">{{ t("olympiad.form.registrationEnd") }}: {{ dateTimeValues.registrationEndAt || "-" }}</span>
          <span class="stat-chip">{{ t("olympiad.form.maxTeams") }}: {{ form.maxTeams || "-" }}</span>
        </div>

        <div class="stack-sm">
          <h3 class="title-sm">{{ tx("Прев’ю олімпіади", "Olympiad preview") }}</h3>
          <div style="pointer-events: none;">
            <TournamentCard :tournament="previewTournament" />
          </div>
        </div>

        <div class="success-box" v-if="message">{{ message }}</div>
        <div class="error-box" v-if="errorMessage">{{ errorMessage }}</div>

        <div class="panel stack-sm" v-if="createdOlympiad">
          <div class="text-soft">{{ t("olympiad.createdHint") }}</div>
          <div class="btn-row">
            <RouterLink class="btn-tonal" :to="`/tournaments/${createdOlympiad.id}`">
              {{ t("admin.openPublic") }}
            </RouterLink>
            <RouterLink class="btn-ghost" :to="`/admin?tournamentId=${createdOlympiad.id}`">
              {{ t("olympiad.openInAdmin") }}
            </RouterLink>
          </div>
        </div>

        <div class="btn-row">
          <button class="btn-ghost" type="button" @click="goToStep(3)">{{ tx("Назад", "Back") }}</button>
          <button class="btn" type="submit" :disabled="submitting">
            {{ submitting ? t("olympiad.creating") : t("olympiad.createAction") }}
          </button>
        </div>
      </SectionBlock>
    </form>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import SectionBlock from "../components/SectionBlock.vue";
import TournamentCard from "../components/TournamentCard.vue";
import { api } from "../services/api";
import { notifier } from "../services/notify";
import { getErrorMessage } from "../services/formatters";
import { t, tx } from "../services/i18n";

const submitting = ref(false);
const message = ref("");
const errorMessage = ref("");
const createdOlympiad = ref(null);
const currentStep = ref(1);
const stepItems = [
  { id: 1, eyebrow: "olympiad.step1Eyebrow" },
  { id: 2, eyebrow: "olympiad.step2Eyebrow" },
  { id: 3, eyebrow: "olympiad.step3Eyebrow" },
  { id: 4, eyebrow: "olympiad.step4Eyebrow" }
];

const form = reactive({
  title: "",
  description: "",
  rules: "",
  startDate: "",
  startTime: "",
  registrationStartDate: defaultDateTimeParts(24).date,
  registrationStartTime: defaultDateTimeParts(24).time,
  registrationEndDate: defaultDateTimeParts(72).date,
  registrationEndTime: defaultDateTimeParts(72).time,
  maxTeams: 20,
  minimumRounds: 1,
  teamMinMembers: 2,
  teamMaxMembers: 5,
  hideTeamsUntilRegistrationEnds: false
});

const dateTimeValues = computed(() => ({
  startAt: toDateTime(form.startDate, form.startTime),
  registrationStartAt: toDateTime(form.registrationStartDate, form.registrationStartTime),
  registrationEndAt: toDateTime(form.registrationEndDate, form.registrationEndTime)
}));

const previewTournament = computed(() => {
  const now = new Date();
  const registrationStart = dateTimeValues.value.registrationStartAt ? new Date(dateTimeValues.value.registrationStartAt) : null;
  const registrationEnd = dateTimeValues.value.registrationEndAt ? new Date(dateTimeValues.value.registrationEndAt) : null;
  const registrationOpen = Boolean(registrationStart && registrationEnd && now >= registrationStart && now <= registrationEnd);

  return {
    id: createdOlympiad.value?.id ?? 0,
    title: form.title || tx("Нова олімпіада", "New olympiad"),
    description: form.description || tx("Опис ще не заповнено.", "Description is not filled yet."),
    status: "DRAFT",
    registrationOpen,
    registeredTeams: 0,
    minimumRounds: form.minimumRounds || 1,
    registrationStartAt: dateTimeValues.value.registrationStartAt || "",
    registrationEndAt: dateTimeValues.value.registrationEndAt || "",
    startAt: dateTimeValues.value.startAt || ""
  };
});

const fieldErrors = computed(() => {
  const errors = {
    title: "",
    description: "",
    rules: "",
    startDate: "",
    startTime: "",
    registrationStartDate: "",
    registrationStartTime: "",
    registrationEndDate: "",
    registrationEndTime: "",
    maxTeams: "",
    minimumRounds: "",
    teamMinMembers: "",
    teamMaxMembers: ""
  };

  const title = form.title.trim();
  if (!title) {
    errors.title = tx("Заповніть назву олімпіади.", "Fill in olympiad title.");
  } else if (title.length < 3) {
    errors.title = tx("Назва має містити щонайменше 3 символи.", "Title must be at least 3 characters long.");
  }

  const description = form.description.trim();
  if (!description) {
    errors.description = tx("Заповніть опис олімпіади.", "Fill in olympiad description.");
  } else if (description.length < 20) {
    errors.description = tx("Опис має містити щонайменше 20 символів.", "Description must be at least 20 characters long.");
  }

  if (form.rules && form.rules.length > 5000) {
    errors.rules = tx("Правила не можуть перевищувати 5000 символів.", "Rules cannot exceed 5000 characters.");
  }

  if (!form.startDate) {
    errors.startDate = tx("Вкажіть дату початку турніру.", "Set tournament start date.");
  }
  if (!form.startTime) {
    errors.startTime = tx("Вкажіть час початку турніру.", "Set tournament start time.");
  }
  if (!form.registrationStartDate) {
    errors.registrationStartDate = tx("Вкажіть дату початку реєстрації.", "Set registration start date.");
  }
  if (!form.registrationStartTime) {
    errors.registrationStartTime = tx("Вкажіть час початку реєстрації.", "Set registration start time.");
  }
  if (!form.registrationEndDate) {
    errors.registrationEndDate = tx("Вкажіть дату завершення реєстрації.", "Set registration end date.");
  }
  if (!form.registrationEndTime) {
    errors.registrationEndTime = tx("Вкажіть час завершення реєстрації.", "Set registration end time.");
  }

  if (
    dateTimeValues.value.registrationStartAt &&
    dateTimeValues.value.registrationEndAt &&
    dateTimeValues.value.registrationEndAt <= dateTimeValues.value.registrationStartAt
  ) {
    errors.registrationEndTime = t("olympiad.validation.registrationOrder");
  }
  if (
    dateTimeValues.value.startAt &&
    dateTimeValues.value.registrationEndAt &&
    dateTimeValues.value.startAt < dateTimeValues.value.registrationEndAt
  ) {
    errors.startTime = t("olympiad.validation.startAfterRegistration");
  }

  if (!isPositiveInteger(form.minimumRounds)) {
    errors.minimumRounds = tx("Мінімум раундів має бути цілим числом від 1.", "Minimum rounds must be an integer starting from 1.");
  }
  if (!isPositiveInteger(form.teamMinMembers)) {
    errors.teamMinMembers = tx("Мінімум учасників команди має бути цілим числом від 1.", "Minimum team members must be an integer starting from 1.");
  }
  if (!isPositiveInteger(form.teamMaxMembers)) {
    errors.teamMaxMembers = tx("Максимум учасників команди має бути цілим числом від 1.", "Maximum team members must be an integer starting from 1.");
  } else if (isPositiveInteger(form.teamMinMembers) && Number(form.teamMaxMembers) < Number(form.teamMinMembers)) {
    errors.teamMaxMembers = t("olympiad.validation.teamMembersOrder");
  }
  if (form.maxTeams !== null && form.maxTeams !== "" && !isPositiveInteger(form.maxTeams)) {
    errors.maxTeams = tx("Максимум команд має бути цілим числом від 1 або порожнім.", "Max teams must be an integer starting from 1 or empty.");
  }

  return errors;
});

function canOpenStep(step) {
  if (step <= 1) {
    return true;
  }
  for (let index = 1; index < step; index += 1) {
    if (getStepValidationError(index)) {
      return false;
    }
  }
  return true;
}

function goToStep(step) {
  const targetStep = Math.max(1, Math.min(4, step));
  if (!canOpenStep(targetStep)) {
    const firstInvalid = findFirstInvalidStep(targetStep - 1);
    if (firstInvalid) {
      currentStep.value = firstInvalid.step;
      errorMessage.value = firstInvalid.message;
      return;
    }
    errorMessage.value = t("olympiad.stepLocked");
    return;
  }
  errorMessage.value = "";
  currentStep.value = targetStep;
}

function goNext(nextStep) {
  const currentError = getStepValidationError(currentStep.value);
  if (currentError) {
    errorMessage.value = currentError;
    return;
  }
  goToStep(nextStep);
}

function defaultDateTime(hoursFromNow = 0) {
  const date = new Date(Date.now() + hoursFromNow * 60 * 60 * 1000);
  const pad = (value) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function defaultDateTimeParts(hoursFromNow = 0) {
  const dateTime = defaultDateTime(hoursFromNow);
  const [date = "", time = ""] = dateTime.split("T");
  return { date, time };
}

function toDateTime(date, time) {
  if (!date || !time) {
    return "";
  }
  return `${date}T${time}`;
}

async function createOlympiad() {
  errorMessage.value = validateForm();
  if (errorMessage.value) {
    return;
  }

  submitting.value = true;
  message.value = "";
  createdOlympiad.value = null;
  try {
    const created = await api.admin.createTournament({
      title: form.title,
      description: form.description,
      rules: form.rules,
      startAt: dateTimeValues.value.startAt,
      registrationStartAt: dateTimeValues.value.registrationStartAt,
      registrationEndAt: dateTimeValues.value.registrationEndAt,
      maxTeams: form.maxTeams || null,
      minimumRounds: form.minimumRounds,
      teamMinMembers: form.teamMinMembers,
      teamMaxMembers: form.teamMaxMembers,
      hideTeamsUntilRegistrationEnds: form.hideTeamsUntilRegistrationEnds
    });
    createdOlympiad.value = created;
    message.value = t("admin.tournamentCreated");
    notifier.pushNotification(t("admin.tournamentCreated"), "success");
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
  } finally {
    submitting.value = false;
  }
}

function validateForm() {
  const firstInvalid = findFirstInvalidStep(3);
  if (firstInvalid) {
    currentStep.value = firstInvalid.step;
    return firstInvalid.message;
  }
  return "";
}

function findFirstInvalidStep(limitStep = 3) {
  for (let step = 1; step <= limitStep; step += 1) {
    const message = getStepValidationError(step);
    if (message) {
      return { step, message };
    }
  }
  return null;
}

function getStepValidationError(step) {
  const stepFieldOrder = {
    1: ["title", "description", "rules"],
    2: ["startDate", "startTime", "registrationStartDate", "registrationStartTime", "registrationEndDate", "registrationEndTime"],
    3: ["minimumRounds", "teamMinMembers", "teamMaxMembers", "maxTeams"]
  };
  const fields = stepFieldOrder[step] || [];
  for (const field of fields) {
    if (fieldErrors.value[field]) {
      return fieldErrors.value[field];
    }
  }

  if (step === 1) {
    return "";
  }

  if (step === 2) {
    return "";
  }

  if (step === 3) {
    return "";
  }

  return "";
}

function isPositiveInteger(value) {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 1;
}
</script>
