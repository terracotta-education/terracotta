<template>
  <v-app
    v-if="preview || isError"
    class="app"
  >
    <v-main>
      <v-container>
        <v-row>
          <v-col>
            <v-img
              class="mx-auto mb-10"
              src="@/assets/terracotta_logo.svg"
              alt="Terracotta Logo"
              max-width="173"
            />

            <v-card
              v-if="preview && isSuccess"
              class="first-party-card mx-auto"
              max-width="700"
              min-width="fit-content"
            >
              <div class="pt-5">
                <div class="icon-circle icon-circle-success">
                  <v-icon>
                    mdi-check
                  </v-icon>
                </div>
              </div>

              <div>
                <v-card-title>
                  Successfully returned to Terracotta following preview
                </v-card-title>

                <v-card-text class="first-party-card__text">
                  <div>
                    <b>Preview Launch Token:</b>
                    {{ launchToken }}
                  </div>

                  <div>
                    <b>Preview Score Received:</b>
                    {{ score }}
                  </div>
                </v-card-text>
              </div>
            </v-card>

            <v-card
              v-else-if="preview && isInvalidScore"
              class="first-party-card mx-auto"
              max-width="700"
            >
              <div class="pt-5">
                <div class="icon-circle icon-circle-invalid">
                  <v-icon>
                    mdi-exclamation-thick
                  </v-icon>
                </div>
              </div>

              <div>
                <v-card-title>
                  {{ invalidScore.title }}
                </v-card-title>

                <v-card-text class="first-party-card__text">
                  <div
                    v-html="invalidScore.info"
                    class="mb-8"
                  />

                  <div>
                    <b>URL received:</b>
                    {{ url }}
                  </div>
                </v-card-text>
              </div>
            </v-card>

            <v-card
              v-else
              class="first-party-card mx-auto"
              max-width="700"
            >
              <div class="pt-5">
                <div class="icon-circle icon-circle-invalid">
                  <v-icon>
                    mdi-exclamation-thick
                  </v-icon>
                </div>
              </div>

              <div>
                <v-card-title>
                  {{ error.title }}
                </v-card-title>

                <v-card-text class="first-party-card__text">
                  <div
                    v-html="error.info[0]"
                    class="mb-4"
                  />

                  <div v-if="error.info.length > 1">
                    <v-btn
                      v-if="moreAttemptsAvailable"
                      class="mb-4"
                      color="primary"
                      @click="handleReattemptAssignment"
                    >
                      Reattempt assignment
                    </v-btn>

                    <div v-html="error.info[1]" />
                  </div>

                  <div v-if="preview">
                    <b>URL received:</b>
                    {{ url }}
                  </div>
                </v-card-text>
              </div>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup>
import {
  computed,
  onMounted
} from "vue";

import dayjs from "@/plugins/dayjs";

defineOptions({
  name: "IntegrationResult"
});

const props = defineProps({
  integrationData: {
    type: Object,
    required: true
  }
});

const preview = computed(() => {
  return props.integrationData.preview;
});

const client = computed(() => {
  return props.integrationData.client;
});

const launchToken = computed(() => {
  return props.integrationData.launchToken;
});

const score = computed(() => {
  return props.integrationData.score;
});

const status = computed(() => {
  return props.integrationData.status;
});

const url = computed(() => {
  return props.integrationData.url;
});

const errorCode = computed(() => {
  return props.integrationData.errorCode;
});

const moreAttemptsAvailable = computed(() => {
  return props.integrationData.moreAttemptsAvailable;
});

const isError = computed(() => {
  return status.value !== "OK";
});

const isCustomWebActivity = computed(() => {
  return client.value === "Custom Web Activity";
});

const errorMessage = computed(() => {
  const rawMessage = props.integrationData.errorMessage;

  if (!rawMessage) {
    return "";
  }

  const timestamp = rawMessage.split("::")[1];
  const time = dayjs(Number(timestamp)).format(
    "MMMM DD [at] h:mm A"
  );

  if (rawMessage.startsWith("Error Locked::")) {
    return `The assignment was locked ${time}.`;
  }

  if (rawMessage.startsWith("Error Unlocked::")) {
    return `The assignment is locked until ${time}.`;
  }

  return "";
});

const isInvalidScore = computed(() => {
  return score.value === null || Number.isNaN(Number(score.value));
});

const isSuccess = computed(() => {
  return (
    !isError.value &&
    launchToken.value !== null &&
    !isInvalidScore.value
  );
});

const customWebActivityGuideUrl =
  "https://terracotta-education.atlassian.net/wiki/spaces/TC/pages/336330757/Terracotta+Custom+Web+Activity+Integration+Guide";

const qualtricsGuideUrl =
  "https://terracotta-education.atlassian.net/wiki/spaces/TC/pages/336265230/Terracotta+Qualtrics+Integration+Guide";

