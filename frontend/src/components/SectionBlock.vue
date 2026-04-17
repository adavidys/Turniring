<template>
  <section class="panel stack">
    <div class="toolbar">
      <div class="stack-sm">
        <span v-if="eyebrow" class="eyebrow">{{ eyebrow }}</span>
        <h2 class="title-md">{{ title }}</h2>
      </div>
      <slot name="actions" />
    </div>
    <p v-if="description" class="text-soft">{{ description }}</p>
    <slot />
  </section>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ""
  },
  eyebrow: {
    type: String,
    default: ""
  }
});
</script>
