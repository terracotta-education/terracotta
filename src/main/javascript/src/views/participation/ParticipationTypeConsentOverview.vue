<template>
    <div>
        <h1
          class="mb-5"
        >
          Because you would like <strong>students to be invited to agree to participate in study within {{ lmsTitle }}</strong>, we'll need to create a consent assignment.
        </h1>
        <v-btn
          @click="setConsent"
          elevation="0"
          color="primary"
          class="mb-4"
        >
          Continue
        </v-btn>
    </div>
</template>

<script>
import { mapGetters } from "vuex";

export default {
  name: "ParticipationTypeConsentOverview",
  props: {
    experiment: {
      type: Object,
      required: true
    }
  },
  computed: {
    ...mapGetters({
      consent: "consent/consent",
      editMode: "navigation/editMode",
      configurations: "configuration/get"
    }),
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || "Home";
    },
    lmsTitle() {
      return this.configurations?.lmsTitle || "LMS";
    }
  },
  methods: {
    setConsent() {
      this.$router.push(
        {
          name: "ParticipationTypeConsentTitle",
          params: {
            experiment: this.experiment.experimentId
          }
        }
      );
    },
    saveExit() {
      this.$router.push({
        name: this.getSaveExitPage,
        params: {
          experiment: this.experiment.experimentId
        }
      });
    }
  }
}
</script>