<template>
  <div>
    <v-row
      class="integrations-header"
    >
      <h4>
        {{ title }}
      </h4>
      <div
        v-html="messages.header.directions"
        class="mt-3 mb-8"
      >
      </div>
      <div
        v-html="messages.header.instructions"
        class="mb-8"
      >
      </div>
    </v-row>
    <v-row
      class="row-sections"
    >
      <v-card
        class="mt-10 section-card focus-section"
      >
        <div
          class="section-circle focus-section-circle"
        >
          <span>1</span>
        </div>
        <v-card-title>
          {{ messages.section.titles.copy }}
        </v-card-title>
        <v-card-text>
          <div
            class="my-8"
          >
            <v-textarea
              v-model="launchUrl"
              :label="messages.launchUrl.label"
              :rules="textRules"
              :error="showIframeValidationError"
              @blur="validateIframeUrl"
              class="mb-3"
              rows="1"
              hide-details="auto"
              auto-grow
              outlined
              dense
              required
            />
            <div>{{ messages.launchUrl.instructions }}</div>
          </div>
          <div
            class="mb-8"
          >
            <v-text-field
              v-model="points"
              :rules="numberRules"
              label="Maximum points"
              type="number"
              step="any"
              hide-details="auto"
              class="mb-3"
              outlined
              required
            />
            <div>{{ messages.points.instructions }}</div>
          </div>
          <div
            v-if="showFeedbackEnabled"
            class="mb-4"
          >
            <v-checkbox
              v-model="feedbackEnabled"
              label="Tool allows students to view past submissions"
            />
          </div>
          <div
            :class="!showIframeValidationError ? 'mb-8' : 'mb-2'"
          >
            <v-btn
              :disabled="!enablePreviewButton"
              :href="!showIframeValidationError ? previewLaunchUrl : null"
              :color="!showIframeValidationError ? 'primary' : 'error'"
              target="_blank"
            >
              <v-icon>mdi-eye-outline</v-icon>
              PREVIEW
            </v-btn>
          </div>
          <div
            v-if="showIframeValidationError"
            class="mb-8 error--text"
          >
            Error rendering content. Please see
            <a
              :href="iframeInvalidInfoUrl"
              target="_blank"
            >
              this link
            </a>
            for more information.
          </div>
        </v-card-text>
      </v-card>
      <v-card
        :disabled="disableSection2"
        :class="{'focus-section': section1Complete}"
        class="mt-10 section-card"
      >
        <div
          :class="{
            'focus-section-circle': section1Complete,
            'inactive-section-circle': !section1Complete
          }"
          class="section-circle"
        >
          <span>2</span>
        </div>
        <v-card-title>
          {{ messages.section.titles.insert }}
        </v-card-title>
        <v-card-text>
          <div
            class="my-8"
          >
            <v-textarea
              v-model="returnUrl"
              class="return-url mb-3"
              rows="2"
              hide-details="auto"
              auto-grow
              readonly
              dense
            />
            <div
              class="copy-url"
            >
              <v-btn
                @click="copyReturnUrl"
                :color="copiedReturnUrl.buttonColor"
              >
                <v-icon>
                  {{ copiedReturnUrl.icon }}
                </v-icon>
                {{ copiedReturnUrl.label }}
              </v-btn>
            </div>
          </div>
          <div
            class="mb-8"
          >
            {{ messages.returnUrl.instructions.expects }}
          </div>
          <div
            v-html="messages.returnUrl.instructions.details"
            class="mb-8"
          >
          </div>
        </v-card-text>
      </v-card>
    </v-row>
  </div>
</template>

<script>
import { mapGetters, mapActions, mapMutations } from "vuex";

