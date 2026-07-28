<template>
  <v-row>
    <v-col>
      <v-card
        v-if="muted"
        class="pt-5 px-5 mx-auto bg-yellow-lighten-5 rounded-lg"
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
