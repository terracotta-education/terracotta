<template>
  <div>
    <h2>Submission Details</h2>
    <v-divider />
    <v-list density="compact">
      <v-list-item>
        <template #prepend><strong>Time</strong></template>
        <template #append><span>{{ timeBeforeSubmission }}</span></template>
      </v-list-item>
      <v-list-item>
        <template #prepend><strong>Allowed Attempts</strong></template>
        <template #append><span>{{ allowedAttempts }}</span></template>
      </v-list-item>
      <v-list-item>
        <template #prepend><strong>Submitted</strong></template>
        <template #append><span>{{ dateSubmitted }}</span></template>
      </v-list-item>
      <v-list-item>
        <template #prepend><strong>Current Score</strong></template>
        <template #append><span>{{ currentScore }}</span></template>
      </v-list-item>
      <v-list-item>
        <template #prepend><strong>Kept Score</strong></template>
        <template #append><span>{{ keptScore }}</span></template>
      </v-list-item>
    </v-list>
    <v-divider />
  </div>
</template>

<script setup>
defineProps({
  timeBeforeSubmission: { type: String, default: "" },
  allowedAttempts: { type: [String, Number], default: " - " },
  dateSubmitted: { type: String, default: "" },
  currentScore: { type: String, default: "" },
  keptScore: { type: String, default: "" }
});
</script>
