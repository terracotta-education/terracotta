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
            >
              <div
                class="pt-5"
              >
                <div
                  class="icon-circle icon-circle-success"
                >
                  <v-icon>
                    mdi-check
                  </v-icon>
                </div>
              </div>
              <div>
                <v-card-title>
                  Successfully returned to Terracotta following preview
                </v-card-title>
                <v-card-text
                  class="first-party-card__text"
                >
                  <div>
                    <b>Preview Launch Token:</b> {{ launchToken }}
                  </div>
                  <div>
                    <b>Preview Score Received:</b> {{ score }}
                  </div>
                </v-card-text>
              </div>
            </v-card>
            <v-card
              v-else-if="preview && isInvalidScore"
              class="first-party-card mx-auto"
              max-width="700"
            >
              <div
                class="pt-5"
              >
                <div
                  class="icon-circle icon-circle-invalid"
                >
                  <v-icon>
                    mdi-exclamation-thick
                  </v-icon>
                </div>
              </div>
              <div>
                <v-card-title>
                  {{ invalidScore.title }}
                </v-card-title>
                <v-card-text
                  class="first-party-card__text"
                >
                  <div
                    v-html="invalidScore.info"
                    class="mb-8"
                  ></div>
                  <div>
                    <b>URL received:</b> {{ url }}
                  </div>
                </v-card-text>
              </div>
            </v-card>
            <v-card
              v-else
              class="first-party-card mx-auto"
              max-width="700"
            >
              <div
                class="pt-5"
              >
                <div
                  class="icon-circle icon-circle-invalid"
                >
                  <v-icon>
                    mdi-exclamation-thick
                  </v-icon>
                </div>
              </div>
              <div>
                <v-card-title>
                  {{ error.title }}
                </v-card-title>
                <v-card-text
                  class="first-party-card__text"
                >
                  <div
                    v-html="error.info[0]"
                    class="mb-4"
                  ></div>
                  <div
                    v-if="error.info.length > 1"
                  >
                    <v-btn
                      v-if="moreAttemptsAvailable"
                      class="mb-4"
                      color="primary"
                      @click="handleReattemptAssignment"
                    >
                      Reattempt assignment
                    </v-btn>
                    <div
                      v-html="error.info[1]"
                    ></div>
                  </div>
                  <div
                    v-if="preview"
                  >
                    <b>URL received:</b> {{ url }}
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

