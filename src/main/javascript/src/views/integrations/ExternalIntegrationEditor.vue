<template>
  <div>
    <v-row class="integrations-header">
      <h4>
        {{ title }}
      </h4>

      <div
        v-html="messages.header.directions"
      />

      <div
        v-html="messages.header.instructions"
      />
    </v-row>

    <v-row
      class="row-sections"
      density="compact"
      gap="compact"
    >
      <v-card class="mt-10 section-card focus-section">
        <div class="section-circle focus-section-circle">
          <span>1</span>
        </div>

        <v-card-title>
          {{ messages.section.titles.copy }}
        </v-card-title>

        <v-card-text>
          <div class="my-8">
            <v-textarea
              v-model="launchUrl"
              :label="messages.launchUrl.label"
              :rules="textRules"
              :error="showIframeValidationError"
              hide-details="auto"
              variant="outlined"
              class="mb-3"
              rows="1"
              @blur="validateIframeUrl"
              auto-grow
              required
            />

            <div>
              {{ messages.launchUrl.instructions }}
            </div>
          </div>

          <div class="mb-8">
            <v-text-field
              v-model="points"
              :rules="numberRules"
              label="Maximum points"
              type="number"
              step="any"
              hide-details="auto"
              class="mb-3"
              variant="outlined"
              required
            />

            <div>
              {{ messages.points.instructions }}
            </div>
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

          <div :class="!showIframeValidationError ? 'mb-8' : 'mb-2'">
            <v-btn
              :disabled="!enablePreviewButton"
              :href="!showIframeValidationError ? previewLaunchUrl : null"
              :color="!showIframeValidationError ? 'primary' : 'error'"
              class="preview-btn"
              target="_blank"
            >
              <v-icon>mdi-eye-outline</v-icon>
              PREVIEW
            </v-btn>
          </div>

          <div
            v-if="showIframeValidationError"
            class="mb-8 error-text"
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
        :class="{ 'focus-section': section1Complete }"
        class="mt-10 section-card section-2"
      >
        <div
          :class="{
            'focus-section-circle': !disableSection2,
            'inactive-section-circle': disableSection2
          }"
          class="section-circle"
        >
          <span>2</span>
        </div>

        <v-card-title>
          {{ messages.section.titles.insert }}
        </v-card-title>

        <v-card-text>
          <div class="my-8">
            <v-textarea
              v-model="returnUrl"
              class="return-url mb-3"
              rows="2"
              hide-details="auto"
              aria-label="redirect URL back to Terracotta"
              auto-grow
              readonly
              density="compact"
            />

            <div class="copy-url">
              <v-btn
                :color="copiedReturnUrl.buttonColor"
                class="copy-url-btn"
                @click="copyReturnUrl"
              >
                <v-icon>
                  {{ copiedReturnUrl.icon }}
                </v-icon>

                {{ copiedReturnUrl.label }}
              </v-btn>
            </div>
          </div>

          <div class="mb-8">
            {{ messages.returnUrl.instructions.expects }}
          </div>

          <div
            v-html="messages.returnUrl.instructions.details"
            class="mb-8"
          />
        </v-card-text>
      </v-card>
    </v-row>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  watch,
  onMounted
} from "vue";

import { integrations as integrationsModule } from "@/store/integrations/integrations.module";
import { configuration as configurationModule } from "@/store/configuration.module";

defineOptions({
  name: "ExternalIntegrationEditor"
});

const props = defineProps({
  assessment: {
    type: Object,
    required: true
  },
  question: {
    type: Object,
    required: true
  }
});

const emit = defineEmits([
  "integration-updated",
  "url-validation-in-progress"
]);

const integrationsStore = integrationsModule();
const configurationStore = configurationModule();

const URL_PATTERN = new RegExp(
  "^(https?:\\/\\/)?" +
    "((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|" +
    "((\\d{1,3}\\.){3}\\d{1,3}))" +
    "(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*" +
    "(\\?[;&a-z\\d%_.~+=-]*)?" +
    "(\\#[-a-z\\d_]*)?$",
  "i"
);

const showCopied = ref(false);
const integrationQuestion = ref(null);
const feedbackEnabled = ref(false);

const isIframeUrlValid = computed(() => {
  return integrationsStore.isIframeUrlValid;
});

const configurations = computed(() => {
  return configurationStore.configurations;
});

const integration = computed(() => {
  return integrationQuestion.value?.integration;
});

const configuration = computed(() => {
  return integration.value?.configuration || {};
});

const client = computed(() => {
  return configuration.value?.client || {};
});

const clientName = computed(() => {
  return client.value?.name || "";
});

const title = computed(() => {
  return `${clientName.value} Integration`;
});

const lmsTitle = computed(() => {
  return configurations.value?.lmsTitle || "LMS";
});

