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

  // before the embedded tool posts its resize message (handleIntegrationsResize in
  // StudentQuiz.vue), we don't know its real content height yet. This used to fall
  // back to min-height: 100vh, but that measures against THIS document's own
  // viewport - stacked on top of whatever else is already on the page (retake
  // banner, submission details, etc.), it guaranteed this document's total height
  // exceeded one viewport, which is exactly what produced the reported double
  // scrollbar: this document needing to scroll AND the LMS's outer iframe (not yet
  // told to resize, since that only happens once the message arrives) also needing
  // to scroll to show all of it. A fixed, modest default has no such guarantee -
  // worst case the iframe's own content needs to scroll internally for the brief
  // window before the real height arrives, which is far less disruptive than the
  // whole page reliably overflowing on every load.
  & .no-resize {
    min-height: 600px;
  }
}
</style>