<script>
export default {
  props: {
    integrationData:{
      type: Object,
      required: true
    }
  },
  computed: {
    preview() {
      return this.integrationData.preview;
    },
    client() {
      return this.integrationData.client;
    },
    launchToken() {
      return this.integrationData.launchToken;
    },
    score() {
      return this.integrationData.score;
    },
    status() {
      return this.integrationData.status;
    },
    url() {
      return this.integrationData.url;
    },
    errorCode() {
      return this.integrationData.errorCode;
    },
    isError() {
      return this.status !== "OK";
    },
    moreAttemptsAvailable() {
      return this.integrationData.moreAttemptsAvailable;
    },
    isSuccess() {
      if (this.isError) {
        return false;
      }

      if (this.launchToken === null) {
        return false;
      }

      if (this.isInvalidScore) {
        return false;
      }

      return true;
    },
    isInvalidScore() {
      return this.score === null || isNaN(this.score);
    },
    invalidScore() {
      switch(this.client) {
        case "Custom Web Activity":
          return {
            title: "Returned to Terracotta following custom web activity preview with invalid or missing score",
            info: `
              You are seeing this screen because a custom web activity preview returned to Terracotta without a valid score
              parameter. For detailed instructions on how to configure your custom web activity,
              <a href="https://terracotta-education.atlassian.net/wiki/spaces/TC/pages/336330757/Terracotta+Custom+Web+Activity+Integration+Guide" target="_blank">click here</a>.
              If the score parameter is omitted, Terracotta assumes that the student should receive the maximum score for their submission.
            `
          }
        default:
          return {
            title: `
              Returned to Terracotta following ${this.client} preview with invalid or missing score
            `,
            info: `
              You are seeing this screen because a ${this.client} preview returned to Terracotta without a valid score parameter. For
              detailed instructions on how to configure your Qualtrics survey,
              <a href="https://terracotta-education.atlassian.net/wiki/spaces/TC/pages/336265230/Terracotta+Qualtrics+Integration+Guide" target="_blank">click here</a>.
              If the score parameter is omitted, Terracotta assumes that the student should receive the maximum score for their submission.
            `
          }
      }
    },
    error() {
      if (this.preview) {
        switch(this.client) {
          case "Custom Web Activity":
            return {
              title: "Invalid submission token",
              info: [
                `
                  You are seeing this screen because a custom web activity preview returned to Terracotta without a valid score
                  parameter. For detailed instructions on how to configure your custom web activity,
                  <a href="https://terracotta-education.atlassian.net/wiki/spaces/TC/pages/336330757/Terracotta+Custom+Web+Activity+Integration+Guide" target="_blank">click here</a>.
                  If the score parameter is omitted, Terracotta assumes that the student should receive the maximum score for their submission.
                `
              ]
            }
          default:
            return {
              title: "Invalid submission token",
              info: [
                `
                  You are seeing this screen because a ${this.client} preview returned to Terracotta without a valid score parameter. For
                  detailed instructions on how to configure your Qualtrics survey,
                  <a href="https://terracotta-education.atlassian.net/wiki/spaces/TC/pages/336265230/Terracotta+Qualtrics+Integration+Guide" target="_blank">click here</a>.
                  If the score parameter is omitted, Terracotta assumes that the student should receive the maximum score for their submission.
                `
              ]
            }
        }
      } else {
        if (this.moreAttemptsAvailable) {
          return {
            title: "Invalid submission attempt",
            info: [
              `
                You are seeing this screen because an error occurred while attempting to record an assignment submission from a web activity.<br /><br />
                If you are a student, you're seeing this error because your session has expired. Click the button below to try again.
              `,
              `
                If you are an instructor, please revisit documentation on integrating your survey or web activity.
                If the issue continues, contact <a href="mailto:support@terracotta.education">support@terracotta.education</a> and reference the error code: ${this.errorCode}
              `
            ]
          }
        } else {
          return {
            title: "Invalid submission attempt",
            info: [
              `
                You are seeing this screen because an error occurred while attempting to record an assignment submission from a web activity.<br /><br />
                If you are a student, you're seeing this error because your session has expired.
              `,
              `
                If you are an instructor, please revisit documentation on integrating your survey or web activity.
                If the issue continues, contact <a href="mailto:support@terracotta.education">support@terracotta.education</a> and reference the error code: ${this.errorCode}
              `
            ]
          }
        }
      }
    },
  },
  methods: {
    handleReattemptAssignment() {
      var event = new CustomEvent(
        "integrations_reattempt",
        {
          detail: {
            integrationData: this.integrationData
          }
        }
      );
      window.parent.document.dispatchEvent(event);
    }
  },
  mounted() {
    if (this.preview || this.isError) {
      // is an instructor preview or contains an error; don't send iframe message
      return;
    }

    // is a student response; send message to parent window with integration data
    var event = new CustomEvent(
      "integrations_score",
      {
        detail: {
          integrationData: this.integrationData
        }
      }
    );
    window.parent.document.dispatchEvent(event);
  }
}
</script>

<style lang="scss" scoped>
  @import "../../styles/custom";

  .app {
    background-color: rgba(253, 245, 242, 1) !important;
    padding-top: 80px;
  }
  .first-party-card {
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
        background-color: rgba(56, 173, 182, .2);
        color:rgba(56, 173, 182, 1);
        > .v-icon {
          color:rgba(56, 173, 182, 1);
        }
      }
      &.icon-circle-invalid {
        border: 2px solid rgba(229, 21, 62, 1);
        background-color: rgba(229, 21, 62, .2);
        color:rgba(229, 21, 62, 1);
        > .v-icon {
          color:rgba(229, 21, 62, 1);
        }
      }
    }
    & .v-card__title {
      flex-wrap: nowrap;
      font-size: 28px;
      font-weight: unset;
    }
    & .v-card__text {
      font-size: 16px;
    }
  }
</style>
