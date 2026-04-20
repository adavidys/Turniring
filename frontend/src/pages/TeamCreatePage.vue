<template>
  <div class="page-grid">
    <section class="hero-card stack">
      <span class="eyebrow">{{ t("nav.teams") }}</span>
      <div class="toolbar">
        <div class="stack-sm">
          <h1 class="title-lg">{{ t("team.createTitle") }}</h1>
          <p class="text-soft">{{ t("team.createCopy") }}</p>
        </div>
        <RouterLink class="btn-tonal" to="/team">{{ t("team.workspace") }}</RouterLink>
      </div>
    </section>

    <SectionBlock
      :title="tx('Дані команди', 'Team details')"
      :description="tx('Заповніть інформацію про команду. Учасників можна додати пізніше у просторі команди.', 'Fill in team information. You can add members later in the team workspace.')"
      :eyebrow="tx('Крок 1', 'Step 1')"
    >
      <form class="stack" @submit.prevent="createTeam">
        <div class="field-grid">
          <div class="field">
            <label>{{ t("tournament.teamName") }}</label>
            <input v-model="teamForm.name" type="text" required minlength="2" />
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

        <div class="success-box" v-if="message">{{ message }}</div>
        <div class="error-box" v-if="errorMessage">{{ errorMessage }}</div>

        <div class="btn-row">
          <button class="btn" type="submit" :disabled="submitting">
            {{ submitting ? tx("Створення…", "Creating…") : t("team.createTitle") }}
          </button>
          <RouterLink class="btn-ghost" to="/team">{{ t("home.openWorkspace") }}</RouterLink>
        </div>
      </form>
    </SectionBlock>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import SectionBlock from "../components/SectionBlock.vue";
import { api } from "../services/api";
import { notifier } from "../services/notify";
import { getErrorMessage } from "../services/formatters";
import { t, tx } from "../services/i18n";

const submitting = ref(false);
const message = ref("");
const errorMessage = ref("");

const teamForm = reactive({
  name: "",
  city: "",
  organization: "",
  contactHandle: ""
});

async function createTeam() {
  submitting.value = true;
  message.value = "";
  errorMessage.value = "";
  try {
    await api.team.createTeam(teamForm);
    message.value = tx("Команду створено.", "Team created.");
    notifier.pushNotification(tx("Команду створено.", "Team created."), "success");
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
  } finally {
    submitting.value = false;
  }
}
</script>