export default {
  props: {
    assessment: {
      type: Object,
      required: true
    },
    question: {
      type: Object,
      required: true
    }
  },
  data: () => ({
    showCopied: false,
    integrationQuestion: null,
    feedbackEnabled: false
  }),
  watch: {
    assessment: {
      handler(newAssessment) {
        if (!this.showFeedbackEnabled) {
          return;
        }

        this.feedbackEnabled = newAssessment ? newAssessment.allowStudentViewResponses : false;
      },
      deep: true
    },
    question: {
      handler(newQuestion) {
        this.integrationQuestion = newQuestion;
      },
      deep: true,
      immediate: true
    },
    integrationQuestion: {
      handler() {
        this.emit();
      },
      deep: true,
      immediate: true
    },
    feedbackEnabled: {
      handler() {
        this.emit();
      }
    },
    isIframeUrlValid: {
      handler() {
        this.emit();
      }
    }
  },
  computed: {
    ...mapGetters({
      isIframeUrlValid: "integrations/isIframeUrlValid",
      configurations: "configuration/get"
    }),
    integration() {
      return this.integrationQuestion.integration;
    },
    configuration() {
      return this.integration.configuration;
    },
    client() {
      return this.configuration.client;
    },
    clientName() {
      return this.client.name;
    },
    title() {
      return `${this.clientName} Integration`;
    },
    messages() {
      switch (this.clientName) {
        case "Custom Web Activity":
          return {
            header: {
              directions: `
                Terracotta allows you to present a custom interactive website as an assignment in ${this.lmsTitle}. First, provide the URL of the custom web activity and set the maximum score
                that students can receive. Second, edit the web activity so that, upon completing the survey, student submissions return to Terracotta.
              `,
              instructions: `
                For detailed instructions on how to do this,
                <a href="https://terracotta-education.atlassian.net/wiki/spaces/TC/pages/336330757/Terracotta+Custom+Web+Activity+Integration+Guide" target="_blank">click here</a>.
              `
            },
            section: {
              titles: {
                copy: `Launch to ${this.clientName}`,
                insert: "Return to Terracotta"
              }
            },
            launchUrl: {
              label: `${this.clientName} URL`,
              instructions: "Provide the URL of the custom web activity"
            },
            points: {
              instructions: "Indicate the maximum score that students can receive on this activity. The default score is 1."
            },
            returnUrl: {
              instructions: {
                expects: `
                  At the end of the custom web activity, students should be redirected to the URL above. Terracotta expects that the web activity
                  will return two URL parameters: (1) launch_token and (2) score.
                `,
                details: `
                  For detailed instructions on how to configure your custom web activity,
                  <a href="https://terracotta-education.atlassian.net/wiki/spaces/TC/pages/336330757/Terracotta+Custom+Web+Activity+Integration+Guide" target="_blank">click here</a>.
                `
              }
            }
          }
        default:
          return {
            header: {
              directions: `
                Terracotta makes it possible to present a Qualtrics survey as an assignment in ${this.lmsTitle}. First, provide the URL of the Qualtrics survey and set the maximum score
                that students can receive. Second, in your Qualtrics survey edit the End of Survey section so that, upon completing the survey, student submissions return to Terracotta.
              `,
              instructions: `
                For detailed instructions on how to do this,
                <a href="https://terracotta-education.atlassian.net/wiki/spaces/TC/pages/336265230/Terracotta+Qualtrics+Integration+Guide" target="_blank">click here</a>.
              `
            },
            section: {
              titles: {
                copy: `Launch to ${this.clientName}`,
                insert: "Return to Terracotta"
              }
            },
            launchUrl: {
              label: `${this.clientName} URL`,
              instructions: `Provide the URL of the ${this.clientName} survey.`
            },
            points: {
              instructions: "Indicate the maximum score that students can receive on the survey. The default score is 1."
            },
            returnUrl: {
              instructions: {
                expects: `
                  Edit the End of Survey section in Qualtrics so that students are redirected to the URL above. Terracotta expects that Qualtrics will
                  return two embedded data fields: (1) launch_token and (2) score.
                `,
                details: `
                  For detailed instructions on how to configure your ${this.clientName} survey,
                  <a href="https://terracotta-education.atlassian.net/wiki/spaces/TC/pages/336265230/Terracotta+Qualtrics+Integration+Guide" target="_blank">click here</a>.
                `
              }
            }
          }
      }
    },
    launchUrl: {
      get() {
        return this.configuration.launchUrl;
      },
      set(newLaunchUrl) {
        this.configuration.launchUrl = newLaunchUrl;
      }
    },
    points: {
      get() {
        return this.integrationQuestion.points;
      },
      set(newPoints) {
        this.integrationQuestion.points = newPoints;
      }
    },
    returnUrl() {
      return this.client.returnUrl || "";
    },
    section1Complete() {
      if (!this.validateLaunchUrl()) {
        // invalid url input
        return false;
      }

      if (!this.validatePoints()) {
        // no points input, is NaN, or is negative
        return false;
      }

      return true;
    },
    disableSection2() {
      return !this.section1Complete;
    },
    showFeedbackEnabled() {
      switch(this.clientName) {
        case "Custom Web Activity":
          return true;
        default:
          return false;
      }
    },
    enablePreviewButton() {
      return this.validateLaunchUrl();
    },
    previewUrl() {
      return this.integration.previewUrl;
    },
    previewLaunchUrl() {
      return `/integrations/preview?url=${btoa(this.launchUrl + this.previewUrl)}`;
    },
    textRules() {
      return [
        (launchUrl) => {
          return new RegExp(
            "^(https?:\\/\\/)?"+ // validate protocol
            "((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|"+ // validate domain name
            "((\\d{1,3}\\.){3}\\d{1,3}))"+ // validate OR ip (v4) address
            "(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*"+ // validate port and path
            "(\\?[;&a-z\\d%_.~+=-]*)?"+ // validate query string
            "(\\#[-a-z\\d_]*)?$","i" // validate fragment locator
          ).test(launchUrl)
          ||
          "Invalid URL. Please check the format and try again.";
        }
      ]
    },
    numberRules() {
      return [
        (points) => (points && !isNaN(points)) || "Maximum point value is required.",
        (points) => (!isNaN(parseFloat(points)) && points >= 0) || "Maximum point value cannot be negative.",
      ]
    },
    copiedReturnUrl() {
      return {
        buttonColor: this.showCopied ? "#38adb6" : "primary",
        icon: this.showCopied ? "mdi-check" :"mdi-content-copy",
        label: this.showCopied ? "COPIED" : "COPY URL"
      }
    },
    showIframeValidationError() {
      return this.launchUrl && !this.isIframeUrlValid;
    },
    iframeInvalidInfoUrl() {
      return this.assessment.integrationIframeInfoUrl;
    },
    lmsTitle() {
      return this.configurations?.lmsTitle || "LMS";
    }
  },
  methods: {
    ...mapActions({
      iframeUrl: "integrations/validateIframeUrl"
    }),
    ...mapMutations({
      setIframeValid: "integrations/setIframeValid"
    }),
    async copyReturnUrl() {
      try {
        await navigator.clipboard.writeText(this.returnUrl);
        this.showCopied = true;
      } catch($e) {
        console.log("error copying return url to clipboard");
      }
    },
    async validateLaunchUrl() {
      if (!this.launchUrl) {
        return false;
      }

      const pattern = new RegExp(
        "^(https?:\\/\\/)?"+ // validate protocol
        "((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|"+ // validate domain name
        "((\\d{1,3}\\.){3}\\d{1,3}))"+ // validate OR ip (v4) address
        "(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*"+ // validate port and path
        "(\\?[;&a-z\\d%_.~+=-]*)?"+ // validate query string
        "(\\#[-a-z\\d_]*)?$","i" // validate fragment locator
      );
      let isValid = pattern.test(this.launchUrl);

      if (!isValid) {
        return false;
      }

      return this.isIframeUrlValid;
    },
    validatePoints() {
      if (this.points === null || (this.points + "").trim() === "") {
        // null or blank
        return false;
      }

      if (isNaN(this.points)) {
        // not a number
        return false;
      }

      if (this.points < 0) {
        // negative value
        return false;
      }

      return true;
    },
    async validateIframeUrl() {
      this.$emit("url-validation-in-progress", true);
      await this.iframeUrl(this.launchUrl);
      await this.emit();
      this.$emit("url-validation-in-progress", false);
    },
    async emit() {
      let urlValid = await this.validateLaunchUrl();
      await this.$emit(
          "integration-updated",
          {
            ...this.integrationQuestion,
            launchUrlValidated: urlValid,
            pointsValidated: this.validatePoints(),
            feedbackEnabled: this.showFeedbackEnabled ? this.feedbackEnabled : this.assessment.allowStudentViewResponses
          }
        );
    }
  },
  async mounted() {
    this.integrationQuestion = this.question;

    if (this.showFeedbackEnabled) {
      this.feedbackEnabled = this.assessment ? this.assessment.allowStudentViewResponses : false;
    }

    if (this.launchUrl) {
      await this.validateIframeUrl();
    } else {
      this.setIframeValid(true);
    }

    this.emit();
  }
}
</script>

