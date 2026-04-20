<template>
  <div class="page-grid">
    <section class="hero-card stack">
      <span class="eyebrow">{{ tx("Користувачі", "Users") }}</span>
      <div class="toolbar">
        <div class="stack-sm">
          <h1 class="title-lg">{{ t("admin.createUser") }}</h1>
          <p class="text-soft">
            {{ tx("Створіть обліковий запис у правильній ролі, далі користувач зможе увійти та працювати у своїй зоні.", "Create an account with the target role so the user can sign in and work in the proper workspace.") }}
          </p>
        </div>
        <RouterLink class="btn-tonal" to="/admin">{{ t("admin.dashboard") }}</RouterLink>
      </div>
    </section>

    <SectionBlock
      :title="t('admin.createUser')"
      :description="tx('Послідовність: роль → персональні дані → пароль.', 'Sequence: role → personal data → password.')"
      :eyebrow="tx('Крок', 'Step')"
    >
      <form class="stack" @submit.prevent="createUser">
        <div class="field-grid">
          <div class="field">
            <label>{{ t("auth.role") }}</label>
            <select v-model="userForm.role">
              <option>TEAM</option>
              <option>ORGANIZER</option>
              <option>ADMIN</option>
            </select>
          </div>
          <div class="field">
            <label>{{ t("auth.firstName") }}</label>
            <input v-model.trim="userForm.name" type="text" required />
          </div>
          <div class="field">
            <label>{{ t("auth.lastName") }}</label>
            <input v-model.trim="userForm.lastName" type="text" required />
          </div>
          <div class="field">
            <label>{{ t("auth.email") }}</label>
            <input v-model.trim="userForm.email" type="email" required />
          </div>
          <div class="field">
            <label>{{ t("auth.password") }}</label>
            <input v-model="userForm.password" type="password" minlength="8" required />
          </div>
        </div>

        <p class="text-soft">{{ t("admin.juryInviteOnly") }}</p>
        <div class="success-box" v-if="message">{{ message }}</div>
        <div class="error-box" v-if="errorMessage">{{ errorMessage }}</div>

        <div class="btn-row">
          <button class="btn" type="submit" :disabled="submitting">
            {{ submitting ? tx("Створення…", "Creating…") : t("admin.createUser") }}
          </button>
          <RouterLink class="btn-ghost" to="/jury/add">{{ t("nav.addJury") }}</RouterLink>
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
const userForm = reactive({
  name: "",
  lastName: "",
  email: "",
  password: "",
  role: "TEAM"
});

async function createUser() {
  submitting.value = true;
  message.value = "";
  errorMessage.value = "";
  try {
    await api.admin.createUser(userForm);
    message.value = t("admin.userCreated");
    notifier.pushNotification(t("admin.userCreated"), "success");
    userForm.password = "";
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
  } finally {
    submitting.value = false;
  }
}
</script>
