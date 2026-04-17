<template>
  <section class="split">
    <article class="hero-card stack">
      <span class="eyebrow">Create account</span>
      <h1 class="title-lg">Open a team-ready identity in one step.</h1>
      <p class="text-soft">
        Registration creates a `TEAM` account by default. After that you can join tournaments, register a roster,
        and manage submissions from the team workspace.
      </p>
    </article>

    <article class="panel stack">
      <h2 class="title-md">Register</h2>
      <form class="stack" @submit.prevent="handleSubmit">
        <div class="field-grid">
          <div class="field">
            <label for="register-name">First name</label>
            <input id="register-name" v-model="form.name" type="text" minlength="2" required />
          </div>
          <div class="field">
            <label for="register-last-name">Last name</label>
            <input id="register-last-name" v-model="form.lastName" type="text" minlength="2" required />
          </div>
          <div class="field">
            <label for="register-email">Email</label>
            <input id="register-email" v-model="form.email" type="email" required />
          </div>
          <div class="field">
            <label for="register-password">Password</label>
            <input id="register-password" v-model="form.password" type="password" minlength="8" required />
          </div>
        </div>

        <div class="error-box" v-if="errorMessage">{{ errorMessage }}</div>

        <div class="btn-row">
          <button class="btn" type="submit" :disabled="submitting">
            {{ submitting ? "Creating account..." : "Create account" }}
          </button>
          <RouterLink class="btn-ghost" to="/auth/login">Already have an account?</RouterLink>
        </div>
      </form>
    </article>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { authStore } from "../services/auth";
import { notifier } from "../services/notify";
import { getErrorMessage } from "../services/formatters";

const router = useRouter();
const submitting = ref(false);
const errorMessage = ref("");

const form = reactive({
  name: "",
  lastName: "",
  email: "",
  password: ""
});

async function handleSubmit() {
  submitting.value = true;
  errorMessage.value = "";
  try {
    await authStore.register(form);
    notifier.pushNotification("Account created. You can start joining tournaments.", "success");
    router.push("/team");
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
  } finally {
    submitting.value = false;
  }
}
</script>
