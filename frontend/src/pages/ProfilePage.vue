<template>
  <div class="page-grid">
    <section class="hero-card stack" v-if="profile">
      <span class="eyebrow">Profile</span>
      <div class="toolbar">
        <div class="stack-sm">
          <h1 class="title-lg">{{ profile.name }} {{ profile.lastName }}</h1>
          <p class="text-soft">{{ profile.email }}</p>
        </div>
        <StatusBadge :status="profile.role" />
      </div>
    </section>

    <div v-if="loading" class="panel">Loading profile…</div>
    <div v-else-if="errorMessage" class="error-box">{{ errorMessage }}</div>
    <template v-else-if="profile">
      <SectionBlock title="Account summary" description="Current identity and workspace access." eyebrow="Access">
        <div class="stat-row">
          <span class="stat-chip">Role: {{ profile.role }}</span>
          <span class="stat-chip">Teams: {{ profile.teams.length }}</span>
          <span class="stat-chip">Managed tournaments: {{ profile.managedTournaments.length }}</span>
          <span class="stat-chip">Jury assignments: {{ profile.juryAssignments.length }}</span>
        </div>
      </SectionBlock>

      <section class="split" v-if="profile.teams.length || profile.managedTournaments.length">
        <SectionBlock title="Team history" description="Rosters you captain across tournaments." eyebrow="Teams">
          <div v-if="profile.teams.length" class="stack">
            <article v-for="team in profile.teams" :key="team.id" class="panel stack-sm">
              <div class="toolbar">
                <div>
                  <h3 class="title-sm">{{ team.name }}</h3>
                  <p class="text-soft">Tournament #{{ team.tournamentId }}</p>
                </div>
                <RouterLink class="btn-tonal" to="/team">Open workspace</RouterLink>
              </div>
              <div class="member-list">
                <div v-for="member in team.members" :key="member.id || member.email" class="member-row">
                  <span>{{ member.fullName }}</span>
                  <span class="text-soft">{{ member.email }}</span>
                </div>
              </div>
            </article>
          </div>
          <div v-else class="empty-box">No team history yet.</div>
        </SectionBlock>

        <SectionBlock title="Managed tournaments" description="Events created or controlled from your role." eyebrow="Admin">
          <div v-if="profile.managedTournaments.length" class="grid-auto">
            <TournamentCard v-for="tournament in profile.managedTournaments" :key="tournament.id" :tournament="tournament" />
          </div>
          <div v-else class="empty-box">No managed tournaments attached to this account.</div>
        </SectionBlock>
      </section>

      <SectionBlock
        title="Jury activity"
        description="Assigned submissions and evaluation status."
        eyebrow="Review"
        v-if="profile.role === 'JURY'"
      >
        <div v-if="profile.juryAssignments.length" class="stack">
          <article v-for="assignment in profile.juryAssignments" :key="assignment.assignmentId" class="panel stack-sm">
            <div class="toolbar">
              <div>
                <h3 class="title-sm">{{ assignment.submission.teamName }}</h3>
                <p class="text-soft">{{ assignment.submission.taskTitle }}</p>
              </div>
              <StatusBadge :status="assignment.status" />
            </div>
            <p class="text-soft">{{ assignment.submission.githubUrl }}</p>
          </article>
        </div>
        <div v-else class="empty-box">No jury assignments yet.</div>
      </SectionBlock>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import SectionBlock from "../components/SectionBlock.vue";
import StatusBadge from "../components/StatusBadge.vue";
import TournamentCard from "../components/TournamentCard.vue";
import { authStore } from "../services/auth";
import { notifier } from "../services/notify";
import { getErrorMessage } from "../services/formatters";

const loading = ref(true);
const errorMessage = ref("");
const profile = ref(null);

async function loadProfile() {
  loading.value = true;
  errorMessage.value = "";
  try {
    profile.value = await authStore.refreshProfile();
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
    notifier.pushNotification(errorMessage.value, "error");
  } finally {
    loading.value = false;
  }
}

onMounted(loadProfile);
</script>
