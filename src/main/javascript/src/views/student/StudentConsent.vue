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
    <iframe
      :src="
        'data:application/pdf;base64, ' +
          encodeURI(this.consent.file) +
          // pagemode=none hides thumbnails on Firefox, toolbar=0 works for other browsers
          '#pagemode=none&toolbar=0'
      "
      title="Consent PDF"
    ></iframe>
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
    participant: null,
  }),

  computed: {
    ...mapGetters({
      consent: "consent/consent",
    }),
  },
  methods: {
    ...mapActions({
      getConsentFile: "consent/getConsentFile",
      updateParticipant: "participants/updateParticipant",
      reportStep: "api/reportStep",
    }),
    updateConsent(answer) {
      console.log(answer);
      if (answer !== "") {
        // Update a clone of this participant object
        const updatedParticipant = {
          ...this.participant,
          consent: answer,
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
              icon: "error",
            });
          }
        })
        .catch((response) => {
          console.log("submitParticipant | catch", { response });
        });
    },
  },
  async created() {
    this.getConsentFile(this.experimentId);
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
iframe {
  width: 100%;
  min-height: 300px;
  border: none;
}
</style>
