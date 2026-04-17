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
          <strong>{{ notification.tone === "error" ? "Error" : notification.tone === "success" ? "Done" : "Notice" }}</strong>
          <button class="btn-ghost" type="button" @click="notifier.removeNotification(notification.id)">Close</button>
        </div>
        <p class="text-soft">{{ notification.message }}</p>
      </article>
    </TransitionGroup>
  </div>
</template>

<script setup>
import { notifier } from "../services/notify";
</script>
