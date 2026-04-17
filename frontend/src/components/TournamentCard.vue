<template>
  <RouterLink class="card-link" :to="`/tournaments/${tournament.id}`">
    <article class="panel stack">
      <div class="toolbar">
        <div class="stack-sm">
          <h3 class="title-md">{{ tournament.title }}</h3>
          <p class="text-soft">{{ tournament.description }}</p>
        </div>
        <StatusBadge :status="tournament.status" />
      </div>

      <div class="stat-row">
        <span class="stat-chip">Registration: {{ tournament.registrationOpen ? "Open" : "Closed" }}</span>
        <span class="stat-chip">Teams: {{ tournament.registeredTeams }}</span>
        <span class="stat-chip">Rounds: {{ tournament.minimumRounds }}</span>
      </div>

      <div class="meta-list">
        <div class="meta-row">
          <span class="text-soft">Registration window</span>
          <strong>{{ formatDateTime(tournament.registrationStartAt) }} → {{ formatDateTime(tournament.registrationEndAt) }}</strong>
        </div>
        <div class="meta-row">
          <span class="text-soft">Tournament start</span>
          <strong>{{ formatDateTime(tournament.startAt) }}</strong>
        </div>
      </div>
    </article>
  </RouterLink>
</template>

<script setup>
import { RouterLink } from "vue-router";
import StatusBadge from "./StatusBadge.vue";
import { formatDateTime } from "../services/formatters";

defineProps({
  tournament: {
    type: Object,
    required: true
  }
});
</script>
