<template>
  <v-row>
    <v-col>
      <v-card
        v-if="muted"
        class="pt-5 px-5 mx-auto rounded-lg card-warning"
        variant="outlined"
      >
        <h3>Your assignment is muted</h3>
        <p class="pb-0">Your instructor has not released the grades yet.</p>
      </v-card>

      <div v-if="!muted && assignmentData?.submissions">
        <SubmissionSelector
          :submissions="assignmentData.submissions"
          @select="$emit('select-submission', $event)"
        />
      </div>
    </v-col>
  </v-row>
</template>

<script setup>
import SubmissionSelector from "@/views/assignment/SubmissionSelector.vue";

defineProps({
  muted: { type: Boolean, default: true },
  assignmentData: { type: Object, default: null }
});

defineEmits(["select-submission"]);
</script>

<style lang="scss" scoped>
// bg-yellow-lighten-5 (a Vuetify color utility class) generates no CSS in this
// project's build - see ExperimentType.vue's .card-warning for the full explanation.
// Vuetify's own yellow-lighten-5 (#fffde7) doesn't have a project $yellow map
// equivalent (the project's $yellow is a different, darker/amber shade), so use the
// literal palette value directly.
.card-warning {
  background-color: #fffde7;
}
</style>
