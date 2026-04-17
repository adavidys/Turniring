import { reactive } from "vue";

const notifications = reactive([]);

function pushNotification(message, tone = "info") {
  const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
  notifications.push({ id, message, tone });
  window.setTimeout(() => removeNotification(id), 4200);
}

function removeNotification(id) {
  const index = notifications.findIndex((notification) => notification.id === id);
  if (index >= 0) {
    notifications.splice(index, 1);
  }
}

export const notifier = {
  notifications,
  pushNotification,
  removeNotification
};