const messages = computed(() => {
  if (clientName.value === "Custom Web Activity") {
    return {
      header: {
        directions: `
          Terracotta allows you to present a custom interactive website as an assignment in ${lmsTitle.value}. First, provide the URL of the custom web activity and set the maximum score
          that students can receive. Second, edit the web activity so that, upon completing the survey, student submissions return to Terracotta.
        `,
        instructions: `
          For detailed instructions on how to do this,
          <a href="https://terracotta-education.atlassian.net/wiki/spaces/TC/pages/336330757/Terracotta+Custom+Web+Activity+Integration+Guide" target="_blank">click here</a>.
        `
      },
      section: {
        titles: {
          copy: `Launch to ${clientName.value}`,
          insert: "Return to Terracotta"
        }
      },
      launchUrl: {
        label: `${clientName.value} URL`,
        instructions: "Provide the URL of the custom web activity"
      },
      points: {
        instructions:
          "Indicate the maximum score that students can receive on this activity. The default score is 1."
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
    };
  }

  return {
    header: {
      directions: `
        Terracotta makes it possible to present a Qualtrics survey as an assignment in ${lmsTitle.value}. First, provide the URL of the Qualtrics survey and set the maximum score
        that students can receive. Second, in your Qualtrics survey edit the End of Survey section so that, upon completing the survey, student submissions return to Terracotta.
      `,
      instructions: `
        For detailed instructions on how to do this,
        <a href="https://terracotta-education.atlassian.net/wiki/spaces/TC/pages/336265230/Terracotta+Qualtrics+Integration+Guide" target="_blank">click here</a>.
      `
    },
    section: {
      titles: {
        copy: `Launch to ${clientName.value}`,
        insert: "Return to Terracotta"
      }
    },
    launchUrl: {
      label: `${clientName.value} URL`,
      instructions: `Provide the URL of the ${clientName.value} survey.`
    },
    points: {
      instructions:
        "Indicate the maximum score that students can receive on the survey. The default score is 1."
    },
    returnUrl: {
      instructions: {
        expects: `
          Edit the End of Survey section in Qualtrics so that students are redirected to the URL above. Terracotta expects that Qualtrics will
          return two embedded data fields: (1) launch_token and (2) score.
        `,
        details: `
          For detailed instructions on how to configure your ${clientName.value} survey,
          <a href="https://terracotta-education.atlassian.net/wiki/spaces/TC/pages/336265230/Terracotta+Qualtrics+Integration+Guide" target="_blank">click here</a>.
        `
      }
    }
  };
});

const launchUrl = computed({
  get() {
    return configuration.value.launchUrl;
  },

  set(value) {
    configuration.value.launchUrl = value;
  }
});

const points = computed({
  get() {
    return integrationQuestion.value?.points;
  },

  set(value) {
    integrationQuestion.value.points = value;
  }
});

const returnUrl = computed(() => {
  return client.value?.returnUrl || "";
});

const launchUrlValid = computed(() => {
  return Boolean(
    launchUrl.value &&
      URL_PATTERN.test(launchUrl.value) &&
      isIframeUrlValid.value
  );
});

const section1Complete = computed(() => {
  return launchUrlValid.value && validatePoints();
});

const disableSection2 = computed(() => {
  return !section1Complete.value;
});

const showFeedbackEnabled = computed(() => {
  return clientName.value === "Custom Web Activity";
});

const enablePreviewButton = computed(() => {
  return launchUrlValid.value;
});

const previewUrl = computed(() => {
  return integration.value?.previewUrl;
});

const previewLaunchUrl = computed(() => {
  return `/integrations/preview?url=${btoa(
    `${launchUrl.value}${previewUrl.value}`
  )}`;
});

const textRules = computed(() => {
  return [
    value =>
      URL_PATTERN.test(value) ||
      "Invalid URL. Please check the format and try again."
  ];
});

const numberRules = computed(() => {
  return [
    value =>
      value !== null &&
        `${value}`.trim() !== "" &&
        !Number.isNaN(Number(value)) ||
      "Maximum point value is required.",
    value =>
      !Number.isNaN(Number.parseFloat(value)) &&
        Number.parseFloat(value) >= 0 ||
      "Maximum point value cannot be negative."
  ];
});

const copiedReturnUrl = computed(() => {
  return {
    buttonColor: showCopied.value ? "#00614c" : "primary",
    icon: showCopied.value ? "mdi-check" : "mdi-content-copy",
    label: showCopied.value ? "COPIED" : "COPY URL"
  };
});

const showIframeValidationError = computed(() => {
  return Boolean(launchUrl.value && !isIframeUrlValid.value);
});

const iframeInvalidInfoUrl = computed(() => {
  return props.assessment.integrationIframeInfoUrl;
});

watch(
  () => props.assessment,
  value => {
    if (!showFeedbackEnabled.value) {
      return;
    }

    feedbackEnabled.value = value
      ? value.allowStudentViewResponses
      : false;
  },
  { deep: true }
);

watch(
  () => props.question,
  value => {
    integrationQuestion.value = value;
  },
  {
    deep: true,
    immediate: true
  }
);

watch(
  integrationQuestion,
  () => {
    emitIntegrationUpdate();
  },
  {
    deep: true
  }
);

watch(feedbackEnabled, () => {
  emitIntegrationUpdate();
});

watch(isIframeUrlValid, () => {
  emitIntegrationUpdate();
});

const copyReturnUrl = async () => {
  try {
    await navigator.clipboard.writeText(returnUrl.value);
    showCopied.value = true;
  } catch {
    console.error("Failed to copy return URL");
  }
};

const validatePoints = () => {
  const parsedPoints = Number.parseFloat(points.value);

  return (
    points.value !== null &&
    `${points.value}`.trim() !== "" &&
    !Number.isNaN(parsedPoints) &&
    parsedPoints >= 0
  );
};

const validateIframeUrl = async () => {
  emit("url-validation-in-progress", true);

  await integrationsStore.validateIframeUrl(launchUrl.value);

  emitIntegrationUpdate();

  emit("url-validation-in-progress", false);
};

const emitIntegrationUpdate = () => {
  if (!integrationQuestion.value) {
    return;
  }

  emit("integration-updated", {
    ...integrationQuestion.value,
    launchUrlValidated: launchUrlValid.value,
    pointsValidated: validatePoints(),
    feedbackEnabled: showFeedbackEnabled.value
      ? feedbackEnabled.value
      : props.assessment.allowStudentViewResponses
  });
};

onMounted(async () => {
  integrationQuestion.value = props.question;

  if (showFeedbackEnabled.value) {
    feedbackEnabled.value = props.assessment
      ? props.assessment.allowStudentViewResponses
      : false;
  }

  if (launchUrl.value) {
    await validateIframeUrl();
  } else {
    integrationsStore.setIframeValid(true);
  }

  emitIntegrationUpdate();
});
</script>

<style scoped lang="scss">
.v-row {
  margin: 0 !important;
}

h4 {
  font-weight: bold !important;
  padding-bottom: 0px !important;
  margin-bottom: 0px !important;
}

div.row-sections {
  justify-content: space-between;

  > .v-card {
    max-width: 49%;
    border: thin solid rgba(224, 224, 224, 1);
    border-radius: 10px;
    box-shadow: none;

    :deep(.v-card-subtitle),
    :deep(.v-card-text) {
      padding-bottom: 0 !important;
      font-size: 16px !important;
      color: rgba(0, 0, 0, 0.87) !important;
    }

    :deep(.v-card-subtitle) {
      font-weight: bold;
    }

    :deep(.v-card-title) {
      max-width: fit-content !important;
      margin: 0 auto !important;
    }
  }

  & .section-card {
    position: relative;
    padding-top: 30px;
    border-width: 2px;
    overflow: visible;

    & .section-circle {
      width: 54px;
      height: 54px;
      border-radius: 50%;
      text-align: center;
      font-size: 24px;
      font-weight: bold;
      align-content: center;
      color: white;
      position: absolute;
      top: -27px;
      left: 50%;
      transform: translateX(-50%);
      opacity: 1;
    }

    & .focus-section-circle {
      border: 2px solid map.get($blue, "primary");
      color: map.get($blue, "primary");
      background: map.get($blue, "lightest");
    }

    & .inactive-section-circle {
      border: 2px solid rgba(224, 224, 224, 1);
      color: rgba(224, 224, 224, 1);
      background: white;
    }

    & .return-url {
      background-color: rgba(29, 157, 255, 0.1);
      border-radius: 10px;

      & textarea {
        border-width: 0;
      }

      :deep(.v-input__control) {
        padding: 0 12px;
      }

      & *,
      & *::before,
      & *::after {
        border-width: 0 !important;
      }
    }

    :deep(.v-label) {
      color: rgba(0, 0, 0, 0.87);
    }

    :deep(.v-btn.v-btn--disabled) {
      background-color: map.get($blue, "primary") !important;
      opacity: 0.2 !important;
      color: white !important;
    }

    :deep(.v-btn.v-btn--disabled .v-icon) {
      color: white !important;
    }

    :deep(.v-btn__content) {
      color: white;
    }

    :deep(.v-btn:not(.v-btn--active):not(.v-btn--loading):not(:focus):not(:hover) .v-btn__content) {
      color: white !important;
      opacity: 1 !important;
    }

    & .copy-url {
      display: flex;
      flex-direction: row;
      align-items: start;

      & .copied-label {
        height: fit-content;
        vertical-align: middle;
        margin: auto 0;
      }
    }
  }

  & .focus-section {
    border-color: map.get($blue, "primary");
  }

  .v-card > :first-child:not(.v-btn):not(.v-chip) {
    border-top-left-radius: 50%;
    border-top-right-radius: 50%;
  }

  :deep(.v-field) {
    min-height: 56px;
  }

  :deep(.v-label:not(.v-field-label--floating)) {
    top: 50%;
    transform: translateY(-50%);
  }

  :deep(.v-label.v-field-label--floating) {
    top: 10px;
  }
}

// 636px matches this app's existing mobile-table breakpoint (see ComponentTable.vue)
@media (max-width: 636px) {
  div.row-sections {
    flex-direction: column;

    > .v-card {
      max-width: 100%;
    }
  }
}

.error-text {
  color: map.get($red, "base") !important;
}
</style>
