<template>
  <div class="page-grid">
    <section class="hero-card stack">
      <span class="eyebrow">{{ t("nav.olympiads") }}</span>
      <h1 class="title-lg">{{ t("olympiad.joinTitle") }}</h1>
      <p class="text-soft">{{ t("olympiad.joinCopy") }}</p>
    </section>

    <SectionBlock :title="t('home.section.registrationTitle')" :description="t('home.section.registrationDesc')" :eyebrow="t('home.section.registrationEyebrow')">
      <div v-if="loading" class="panel">{{ t("home.loading") }}</div>
      <div v-else-if="olympiads.length" class="grid-auto">
        <TournamentCard v-for="tournament in olympiads" :key="tournament.id" :tournament="tournament" />
      </div>
      <div v-else class="empty-box">{{ t("olympiad.joinEmpty") }}</div>
    </SectionBlock>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import SectionBlock from "../components/SectionBlock.vue";
import TournamentCard from "../components/TournamentCard.vue";
import { api } from "../services/api";
import { notifier } from "../services/notify";
import { getErrorMessage } from "../services/formatters";
import { t } from "../services/i18n";

const loading = ref(true);
const olympiads = ref([]);

async function loadOlympiads() {
  loading.value = true;
  try {
    const home = await api.public.home();
    olympiads.value = home.registrationOpen || [];
  } catch (error) {
    notifier.pushNotification(getErrorMessage(error), "error");
  } finally {
    loading.value = false;
  }
}

onMounted(loadOlympiads);
</script>
