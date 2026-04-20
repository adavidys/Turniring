<template>
  <section class="split">
    <article class="hero-card stack">
      <span class="eyebrow">{{ t("auth.loginEyebrow") }}</span>
      <h1 class="title-lg">{{ t("auth.loginHeader") }}</h1>
      <p class="text-soft">{{ t("auth.loginCopy") }}</p>
    </article>

    <article class="panel stack">
      <h2 class="title-md">{{ t("auth.loginTitle") }}</h2>
      <form class="stack" @submit.prevent="handleSubmit">
        <div class="field-grid single">
          <div class="field">
            <label for="login-email">{{ t("auth.email") }}</label>
            <input id="login-email" v-model="form.email" type="email" required />
          </div>
          <div class="field">
            <label for="login-password">{{ t("auth.password") }}</label>
            <input id="login-password" v-model="form.password" type="password" required minlength="8" />
          </div>
        </div>

        <div class="error-box" v-if="errorMessage">{{ errorMessage }}</div>

        <div class="btn-row">
          <button class="btn" type="submit" :disabled="submitting">
            {{ submitting ? t("auth.signingIn") : t("auth.signIn") }}
          </button>
          <RouterLink class="btn-ghost" to="/auth/register">{{ t("nav.createAccount") }}</RouterLink>
        </div>
      </form>
    </article>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { authStore } from "../services/auth";
import { notifier } from "../services/notify";
import { getErrorMessage } from "../services/formatters";
import { t } from "../services/i18n";

const router = useRouter();
const route = useRoute();

const form = reactive({
  email: "",
  password: ""
});

const submitting = ref(false);
const errorMessage = ref("");

async function handleSubmit() {
  submitting.value = true;
  errorMessage.value = "";
  try {
    const user = await authStore.login(form);
    notifier.pushNotification(t("notifications.welcome", { name: user.name }), "success");
    router.push(route.query.redirect || authStore.resolveWorkspaceRoute(user.role));
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
  } finally {
    submitting.value = false;
  }
}
</script>
