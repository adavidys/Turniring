<template>
  <div class="page-grid">
    <section class="hero-card stack">
      <span class="eyebrow">{{ tx("Запрошення", "Invite") }}</span>
      <h1 class="title-lg">{{ tx("Запрошення за UUID-посиланням", "Invite via UUID link") }}</h1>
      <p class="text-soft">{{ tx("Перегляньте деталі і підтвердьте приєднання до команди або журі.", "Review details and confirm joining a team or jury.") }}</p>
    </section>

    <div v-if="loading" class="panel">{{ tx("Завантаження запрошення…", "Loading invite…") }}</div>
    <div v-else-if="errorMessage" class="error-box">{{ errorMessage }}</div>
    <section v-else-if="invite" class="panel stack">
      <h2 class="title-md">{{ tx("Тип", "Type") }}: {{ invite.type === "JURY" ? "JURY" : "TEAM" }}</h2>
      <p class="text-soft" v-if="invite.teamName">{{ tx("Команда", "Team") }}: {{ invite.teamName }} (ID: {{ invite.teamId }})</p>
      <p class="text-soft">{{ tx("Дійсне до", "Valid until") }}: {{ formatDateTime(invite.expiresAt) }}</p>
      <div v-if="invite.used" class="error-box">{{ tx("Це запрошення вже використано.", "This invite has already been used.") }}</div>
      <div v-else-if="invite.expired" class="error-box">{{ tx("Термін дії запрошення минув.", "Invite has expired.") }}</div>
      <div v-else-if="!authStore.isLoggedIn.value" class="stack-sm">
        <p class="text-soft">{{ tx("Щоб прийняти запрошення, увійдіть або створіть акаунт.", "To accept the invite, sign in or create an account.") }}</p>
        <div class="btn-row">
          <RouterLink class="btn" :to="loginLink">{{ t("auth.signIn") }}</RouterLink>
          <RouterLink class="btn-ghost" :to="registerLink">{{ t("auth.createAccount") }}</RouterLink>
        </div>
      </div>
      <div v-else class="btn-row">
        <button class="btn" type="button" :disabled="accepting" @click="acceptInvite">
          {{ accepting ? tx("Підтвердження…", "Accepting…") : tx("Прийняти запрошення", "Accept invite") }}
        </button>
      </div>

      <div v-if="accepted" class="success-box">
        {{ tx("Запрошення прийнято. Поточна роль", "Invite accepted. Current role") }}: {{ accepted.role }}.
      </div>
      <div class="btn-row" v-if="accepted">
        <RouterLink class="btn-tonal" :to="accepted.type === 'JURY' ? '/jury' : '/team'">
          {{ tx("Перейти до робочої сторінки", "Open workspace") }}
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { api } from "../services/api";
import { authStore } from "../services/auth";
import { notifier } from "../services/notify";
import { getErrorMessage, formatDateTime } from "../services/formatters";
import { t, tx } from "../services/i18n";

const route = useRoute();

const loading = ref(true);
const accepting = ref(false);
const errorMessage = ref("");
const invite = ref(null);
const accepted = ref(null);

const token = computed(() => route.params.token);
const redirectPath = computed(() => `/invite/${token.value}`);
const loginLink = computed(() => `/auth/login?redirect=${encodeURIComponent(redirectPath.value)}`);
const registerLink = computed(() => `/auth/register?redirect=${encodeURIComponent(redirectPath.value)}`);

function resolveInviteError(error) {
  const message = getErrorMessage(error);
  return message === "This link is not active" ? tx("Це посилання неактивне.", "This link is not active.") : message;
}

async function loadInvite() {
  loading.value = true;
  errorMessage.value = "";
  accepted.value = null;
  try {
    invite.value = await api.public.invite(token.value);
  } catch (error) {
    invite.value = null;
    errorMessage.value = resolveInviteError(error);
  } finally {
    loading.value = false;
  }
}

async function acceptInvite() {
  accepting.value = true;
  try {
    accepted.value = await api.profile.acceptInvite(token.value);
    errorMessage.value = "";
    await authStore.refreshProfile();
    notifier.pushNotification(tx("Запрошення успішно прийнято.", "Invite accepted successfully."), "success");
  } catch (error) {
    const message = resolveInviteError(error);
    errorMessage.value = message;
    notifier.pushNotification(message, "error");
  } finally {
    accepting.value = false;
  }
}

watch(token, loadInvite, { immediate: true });
</script>
