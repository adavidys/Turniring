<template>
  <article class="panel stack card-link">
    <div class="toolbar">
      <div class="stack-sm">
        <RouterLink :to="`/tournaments/${tournament.id}`">
          <h3 class="title-md">{{ tournament.title }}</h3>
        </RouterLink>
        <p class="text-soft">{{ tournament.description }}</p>
      </div>
      <StatusBadge :status="tournament.status" />
    </div>

    <div class="stat-row">
      <span class="stat-chip">{{ t("tournament.registrationLabel") }}: {{ tournament.registrationOpen ? t("tournament.registrationOpen") : t("tournament.registrationClosed") }}</span>
      <span class="stat-chip">{{ t("tournament.teams") }}: {{ tournament.registeredTeams }}</span>
      <span class="stat-chip">{{ t("tournament.rounds") }}: {{ tournament.minimumRounds }}</span>
      <span class="stat-chip">{{ tx("Лайки", "Likes") }}: {{ tournament.likeCount || 0 }}</span>
    </div>

    <div class="meta-list">
      <div class="meta-row">
        <span class="text-soft">{{ t("tournament.registrationWindow") }}</span>
        <strong>{{ formatDateTime(tournament.registrationStartAt) }} → {{ formatDateTime(tournament.registrationEndAt) }}</strong>
      </div>
      <div class="meta-row">
        <span class="text-soft">{{ t("tournament.start") }}</span>
        <strong>{{ formatDateTime(tournament.startAt) }}</strong>
      </div>
    </div>

    <div class="btn-row" v-if="showActions">
      <RouterLink class="btn-tonal" :to="`/tournaments/${tournament.id}`">{{ tx("Відкрити", "Open") }}</RouterLink>
      <button class="btn-ghost" type="button" :disabled="submittingLike" @click="toggleLike">
        {{ tournament.likedByCurrentUser ? tx("Прибрати лайк", "Unlike") : tx("Лайк", "Like") }}
      </button>
    </div>
  </article>
</template>

<script setup>
import { ref } from "vue";
import { RouterLink } from "vue-router";
import StatusBadge from "./StatusBadge.vue";
import { api } from "../services/api";
import { authStore } from "../services/auth";
import { formatDateTime } from "../services/formatters";
import { notifier } from "../services/notify";
import { t, tx } from "../services/i18n";

const props = defineProps({
  tournament: {
    type: Object,
    required: true
  },
  showActions: {
    type: Boolean,
    default: true
  }
});
const emit = defineEmits(["like-change"]);
const submittingLike = ref(false);

async function toggleLike() {
  if (!authStore.isLoggedIn.value) {
    notifier.pushNotification(tx("Увійдіть, щоб лайкати олімпіади.", "Sign in to like olympiads."), "error");
    return;
  }

  submittingLike.value = true;
  try {
    const updated = props.tournament.likedByCurrentUser
      ? await api.public.unlikeTournament(props.tournament.id)
      : await api.public.likeTournament(props.tournament.id);
    emit("like-change", updated);
  } catch (error) {
    notifier.pushNotification(error.message || tx("Не вдалося оновити лайк.", "Failed to update like."), "error");
  } finally {
    submittingLike.value = false;
  }
}
</script>
