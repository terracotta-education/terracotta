<template>
  <div class="consent-steps my-5 mx-auto">
    <div>
      <h2>
        For this assignment, you need to review and accept the consent.
      </h2>
      <button class="consentLink mt-2" @click="openPDF">
        Review the Consent
      </button>
    </div>
    <form @submit.prevent="updateConsent(answer || false)">
      <v-card class="mt-5">
        <v-card-title>Do you accept the consent?</v-card-title>
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
        Next
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
        label: "Yes",
        value: true,
      },
      {
        label: "No",
        value: false,
      },
    ],
  }),

  computed: {
    ...mapGetters({
      consent: "consent/consent",
      participants: "participants/participants",
    }),
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
        const updatedParticipant = this.participants.filter(
          (participant) => participant.user.userKey === this.userId
        )[0];
        updatedParticipant.consent = answer;
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
          } else {
            this.$swal(response.error);
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
    await this.getConsentFile(this.experimentId);
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
