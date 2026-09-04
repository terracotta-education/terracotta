<template>
  <v-row class="integration mt-0">
    <v-col
      v-if="!submitted"
      class="py-0"
    >
      <div
        v-if="assessment.html"
        v-html="assessment.html"
      />
      <iframe
        v-if="!readonly"
        id="integration-iframe"
        :src="integration.launchUrl"
        :class="{ 'no-resize': !hasResizeMessage }"
        title="student assignment"
        aria-label="student assignment"
      />
      <ExternalIntegrationResponseEditor
        v-if="readonly"
        :submission="selectedSubmission"
      />
    </v-col>

    <v-col v-if="submitted">
      <v-alert
        type="success"
        variant="outlined"
      >
        Your answers have been submitted.
      </v-alert>
    </v-col>
  </v-row>
</template>

<script setup>
import ExternalIntegrationResponseEditor from "@/views/integrations/ExternalIntegrationResponseEditor.vue";

defineProps({
  assessment: { type: Object, required: true },
  integration: { type: Object, required: true },
  readonly: { type: Boolean, default: false },
  submitted: { type: Boolean, default: false },
  selectedSubmission: { type: Object, default: null },
  hasResizeMessage: { type: Boolean, default: false }
});
</script>

<style lang="scss" scoped>
.integration {
  min-height: 100%;
  min-width: 100%;

  & > .v-col {
    min-height: 100%;
    min-width: 100%;

    & > iframe {
      min-width: 100%;
      border: none;
    }
  }

  & .no-resize {
    min-height: 100vh;
  }
}
</style>
