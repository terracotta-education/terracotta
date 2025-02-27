<template>
  <v-app
    :style="appStyle"
    tabindex="0"
  >
    <v-main
      v-if="!isIntegration && !isObsolete"
    >
      <template
        v-if="!isTreatmentPreview && hasTokens && userInfo === 'Instructor'"
      >
        <router-view
          :key="$route.fullPath"
        />
      </template>
      <template
        v-else-if="isTreatmentPreview"
      >
        <page-loading
          v-if="!isTreatmentPreviewComplete"
          :display="!childLoaded"
          message="Loading your preview. Please wait."
        />
        <student-quiz
          v-if="!isTreatmentPreviewComplete"
          :experimentId="treatmentPreview.experimentId"
          :condition_id="treatmentPreview.conditionId"
          :treatment_id="treatmentPreview.treatmentId"
          :previewId="treatmentPreview.previewId"
          :ownerId="treatmentPreview.ownerId"
          :preview="true"
          @loaded="childLoaded = true"
        />
        <treatment-preview-complete
          v-if="isTreatmentPreviewComplete"
        />
      </template>
      <template
        v-else-if="hasTokens && userInfo === 'Learner'"
      >
        <div class="student-view mt-5">
          <page-loading
            :display="!childLoaded"
            message="Loading your assignment. Please wait."
          />
          <student-consent
            v-if="consent"
            :experimentId="experimentId"
            :userId="userId"
            @loaded="childLoaded = true"
          />
          <student-quiz
            v-if="!consent && assignmentId"
            :experimentId="experimentId"
            :assignment_id="assignmentId"
            :preview="false"
            @loaded="childLoaded = true"
          />
        </div>
      </template>
      <template
        v-else
      >
        <v-row
          justify="center"
        >
          <v-col
            md="6"
          >
            <v-alert
              prominent
              type="error"
            >
              <v-row
                align="center"
              >
                <v-col
                  class="grow"
                >
                  Error
                </v-col>
              </v-row>
            </v-alert>
          </v-col>
        </v-row>
      </template>
    </v-main>
    <v-main
      v-else-if="isIntegration"
    >
      <integrations
        v-if="!isIntegrationPreview"
        :integrationData="integrationData"
      />
      <integrations-preview
        v-if="isIntegrationPreview"
        :url="integrationPreviewUrl"
      />
    </v-main>
    <v-main
      v-else
    >
      <assignment
        v-if="isObsoleteAssignment"
      />
    </v-main>
  </v-app>
</template>

<script>
import { mapActions, mapGetters } from "vuex";
import Assignment from "@/views/obsolete/Assignment.vue";
import Integrations from "@/views/integrations/Integrations.vue";
import IntegrationsPreview from "@/views/integrations/IntegrationsPreview.vue";
import PageLoading from "@/components/PageLoading";
import StudentConsent from "@/views/student/StudentConsent.vue";
import StudentQuiz from "@/views/student/StudentQuiz.vue";
import TreatmentPreviewComplete from "@/views/preview/TreatmentPreviewComplete.vue";

export default {
  name: "App",
  components: {
    Assignment,
    Integrations,
    IntegrationsPreview,
    PageLoading,
    StudentQuiz,
    StudentConsent,
    TreatmentPreviewComplete
  },
  props: {
    integrationData: {
      type: Object
    },
    obsoleteData: {
      type: Object
    },
    treatmentPreviewData: {
      type: Object
    }
  },
  data: () => ({
    childLoaded: false
  }),
  computed: {
    ...mapGetters({
      hasTokens: "api/hasTokens",
      userInfo: "api/userInfo",
      experimentId: "api/experimentId",
      assignmentId: "api/assignmentId",
      consent: "api/consent",
      userId: "api/userId",
      apiToken: "api/api_token",
    }),
    // Apply per route global styling to the v-app component
    appStyle() {
      return this.$route.meta.appStyle;
    },
    isIntegration() {
      return this.integrationData != null;
    },
    integrationPreviewUrl() {
      return this.integrationData?.previewUrl || null;
    },
    isIntegrationPreview() {
      return this.isIntegration && this.integrationPreviewUrl;
    },
    isObsolete() {
      return this.obsoleteData != null;
    },
    isObsoleteAssignment() {
      return this.isObsolete && this.obsoleteData.type === "assignment";
    },
    isTreatmentPreview() {
      return this.treatmentPreviewData?.preview || false;
    },
    isTreatmentPreviewComplete() {
      return this.treatmentPreviewData?.complete || false;
    },
    treatmentPreview() {
      return {
        experimentId: this.treatmentPreviewData?.experimentId || null,
        conditionId: this.treatmentPreviewData?.conditionId || null,
        treatmentId: this.treatmentPreviewData?.treatmentId || null,
        previewId: this.treatmentPreviewData?.previewId || null,
        ownerId: this.treatmentPreviewData?.ownerId || null,
        complete: this.treatmentPreviewData?.complete || false
      }
    }
  },
  methods: {
    ...mapActions({
      refreshToken: "api/refreshToken",
    }),
  },
  async created() {
    localStorage.clear();

    setInterval(function () {
      this.refreshToken(this.apiToken);
    }.bind(this), 1000 * 60 * 59);
  },
};
</script>

<style lang="scss" >
  @import "./styles/custom";

  h1, h2, h3, h4 {
    line-height: 1.2;
    font-weight: 400;
    padding-bottom: 10px;
  }
  p {
    padding-bottom: 15px;
  }
  .student-view {
    display: flex;
  }
</style>
