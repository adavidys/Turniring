<template>
  <section class="split">
    <article class="hero-card stack">
      <span class="eyebrow">{{ t("auth.registerEyebrow") }}</span>
      <h1 class="title-lg">{{ t("auth.registerHeader") }}</h1>
      <p class="text-soft">{{ t("auth.registerCopy") }}</p>
    </article>

    <article class="panel stack">
      <h2 class="title-md">{{ t("auth.registerTitle") }}</h2>
      <form class="stack" @submit.prevent="handleSubmit">
        <div class="field-grid">
          <div class="field">
            <label for="register-name">{{ t("auth.firstName") }}</label>
            <input id="register-name" v-model="form.name" type="text" minlength="2" required />
          </div>
          <div class="field">
            <label for="register-last-name">{{ t("auth.lastName") }}</label>
            <input id="register-last-name" v-model="form.lastName" type="text" minlength="2" required />
          </div>
          <div class="field">
            <label for="register-email">{{ t("auth.email") }}</label>
            <input id="register-email" v-model="form.email" type="email" required />
          </div>
          <div class="field">
            <label for="register-password">{{ t("auth.password") }}</label>
            <input id="register-password" v-model="form.password" type="password" minlength="8" required />
          </div>
        </div>

        <div class="field stack-sm">
          <label>{{ t("auth.role") }}</label>
          <p class="text-soft">{{ t("auth.roleOpenAccess") }}</p>
          <div class="grid-auto">
            <button
              v-for="option in roleOptions"
              :key="option.value"
              class="btn-ghost register-role-option"
              :class="{ 'is-active': form.role === option.value }"
              type="button"
              @click="form.role = option.value"
              :aria-pressed="form.role === option.value"
            >
              <strong>{{ option.label }}</strong>
              <span class="text-soft">{{ option.description }}</span>
            </button>
          </div>
        </div>

        <div class="error-box" v-if="errorMessage">{{ errorMessage }}</div>

        <div class="btn-row">
          <button class="btn" type="submit" :disabled="submitting">
            {{ submitting ? t("auth.creatingAccount") : t("auth.createAccount") }}
          </button>
          <RouterLink class="btn-ghost" to="/auth/login">{{ t("auth.alreadyHaveAccount") }}</RouterLink>
        </div>
      </form>
    </article>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { authStore } from "../services/auth";
import { notifier } from "../services/notify";
import { getErrorMessage } from "../services/formatters";
import { t } from "../services/i18n";

const router = useRouter();
const route = useRoute();
const submitting = ref(false);
const errorMessage = ref("");

const form = reactive({
  name: "",
  lastName: "",
  email: "",
  password: "",
  role: "TEAM"
});

const roleOptions = computed(() => [
  {
    value: "TEAM",
    label: t("auth.roleTeam"),
    description: t("auth.roleTeamDesc")
  },
  {
    value: "ADMIN",
    label: t("auth.roleAdmin"),
    description: t("auth.roleAdminDesc")
  }
]);

async function handleSubmit() {
  submitting.value = true;
  errorMessage.value = "";
  try {
    const user = await authStore.register(form);
    notifier.pushNotification(t("notifications.registered"), "success");
    router.push(route.query.redirect || authStore.resolveWorkspaceRoute(user.role));
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
  } finally {
    submitting.value = false;
  }
}
</script>
