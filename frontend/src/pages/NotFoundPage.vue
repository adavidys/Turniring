<template>
  <section class="hero-card stack">
    <span class="eyebrow">404</span>
    <h1 class="title-lg">{{ t("notFound.title") }}</h1>
    <p class="text-soft">{{ t("notFound.copy") }}</p>
    <div class="btn-row">
      <RouterLink class="btn" to="/">{{ t("notFound.back") }}</RouterLink>
    </div>
  </section>
</template>

<script setup>
import { RouterLink } from "vue-router";
import { t } from "../services/i18n";
</script>