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
  onMounted,
  onBeforeUnmount
} from "vue";

import { useRoute } from "vue-router";

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
  return apiStore.refreshToken(apiToken.value);
};

const retrieveConfiguration = () => {
  return configurationStore.retrieve();
};

// -------------------------------------
// Lifecycle
// -------------------------------------

onMounted(async () => {
  localStorage.clear();

  if (!isTreatmentPreview.value) {
    await retrieveConfiguration();
  }

  refreshInterval = window.setInterval(() => {
    refreshToken();
  }, 1000 * 60 * 59);
});

onBeforeUnmount(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
  }
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