<style scoped>
.row {
  margin: 0px !important;
}
h4 {
  font-weight: bold !important;
}
div.row-sections {
  justify-content: space-between;
  > .v-card {
    max-width: 49%;
    border: thin solid rgba(224, 224, 224, 1);
    border-radius: 10px;
    box-shadow: none;
    & .v-card__subtitle,
    & .v-card__text {
      padding-bottom: 0px !important;
      font-size: 16px !important;
      color: rgba(0, 0, 0, .87) !important;
    }
    & .v-card__subtitle {
      font-weight: bold;
    }
    & .v-card__title {
      max-width: fit-content !important;
      margin: 0 auto !important;
    }
  }
  & .section-card {
    position: relative;
    padding-top: 30px;
    border-width: 2px;
    & .section-circle {
      width: 54px;
      height: 54px;
      border-radius: 50%;
      text-align: center;
      font-size: 24px;
      font-weight: bold;
      align-content: center;
      color:white;
      position: absolute;
      top: -27px;
      left: 50%;
      transform: translateX(-50%);
      opacity: 1;
    }
    & .focus-section-circle {
      border: 2px solid rgba(29, 157, 255, 1);
      color: rgba(29, 157, 255, 1);
      background: rgba(237, 247, 255, 1);
    }
    & .inactive-section-circle {
      border: 2px solid rgba(224, 224, 224, 1);
      color: rgba(224, 224, 224, 1);
      background:white;
    }
    & .return-url {
      background-color: rgba(29, 157, 255, .1);
      border-radius: 10px;
      & textarea {
        border-width: 0px;
      }
      & .v-input__control {
        padding: 0 12px;
      }
      & *,
      & *:before,
      & *:after {
        border-width: 0 !important;
      }
    }
    & .theme--light {
      &.v-label {
        color: rgba(0, 0, 0, .87);
      }
      &.v-btn.v-btn--disabled {
        &.v-btn--has-bg {
          background-color: rgba(0, 119, 210, 1) !important;
          opacity: .2 !important;
          color: white !important;
        }
        & .v-icon {
          color: white !important;
        }
      }
    }
    & .v-btn__content {
      color: white;
    }
    & .copy-url {
      display: flex;
      flex-direction: row;
      align-items: start;
      & .copied-label {
        height:fit-content;
        vertical-align: middle;
        margin: auto 0;
      }
    }
  }
  & .focus-section {
    border-color: rgba(29, 157, 255, 1);
  }
  .v-card > :first-child:not(.v-btn):not(.v-chip) {
    border-top-left-radius: 50%;
    border-top-right-radius: 50%;
  }
  .v-text-field--outlined.v-input--dense.v-text-field--outlined > .v-input__control > .v-input__slot {
    min-height: 56px;
  }
  .v-text-field--outlined.v-input--dense .v-label {
    top: auto;
    &.v-label--active {
      top: 10px;
    }
  }
}
</style>
