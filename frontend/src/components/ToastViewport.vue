<template>
  <div class="toast-stack">
    <TransitionGroup name="fade">
      <article
        v-for="notification in notifier.notifications"
        :key="notification.id"
        class="toast"
        :data-tone="notification.tone"
      >
        <div class="toolbar">
          <strong>{{ notification.tone === "error" ? t("toast.error") : notification.tone === "success" ? t("toast.success") : t("toast.notice") }}</strong>
          <button class="btn-ghost" type="button" @click="closeNotification(notification.id)">{{ t("toast.close") }}</button>
        </div>
        <p class="text-soft">{{ notification.message }}</p>
      </article>
    </TransitionGroup>
  </div>
</template>

<script setup>
import { notifier } from "../services/notify";
import { t } from "../services/i18n";

function closeNotification(id) {
  notifier.removeNotification(id);
}
</script>