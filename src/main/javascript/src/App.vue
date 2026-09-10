<template>
  <v-app
    :style="appStyle"
    tabindex="0"
  >
    <SkipTo
      v-if="showSkipLink"
    />

    <!-- Main App -->
    <v-main
      v-if="!isIntegration && !isObsolete"
    >
      <StatusAlert />

      <!-- Instructor -->
      <template
        v-if="!isTreatmentPreview && hasTokens && userInfo === 'Instructor'"
      >
        <router-view
          :key="$route.path"
        />
      </template>

      <!-- Treatment Preview -->
      <template
        v-else-if="isTreatmentPreview"
      >
        <PageLoading
          v-if="!isTreatmentPreviewComplete"
          :display="!childLoaded"
          message="Loading your preview. Please wait."
        />

        <StudentQuiz
          v-if="!isTreatmentPreviewComplete"
          :experiment-id="treatmentPreview.experimentId"
          :preview-condition-id="treatmentPreview.conditionId"
          :preview-treatment-id="treatmentPreview.treatmentId"
          :preview-id="treatmentPreview.previewId"
          :owner-id="treatmentPreview.ownerId"
          :preview="true"
          @loaded="childLoaded = true"
        />

        <TreatmentPreviewComplete
          v-if="isTreatmentPreviewComplete"
        />
      </template>

      <!-- Learner -->
      <template
        v-else-if="hasTokens && userInfo === 'Learner'"
      >
        <div class="student-view">
          <IntegrationsTokenAlert
            v-if="
              !consent &&
              assignmentId &&
              childLoaded &&
              integrationsTokenAlert
            "
            :alert="integrationsTokenAlert"
          />

          <PageLoading
            :display="!childLoaded"
            message="Loading your assignment. Please wait."
            class="mt-5"
          />

          <StudentConsent
            v-if="consent"
            :experiment-id="experimentId"
            :user-id="userId"
            @loaded="childLoaded = true"
          />

          <StudentQuiz
            v-if="!consent && assignmentId"
            :experiment-id="experimentId"
            :assignment-id="assignmentId"
            :preview="false"
            @loaded="childLoaded = true"
            @integrationsTokenAlert="
              integrationsTokenAlert = $event
            "
          />
        </div>
      </template>

      <!-- Error -->
      <template v-else>
        <v-row justify="center">
          <v-col md="6">
            <v-alert
              type="error"
              variant="outlined"
            >
              <v-row align="center">
                <v-col class="grow">
                  Error
                </v-col>
              </v-row>
            </v-alert>
          </v-col>
        </v-row>
      </template>
    </v-main>

    <!-- Integrations -->
    <v-main
      v-else-if="isIntegration"
    >
      <Integrations
        v-if="!isIntegrationPreview"
        :integration-data="integrationData"
        @integrationsTokenAlert="
          integrationsTokenAlert = $event
        "
      />

      <IntegrationsPreview
        v-if="isIntegrationPreview"
        :url="integrationPreviewUrl"
      />
    </v-main>

    <!-- Obsolete -->
    <v-main v-else>
      <Assignment
        v-if="isObsoleteAssignment"
      />
    </v-main>
  </v-app>
</template>

<script setup>
import {
  ref,
  computed,
  defineAsyncComponent,
  nextTick,
  onMounted,
  onBeforeUnmount
} from "vue";

import { useRoute } from "vue-router";
import Swal from "sweetalert2";

import Assignment from "@/views/obsolete/Assignment.vue";
import Integrations from "@/views/integrations/Integrations.vue";
import IntegrationsPreview from "@/views/integrations/IntegrationsPreview.vue";
import IntegrationsTokenAlert from "@/views/integrations/IntegrationsTokenAlert.vue";
import PageLoading from "@/components/PageLoading.vue";
import SkipTo from "@/components/SkipTo.vue";
import StatusAlert from "@/components/alert/StatusAlert.vue";
import StudentQuiz from "@/views/student/quiz/StudentQuiz.vue";
import TreatmentPreviewComplete from "@/views/preview/TreatmentPreviewComplete.vue";

// Lazy-loaded: StudentConsent pulls in vue-pdf-embed/pdfjs-dist (~2.5MB). A static
// import here would put that in App.vue's eager bundle for every page load, even
// though this component only renders when consent is actually pending.
const StudentConsent = defineAsyncComponent(() => import("@/views/student/StudentConsent.vue"));

import { api as apiModule } from "@/store/api.module";
import { configuration as configurationModule } from "@/store/configuration.module";

defineOptions({
  name: "App"
});

const props = defineProps({
  integrationData: {
    type: Object,
    default: null
  },
  obsoleteData: {
    type: Object,
    default: null
  },
  treatmentPreviewData: {
    type: Object,
    default: null
  }
});

const route = useRoute();

const apiStore = apiModule();
const configurationStore = configurationModule();

// -------------------------------------
// State
// -------------------------------------

const childLoaded = ref(false);
const integrationsTokenAlert = ref(null);

let refreshInterval = null;
let sessionExpired = false;
let refreshInFlight = false; // avoid overlapping calls if the interval tick and a
                              // visibilitychange fire close together

// -------------------------------------
// Pinia State
// -------------------------------------

const hasTokens = computed(() => apiStore.hasTokens);
const userInfo = computed(() => apiStore.userInfo);
const experimentId = computed(() => apiStore.experimentId);
const assignmentId = computed(() => apiStore.assignmentId);
const consent = computed(() => apiStore.consent);
const userId = computed(() => apiStore.userId);
const apiToken = computed(() => apiStore.apiToken);

const configuration = computed(
  () => configurationStore.configurations
);

