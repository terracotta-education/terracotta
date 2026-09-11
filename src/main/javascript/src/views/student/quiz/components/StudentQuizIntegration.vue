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
  min-width: 100%;

  // no min-height here (or on .v-col below) - Vuetify's v-application renders with
  // its "full-height" layout mode, which this row/col's own min-height: 100% used to
  // resolve against regardless of how much space any PRECEDING sibling content
  // (retake banner, submission details, etc. in StudentQuiz.vue) already used above
  // it, inflating this area by a full extra viewport's worth of space on top of that.
  // Without it, this area sizes to its own content instead of fighting it.
  & > .v-col {
    min-width: 100%;

    & > iframe {
      // an iframe defaults to display: inline, which sits it on a text baseline and
      // reserves a few extra px below it for descenders (the same "mystery gap
      // under an image" quirk that comes up whenever an inline-replaced element
      // sits in a block context) - block avoids that.
      display: block;
      min-width: 100%;
      border: none;
    }
  }

  // A fixed fallback, not one measured off window.innerHeight (an earlier version of
  // this rule did that): App.vue's own resize reporting (notifyParentOfHeight) sizes
  // the LMS's outer iframe off this document's height, and that outer iframe IS this
  // window - so window.innerHeight was never an independent measurement, it was
  // downstream of whatever this very fallback last reported. Each render fed a larger
  // "remaining space" back into the next one, growing without bound (confirmed live:
  // the iframe visibly grew every frame until the LMS gave up and let it scroll
  // internally instead). A flat value can't feed back into itself this way. Safe now
  // that the old min-height: 100vh double-scrollbar problem this was trying to dodge
  // is fixed at its source (see .app--embedded in _global.scss) - the LMS's own page
  // scrolling to fit this height is the intended behavior, not something to avoid.
  & .no-resize {
    min-height: 600px;
  }
}
</style>
