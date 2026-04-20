<template>
  <div class="page-grid">
    <section class="hero-card stack">
      <span class="eyebrow">{{ t("nav.jury") }}</span>
      <div class="toolbar">
        <div class="stack-sm">
          <h1 class="title-lg">{{ t("jury.addTitle") }}</h1>
          <p class="text-soft">{{ t("jury.addCopy") }}</p>
        </div>
        <RouterLink class="btn-tonal" to="/admin">{{ t("admin.dashboard") }}</RouterLink>
      </div>
    </section>

    <SectionBlock
      :title="t('jury.inviteSectionTitle')"
      :description="t('jury.inviteSectionDesc')"
      :eyebrow="t('jury.inviteEyebrow')"
    >
      <div class="stack">
        <div class="btn-row">
          <button class="btn" type="button" :disabled="submitting" @click="createJuryInvite">
            {{ submitting ? t("jury.inviteGenerating") : t("jury.inviteGenerate") }}
          </button>
        </div>

        <div v-if="inviteLink" class="field">
          <label>{{ t("jury.inviteLink") }}</label>
          <input :value="inviteLink" readonly />
          <div class="btn-row">
            <button class="btn-ghost" type="button" @click="copyInviteLink(inviteLink)">{{ t("jury.copyLink") }}</button>
          </div>
        </div>

        <div class="success-box" v-if="message">{{ message }}</div>
        <div class="error-box" v-if="errorMessage">{{ errorMessage }}</div>
      </div>
    </SectionBlock>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { RouterLink } from "vue-router";
import SectionBlock from "../components/SectionBlock.vue";
import { api } from "../services/api";
import { notifier } from "../services/notify";
import { getErrorMessage } from "../services/formatters";
import { t } from "../services/i18n";

const submitting = ref(false);
const message = ref("");
const errorMessage = ref("");
const inviteLink = ref("");

async function createJuryInvite() {
  submitting.value = true;
  message.value = "";
  errorMessage.value = "";
  try {
    const response = await api.admin.createJuryInvite();
    inviteLink.value = toAbsoluteInviteLink(response.invitePath);
    message.value = t("jury.addSuccess");
    notifier.pushNotification(t("jury.addSuccess"), "success");
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
  } finally {
    submitting.value = false;
  }
}

async function copyInviteLink(link) {
  try {
    await navigator.clipboard.writeText(link);
    notifier.pushNotification(t("jury.copySuccess"), "success");
  } catch {
    notifier.pushNotification(t("jury.copyError"), "error");
  }
}

function toAbsoluteInviteLink(invitePath) {
  return new URL(invitePath, window.location.origin).toString();
}
</script>