// -------------------------------------
// Computed
// -------------------------------------

const appStyle = computed(() => {
  return route.meta.appStyle;
});

const isIntegration = computed(() => {
  return props.integrationData != null;
});

const integrationPreviewUrl = computed(() => {
  return props.integrationData?.previewUrl || null;
});

const isIntegrationPreview = computed(() => {
  return (
    isIntegration.value &&
    !!integrationPreviewUrl.value
  );
});

const isObsolete = computed(() => {
  return props.obsoleteData != null;
});

const isObsoleteAssignment = computed(() => {
  return (
    isObsolete.value &&
    props.obsoleteData?.type === "assignment"
  );
});

const isTreatmentPreview = computed(() => {
  return props.treatmentPreviewData?.preview || false;
});

const isTreatmentPreviewComplete = computed(() => {
  console.log("props", {props});
  return props.treatmentPreviewData?.complete || false;
});

const treatmentPreview = computed(() => ({
  experimentId:
    props.treatmentPreviewData?.experimentId ?? null,

  conditionId:
    props.treatmentPreviewData?.conditionId ?? null,

  treatmentId:
    props.treatmentPreviewData?.treatmentId ?? null,

  previewId:
    props.treatmentPreviewData?.previewId ?? null,

  ownerId:
    props.treatmentPreviewData?.ownerId ?? null,

  complete:
    props.treatmentPreviewData?.complete ?? false
}));

const showSkipLink = computed(() => {
  return configuration.value?.showSkipLink || false;
});

// -------------------------------------
// Methods
// -------------------------------------

const refreshToken = () => {
  return apiStore.refreshToken();
};

const retrieveConfiguration = () => {
  return configurationStore.retrieve();
};

// clears stale persisted state (e.g. pinia-plugin-persistedstate's auth token) on every
// fresh mount, without wiping StudentQuiz.vue's draft-answer safety net keys, which need
// to survive exactly the relaunch this mount represents
const clearStaleStorageExceptDrafts = () => {
  const draftPrefix = "terracotta-quiz-draft-";

  Object.keys(localStorage)
    .filter(key => !key.startsWith(draftPrefix))
    .forEach(key => localStorage.removeItem(key));
};

// Tells the LMS platform (Canvas et al - this is the standard LTI Platform Messages
// postMessage subject, not Terracotta-specific) how tall this page actually is, so it
// can size ITS OWN iframe (the one wrapping the whole Terracotta tool) to fit,
// instead of leaving it at whatever default height the platform picked. Without
// this, that outer iframe can end up shorter than Terracotta's real content, which
// means a second, outer scrollbar on top of whatever's already inside the tool - the
// same double-scrollbar failure mode already fixed for the student integration
// quiz's own nested iframe, just one level up. This runs for every page in the app
// (not just that one flow), reacting to any layout change, not just the cases that
// happen to already have their own resize handling.
//
// Matches the height calculation the resize-observer scripts under
// public/js/integrations/resize/ already use on the OTHER side of a similar handshake
// (an embedded integration tool reporting ITS size to Terracotta), for consistency.
let frameResizeObserver = null;

const isEmbeddedInAnIframe = () => {
  try {
    return window.self !== window.top;
  } catch {
    // a cross-origin parent throws on window.top access in some browsers - if we
    // can't tell, assume embedded, since that's the case this exists for
    return true;
  }
};

const notifyParentOfHeight = () => {
  const height = Math.max(document.body.offsetHeight, document.documentElement.offsetHeight);

  window.parent.postMessage({ subject: "lti.frameResize", height }, "*");
};

const startFrameResizeReporting = () => {
  if (!isEmbeddedInAnIframe()) {
    return;
  }

  notifyParentOfHeight();
  frameResizeObserver = new ResizeObserver(notifyParentOfHeight);
  frameResizeObserver.observe(document.body);
};

const stopFrameResizeReporting = () => {
  frameResizeObserver?.disconnect();
  frameResizeObserver = null;
};

const stopTokenMonitoring = () => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
    refreshInterval = null;
  }

  document.removeEventListener("visibilitychange", handleVisibilityChange);
};

const checkAndRefreshToken = async () => {
  if (sessionExpired || refreshInFlight || !apiToken.value) {
    return;
  }

  if (apiStore.isApiTokenExpired()) {
    sessionExpired = true;
    stopTokenMonitoring();
    apiStore.markSessionExpired();
    await nextTick(); // let StudentQuiz.vue's draft-save watcher react first

    await Swal.fire(
      "Your session has expired. Please return to your course and re-open this assignment to continue."
    );

    return;
  }

  refreshInFlight = true;

  try {
    await refreshToken();
  } finally {
    refreshInFlight = false;
  }
};

const handleVisibilityChange = () => {
  if (document.visibilityState === "visible") {
    checkAndRefreshToken();
  }
};

// -------------------------------------
// Lifecycle
// -------------------------------------

onMounted(async () => {
  clearStaleStorageExceptDrafts();

  if (!isTreatmentPreview.value) {
    await retrieveConfiguration();
  }

  refreshInterval = window.setInterval(() => {
    checkAndRefreshToken();
  }, 1000 * 60 * 59);

  document.addEventListener("visibilitychange", handleVisibilityChange);
  startFrameResizeReporting();
});

onBeforeUnmount(() => {
  stopTokenMonitoring();
  stopFrameResizeReporting();
});
</script>

<style lang="scss">
h1,
h2,
h3,
h4 {
  line-height: 1.2;
  font-weight: 400;
  padding-bottom: 10px;
}

p {
  padding-bottom: 15px;
}
</style>
