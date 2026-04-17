<template>
  <section class="hero-card stack">
    <span class="eyebrow">404</span>
    <h1 class="title-lg">This route is outside the bracket.</h1>
    <p class="text-soft">
      The page you requested does not exist in the current tournament map.
    </p>
    <div class="btn-row">
      <RouterLink class="btn" to="/">Back home</RouterLink>
    </div>
  </section>
</template>

<script setup>
import { RouterLink } from "vue-router";
</script>
