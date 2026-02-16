<template>
  <div
    v-show="pageFullyLoaded"
    class="consent-steps my-5 mx-auto"
  >
    <v-row
      class="mb-6 mx-0"
    >
      <div>
        You are being asked to participate in a research study. Please read the statement below, then scroll to the bottom to select your response.
        Your teacher will be able to see whether you submitted a response, but will not be able to see your selection.
      </div>
    </v-row>
    <v-alert
      v-if="respondedAlert.show"
      prominent
      type="info"
    >
      <v-row
        align="center"
      >
        <v-col
          class="grow"
        >
          You responded "{{ respondedAlert.consent }}agree to participate" on {{ respondedAlert.date }}
        </v-col>
      </v-row>
    </v-alert>
    <v-alert
      v-if="alreadyAccessedAlert.show"
      prominent
      type="error"
    >
      <v-row
        align="center"
      >
        <v-col
          class="grow"
        >
          You have already accessed an assignment that is part of this study. At this time, no matter your response to the following question, you cannot be included in this study.
        </v-col>
      </v-row>
    </v-alert>
    <vue-pdf-embed
      v-if="pageFullyLoaded"
      :source="'data:application/pdf;base64,' + pdfFile"
    />
    <form
      @submit.prevent="updateConsent(answer || false)"
    >
      <v-card
        class="mt-5"
      >
        <v-card-title>
          In consideration of the above, will you participate in this research study?
        </v-card-title>
        <v-list
          class="optionList"
        >
          <v-radio-group
            v-model="answer"
            :disabled="disableOptions"
          >
            <v-radio
              v-for="opt in options"
              :key="opt.label"
              :label="opt.label"
              :value="opt.value"
            ></v-radio>
          </v-radio-group>
        </v-list>
      </v-card>
      <v-row
        class="mt-5 submit-row"
      >
        <v-btn
          :disabled="disableSubmit"
          elevation="0"
          color="primary"
          class="mr-4"
          type="submit"
        >
          Submit
        </v-btn>
        <div
          v-if="disableOptions"
          class="please-wait"
        >
          Submitting your consent. Please wait...
        </div>
      </v-row>
    </form>
  </div>
</template>

<script>
import { mapActions } from "vuex";
import moment from "moment";
import VuePdfEmbed from 'vue-pdf-embed/dist/vue2-pdf-embed';

export default {
  name: "StudentConsent",
  props: [
    "experimentId",
    "userId"
  ],
  components: {
    VuePdfEmbed
  },
  data: () => ({
    answer: "",
    options: [
      {
        label: "I agree to participate",
        value: true,
      },
      {
        label: "I do not agree to participate",
        value: false,
      },
    ],
    participant: null,
    pdfFile: null,
    pdfReady: false,
    participantReady: false,
    pageFullyLoaded: false,
    disableSubmit: true,
    disableOptions: false
  }),
  watch: {
    pdfFile() {
      this.pdfReady = true;
      if (this.participantReady) {
        this.pageFullyLoaded = true;
      }
    },
    participant: {
      handler() {
        this.participantReady = true;
        if (this.pdfReady) {
          this.pageFullyLoaded = true;
        }
      },
      deep: true
    },
    pageFullyLoaded() {
      this.$emit('loaded');
    },
    answer: {
      handler(newAnswer) {
        this.disableSubmit = newAnswer === "";
      }
    }
  },
  computed: {
    hasConsentedAlready() {
      return ["CONSENT", "REVOKED"].includes(this.participant.source) && (this.participant.dateGiven !== null || this.participant.dateRevoked !== null);
    },
    alreadyAccessedAlert() {
      return {
        show: this.participant && this.participant.started && !this.participant.consent
      }
    },
    respondedAlert() {
      if (!this.participant) {
        return {
          show: false,
          consent: "",
          date: ""
        }
      }

      return {
        show: this.hasConsentedAlready,
        consent: this.participant.consent ? "" : "do not ",
        date: moment(this.participant.consent ? this.participant.dateGiven : this.participant.dateRevoked).format("MMMM D, YYYY [ at ] h:mma")
      }
    }
  },
  methods: {
    ...mapActions({
      getConsentFile: "consent/getConsentFile",
      updateParticipant: "participants/updateParticipant",
      reportStep: "api/reportStep",
    }),
    updateConsent(answer) {
      if (this.participant.started) {
        // participant has submitted already and cannot be included in the study
        this.$swal({
          text: "You have already accessed an assignment that is part of this study. At this time, no matter your response, you cannot be included in this study.",
          icon: "error",
        });
        return;
      }
      if (answer === "") {
        return;
      }
      if (this.hasConsentedAlready && this.participant.consent === answer) {
        // participant has already submitted consent and is trying to resubmit with same; just show alert and return
        this.$swal({
          text: "Successfully submitted Consent",
          icon: "success",
        });
        return;
      }
      this.disableSubmit = true;
      this.disableOptions = true;
      const updatedParticipant = {
        ...this.participant,
        consent: answer,
      };
      this.submitParticipant(updatedParticipant);
    },
    submitParticipant(participantData) {
      this.updateParticipant({
        experimentId: this.experimentId,
        participantData,
      })
        .then((response) => {
          this.disableSubmit = false;
          this.disableOptions = false;
          this.participant = {...this.participant, ...response};
          this.$swal({
            text: "Successfully submitted consent",
            icon: "success",
          });
          if (response.message) {
            this.$swal({
              text: response.message,
              icon: "error",
            });
          }
        })
        .catch((response) => {
          console.log("submitParticipant | catch", { response });
          this.disableSubmit = false;
          this.disableOptions = false;
        });
    },
    handleConsentFileDownload() {
      this.getConsentFile(this.experimentId)
        .then((file) => {
          this.pdfFile = encodeURI(file);
        });
    }
  },
  async created() {
    this.handleConsentFileDownload();
    const stepResponse = await this.reportStep({
      experimentId: this.experimentId,
      step: "launch_consent_assignment",
    });
    this.participant = stepResponse.data;
  },
};
</script>

<style lang="scss" scoped>
.consent-steps {
  min-height: 100%;
  padding: 30px 45px;
}
.optionList {
  margin-left: 15px;
}
div.vue-pdf-embed {
  margin: 0 auto;
  min-height: fit-content;
  max-height: fit-content;
  overflow-y: scroll;
  box-shadow: 0 3px 1px -2px rgba(0,0,0,.2),0 2px 2px 0 rgba(0,0,0,.14),0 1px 5px 0 rgba(0,0,0,.12);
}
.submit-row {
  margin: 0;
  > .please-wait {
    max-height: fit-content;
    margin: auto 0;
    color: #9e9e9e;
  }
}
</style>
