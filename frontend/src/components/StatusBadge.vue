<template>
  <span class="status-badge" :data-tone="tone">
    {{ label }}
  </span>
</template>

<script setup>
import { computed } from "vue";
import { formatStatus } from "../services/formatters";

const props = defineProps({
  status: {
    type: String,
    default: ""
  }
});

const label = computed(() => formatStatus(props.status));

const tone = computed(() => {
  const status = props.status || "";
  if (["RUNNING", "ACTIVE", "EVALUATED", "FINISHED"].includes(status)) {
    return "success";
  }
  if (["REGISTRATION", "SUBMISSION_CLOSED"].includes(status)) {
    return "warning";
  }
  return "neutral";
});
</script>
