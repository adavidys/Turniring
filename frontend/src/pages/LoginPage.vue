<template>
  <section class="split">
    <article class="hero-card stack">
      <span class="eyebrow">Sign in</span>
      <h1 class="title-lg">Return to your tournament desk.</h1>
      <p class="text-soft">
        Use the credentials from your role workspace. Teams can continue submissions, jury can score assignments,
        and admins can manage rounds.
      </p>
    </article>

    <article class="panel stack">
      <h2 class="title-md">Login</h2>
      <form class="stack" @submit.prevent="handleSubmit">
        <div class="field-grid single">
          <div class="field">
            <label for="login-email">Email</label>
            <input id="login-email" v-model="form.email" type="email" required />
          </div>
          <div class="field">
            <label for="login-password">Password</label>
            <input id="login-password" v-model="form.password" type="password" required minlength="8" />
          </div>
        </div>

        <div class="error-box" v-if="errorMessage">{{ errorMessage }}</div>

        <div class="btn-row">
          <button class="btn" type="submit" :disabled="submitting">
            {{ submitting ? "Signing in..." : "Sign in" }}
          </button>
          <RouterLink class="btn-ghost" to="/auth/register">Create account</RouterLink>
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
    notifier.pushNotification(`Welcome back, ${user.name}.`, "success");
    router.push(route.query.redirect || "/profile");
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
  } finally {
    submitting.value = false;
  }
}
</script>
