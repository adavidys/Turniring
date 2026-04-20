<template>
  <div class="page-grid">
    <section class="hero-card stack">
      <span class="eyebrow">{{ t("profileData.eyebrow") }}</span>
      <div class="toolbar">
        <div class="stack-sm">
          <h1 class="title-lg">{{ t("profileData.title") }}</h1>
          <p class="text-soft">{{ t("profileData.copy") }}</p>
        </div>
        <RouterLink class="btn-tonal" to="/profile">{{ t("profileData.openProfile") }}</RouterLink>
      </div>
    </section>

    <div v-if="loading" class="panel">{{ t("profileData.loading") }}</div>
    <div v-else-if="errorMessage" class="error-box">{{ errorMessage }}</div>
    <SectionBlock
      v-else
      :title="t('profileData.formTitle')"
      :description="t('profileData.formDesc')"
      :eyebrow="t('profileData.formEyebrow')"
    >
      <form class="stack" @submit.prevent="saveProfileData">
        <div class="field-grid">
          <div class="field">
            <label>{{ t("auth.firstName") }}</label>
            <input v-model="form.name" type="text" required minlength="2" />
          </div>
          <div class="field">
            <label>{{ t("auth.lastName") }}</label>
            <input v-model="form.lastName" type="text" required minlength="2" />
          </div>
          <div class="field">
            <label>{{ t("auth.email") }}</label>
            <input v-model="form.email" type="email" required />
          </div>
          <div class="field">
            <label>{{ t("profileData.newPassword") }}</label>
            <input v-model="form.newPassword" type="password" minlength="8" />
          </div>
        </div>

        <div class="success-box" v-if="message">{{ message }}</div>
        <div class="error-box" v-if="saveError">{{ saveError }}</div>

        <div class="btn-row">
          <button class="btn" type="submit" :disabled="saving">
            {{ saving ? t("profileData.saving") : t("profileData.save") }}
          </button>
        </div>
      </form>
    </SectionBlock>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import SectionBlock from "../components/SectionBlock.vue";
import { authStore } from "../services/auth";
import { notifier } from "../services/notify";
import { getErrorMessage } from "../services/formatters";
import { t } from "../services/i18n";

const loading = ref(true);
const saving = ref(false);
const errorMessage = ref("");
const saveError = ref("");
const message = ref("");

const form = reactive({
  name: "",
  lastName: "",
  email: "",
  newPassword: ""
});

async function loadData() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const profile = await authStore.refreshProfile();
    form.name = profile.name || "";
    form.lastName = profile.lastName || "";
    form.email = profile.email || "";
    form.newPassword = "";
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
  } finally {
    loading.value = false;
  }
}

async function saveProfileData() {
  saving.value = true;
  saveError.value = "";
  message.value = "";
  try {
    await authStore.updateProfileData({
      name: form.name,
      lastName: form.lastName,
      email: form.email,
      newPassword: form.newPassword.trim() ? form.newPassword : null
    });
    form.newPassword = "";
    message.value = t("profileData.saved");
    notifier.pushNotification(t("profileData.saved"), "success");
  } catch (error) {
    saveError.value = getErrorMessage(error);
  } finally {
    saving.value = false;
  }
}

onMounted(loadData);
</script>
