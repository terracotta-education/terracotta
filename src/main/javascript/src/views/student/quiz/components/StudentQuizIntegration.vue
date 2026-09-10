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
        ref="iframeEl"
        :src="integration.launchUrl"
        :class="{ 'no-resize': !hasResizeMessage }"
        :style="hasResizeMessage ? {} : { height: `${fallbackHeight}px` }"
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
import { ref, onMounted, onBeforeUnmount } from "vue";
import ExternalIntegrationResponseEditor from "@/views/integrations/ExternalIntegrationResponseEditor.vue";

defineProps({
  assessment: { type: Object, required: true },
  integration: { type: Object, required: true },
  readonly: { type: Boolean, default: false },
  submitted: { type: Boolean, default: false },
  selectedSubmission: { type: Object, default: null },
  hasResizeMessage: { type: Boolean, default: false }
});

const iframeEl = ref(null);
// a sane guess for the very first paint, before onMounted's real measurement runs -
// see measureFallbackHeight's own comment for why this can't just be a fixed value
// generally.
const fallbackHeight = ref(400);
let resizeObserver = null;

// Keeps this iframe's own contribution to the page's height bounded to whatever
// vertical space is actually left below it in the viewport, so the fallback used
// before the embedded tool posts its real resize message (handleIntegrationsResize in
// StudentQuiz.vue) can't itself push this document taller than one viewport - which
// is exactly what caused the double-scrollbar bug a flat CSS min-height had (see this
// file's git history). A fixed px guess can't adapt to that: how much banner/
// instruction content renders above this iframe varies (retake banner, submission
// details, the assessment intro html above), and so does the viewport itself.
//
// This matters even more for any integration that never sends a resize message at
// all - the resize-observer scripts under public/js/integrations/resize/ are
// something the integration's OWN author has to add on their end; nothing here can
// assume they did. For those, this isn't a brief loading state, it's the iframe's
// height for the rest of the session, so it needs to be a reasonable real size, not
// just "not broken for a moment."
const measureFallbackHeight = () => {
  if (!iframeEl.value) {
    return;
  }

  const MIN_HEIGHT = 300; // still usable if something above pushes this near/past the fold
  const top = iframeEl.value.getBoundingClientRect().top;

  fallbackHeight.value = Math.max(window.innerHeight - top, MIN_HEIGHT);
};

onMounted(() => {
  measureFallbackHeight();
  // catches both viewport resizes and layout shifts from content elsewhere on the
  // page (a banner appearing/disappearing, the assessment intro html rendering) -
  // observing document.body is broad on purpose, since enumerating every possible
  // cause of "content above this iframe changed height" isn't practical.
  resizeObserver = new ResizeObserver(measureFallbackHeight);
  resizeObserver.observe(document.body);
  window.addEventListener("resize", measureFallbackHeight);
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  window.removeEventListener("resize", measureFallbackHeight);
});
</script>

<style lang="scss" scoped>
.integration {
  min-width: 100%;

  // no min-height here (or on .v-col below) - Vuetify's v-application renders with
  // its "full-height" layout mode, which this row/col's own min-height: 100% used to
  // resolve against regardless of how much space any PRECEDING sibling content
  // (retake banner, submission details, etc. in StudentQuiz.vue) already used above
  // it. That's a second, independent way this page's total height could end up
  // taller than one viewport - confirmed via a standalone reproduction with a
  // stand-in "banner" div above this component: even with the iframe itself sized
  // exactly to the remaining viewport space (see measureFallbackHeight's comment
  // above), this row/col still inflated to a FULL extra viewport-height's worth of
  // space on top of that banner, because min-height: 100% doesn't know or care what
  // else is already on the page. Without it, this area sizes to its own content
  // (the iframe's own height, which is already correctly computed) instead of
  // fighting it.
  & > .v-col {
    min-width: 100%;

    & > iframe {
      // an iframe defaults to display: inline, which sits it on a text baseline and
      // reserves a few extra px below it for descenders (the same "mystery gap
      // under an image" quirk that comes up whenever an inline-replaced element
      // sits in a block context) - block avoids that, so this element's own
      // measured height (see measureFallbackHeight above) is really all the extra
      // vertical space its container ends up needing.
      display: block;
      min-width: 100%;
      border: none;
    }
  }

  // .no-resize itself carries no height rule any more - the fallback height is set
  // as an inline style computed by measureFallbackHeight() in the script above (a
  // fixed CSS value, like the min-height: 100vh this used to be, can't adapt to how
  // much banner/instruction content is actually rendered above this iframe or to the
  // viewport size, and reliably guessed wrong in a way that caused a double
  // scrollbar - see that function's comment). The class stays as a hook for
  // tests/styling to key off "we don't have a real height yet" if needed.
}
</style>