const invalidScore = computed(() => {
  if (isCustomWebActivity.value) {
    return {
      title:
        "Returned to Terracotta following custom web activity preview with invalid or missing score",
      info: `
        You are seeing this screen because a custom web activity preview returned to Terracotta without a valid score
        parameter. For detailed instructions on how to configure your custom web activity,
        <a href="${customWebActivityGuideUrl}" target="_blank">click here</a>.
        If the score parameter is omitted, Terracotta assumes that the student should receive the maximum score for their submission.
      `
    };
  }

  return {
    title:
      `Returned to Terracotta following ${client.value} preview with invalid or missing score`,
    info: `
      You are seeing this screen because a ${client.value} preview returned to Terracotta without a valid score parameter. For
      detailed instructions on how to configure your Qualtrics survey,
      <a href="${qualtricsGuideUrl}" target="_blank">click here</a>.
      If the score parameter is omitted, Terracotta assumes that the student should receive the maximum score for their submission.
    `
  };
});

const previewError = computed(() => {
  if (isCustomWebActivity.value) {
    return {
      title: "Invalid submission token",
      info: [
        `
          You are seeing this screen because a custom web activity preview returned to Terracotta without a valid score
          parameter. For detailed instructions on how to configure your custom web activity,
          <a href="${customWebActivityGuideUrl}" target="_blank">click here</a>.
          If the score parameter is omitted, Terracotta assumes that the student should receive the maximum score for their submission.
        `
      ]
    };
  }

  return {
    title: "Invalid submission token",
    info: [
      `
        You are seeing this screen because a ${client.value} preview returned to Terracotta without a valid score parameter. For
        detailed instructions on how to configure your Qualtrics survey,
        <a href="${qualtricsGuideUrl}" target="_blank">click here</a>.
        If the score parameter is omitted, Terracotta assumes that the student should receive the maximum score for their submission.
      `
    ]
  };
});

const expiredSessionInfo = computed(() => {
  if (moreAttemptsAvailable.value) {
    return `
      You are seeing this screen because an error occurred while attempting to record an assignment submission from a web activity.<br /><br />
      If you are a student, you're seeing this error because your session has expired. Click the button below to try again.
    `;
  }

  return `
    You are seeing this screen because an error occurred while attempting to record an assignment submission from a web activity.<br /><br />
    If you are a student, you're seeing this error because your session has expired.<br /><br />

    ${errorMessage.value}
  `;
});

const instructorInfo = computed(() => {
  return `
    If you are an instructor, please revisit documentation on integrating your survey or web activity.
    If the issue continues, contact <a href="mailto:support@terracotta.education">support@terracotta.education</a> and reference the error code: ${errorCode.value}
  `;
});

const submissionError = computed(() => {
  return {
    title: "Invalid submission attempt",
    info: [
      expiredSessionInfo.value,
      instructorInfo.value
    ]
  };
});

const error = computed(() => {
  return preview.value
    ? previewError.value
    : submissionError.value;
});

const dispatchIntegrationEvent = eventName => {
  const event = new CustomEvent(eventName, {
    detail: {
      integrationData: props.integrationData
    }
  });

  window.parent.document.dispatchEvent(event);
};

const handleReattemptAssignment = () => {
  dispatchIntegrationEvent("integrations_reattempt");
};

onMounted(() => {
  if (preview.value || isError.value) {
    return;
  }

  dispatchIntegrationEvent("integrations_score");
});
</script>

<style lang="scss" scoped>
.app {
  background-color: rgba(253, 245, 242, 1) !important;
  padding-top: 80px;

  & .first-party-card {
    padding: 32px;
    display: flex;
    justify-content: space-between;
    border-radius: 10px;

    & .first-party-card__text {
      color: rgba(0, 0, 0, 0.87) !important;
    }

    & .icon-circle {
      width: 54px;
      height: 54px;
      border-radius: 50%;
      text-align: center;
      font-size: 24px;
      font-weight: bold;
      align-content: center;
      opacity: 1;

      &.icon-circle-success {
        border: 2px solid rgba(56, 173, 182, 1);
        background-color: rgba(56, 173, 182, 0.2);
        color: rgba(56, 173, 182, 1);

        > .v-icon {
          color: rgba(56, 173, 182, 1);
        }
      }

      &.icon-circle-invalid {
        border: 2px solid map.get($red, "base");
        background-color: rgba(229, 21, 62, 0.2);
        color: map.get($red, "base");

        > .v-icon {
          color: map.get($red, "base");
        }
      }
    }

    & .v-card-title {
      font-size: 28px;
      font-weight: unset;
    }

    & .v-card-text {
      font-size: 16px;
    }
  }
}
</style>
