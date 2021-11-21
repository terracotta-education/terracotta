<template>
  <div class="consent-steps my-5 mx-auto">
    <v-alert
      v-if="participant && participant.started && !participant.consent"
      prominent
      type="error"
    >
      <v-row align="center">
        <v-col class="grow">
          You have already accessed an assignment that is part of this study. At
          this time, no matter your response to the following question, you
          cannot be included in this study.
        </v-col>
      </v-row>
    </v-alert>
    <div>
      <button class="consentLink mt-2" @click="openPDF">
        Review the Consent
      </button>
    </div>
    <form @submit.prevent="updateConsent(answer || false)">
      <v-card class="mt-5">
        <v-card-title
          >In the consideration of the above, will you participate in this
          research study?</v-card-title
        >
        <v-list class="optionList">
          <v-radio-group v-model="answer">
            <v-radio
              v-for="opt in options"
              :key="opt.label"
              :label="opt.label"
              :value="opt.value"
            ></v-radio>
          </v-radio-group>
        </v-list>
      </v-card>
      <v-btn elevation="0" color="primary" class="mr-4 mt-5" type="submit">
        Submit
      </v-btn>
    </form>
  </div>
</template>

<script>
import { mapGetters, mapActions } from "vuex";

export default {
  name: "StudentConsent",
  props: ["experimentId", "userId"],
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
  }),

  computed: {
    ...mapGetters({
      consent: "consent/consent",
      participants: "participants/participants",
    }),
    participant() {
      const filteredParticipants =
        this.participants && Array.isArray(this.participants)
          ? this.participants.filter(
              (participant) => participant.user.userKey === this.userId
            )
          : [];
      return filteredParticipants.length === 1 ? filteredParticipants[0] : null;
    },
  },
  methods: {
    ...mapActions({
      getConsentFile: "consent/getConsentFile",
      getParticipants: "participants/fetchParticipants",
      updateParticipant: "participants/updateParticipant",
    }),
    updateConsent(answer) {
      console.log(answer);
      if (answer !== "") {
        // Update a clone of this participant object
        const updatedParticipant = {
          ...this.participant,
          consent: answer
        };
        this.submitParticipant(updatedParticipant);
      }
    },
    submitParticipant(participantData) {
      this.updateParticipant({
        experimentId: this.experimentId,
        participantData,
      })
        .then((response) => {
          if (response.status === 200) {
            this.$swal({
              text: `Successfully submitted Consent`,
              icon: "success",
            });
          } else if (response.message) {
            this.$swal({
              text: response.message, 
              icon: 'error'
            });
          }
        })
        .catch((response) => {
          console.log("submitParticipant | catch", { response });
        });
    },
    openPDF() {
      // Second Parameter intentionally left blank
      let pdfWindow = window.open("", "", "_blank");
      pdfWindow.opener = null;
      pdfWindow.document.write(
        "<iframe width='100%' height='100%' src='data:application/pdf;base64, " +
          encodeURI(this.consent.file) +
          "'></iframe>"
      );

      return false;
    },
  },
  async created() {
    this.getConsentFile(this.experimentId);
    await this.getParticipants(this.experimentId);
  },
};
</script>

<style lang="scss" scopoed>
@import "~vuetify/src/styles/main.sass";
@import "~@/styles/variables";

.consent-steps {
  min-height: 100%;
  grid-template-rows: auto 1fr;
  grid-template-columns: auto 1fr;
  grid-template-areas:
    "aside nav"
    "aside article";
  padding: 30px 45px;
}

.optionList {
  margin-left: 15px;
}
.consentLink {
  background: none !important;
  border: none;
  padding: 0 !important;
  color: #069 !important;
  cursor: pointer;
}
</style>
