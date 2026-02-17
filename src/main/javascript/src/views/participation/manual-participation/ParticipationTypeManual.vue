<template>
  <div>
    <h1
      class="my-3"
    >
      Because you would like to
      <strong>manually determine who can participate in the experiment</strong>,
      we've set you up to select students who are enrolled in your class.
    </h1>
    <v-card
      class="pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
      outlined
    >
      <p>
        <strong>Tip:</strong>
        If you are working with minors, we suggest you collect parental consents before proceeding. You can
        <a
          :href="require('@/assets/files/Terracotta_ParentalPermission_template.docx')"
          download="Terracotta_ParentalPermission_template.docx"
        >download a permission template here</a>. <!-- don't format this -->
      </p>
    </v-card>
    <div
      class="mt-5"
    >
      <v-btn
        :to="{ name: 'ParticipationTypeManualSelection' }"
        elevation="0"
        color="primary"
      >
        Continue
      </v-btn>
      <router-link
        :to="{ name: nextPage('ParticipationDistribution') }"
        class="plain-link ml-3"
      >
        Skip participant selection for now
      </router-link>
    </div>
  </div>
</template>

<script>
import { mapGetters } from "vuex";

export default {
  name: "ParticipationTypeManual",
  props: {
    experiment: {
      type: Object,
      required: true
    }
  },
  computed: {
    ...mapGetters({
      editMode: "navigation/editMode"
    }),
    saveExitPage() {
      return this.editMode?.callerPage?.name || "Home";
    },
    conditions() {
      return this.experiment.conditions;
    },
    singleConditionExperiment() {
      return this.conditions.length === 1;
    }
  },
  methods: {
    nextPage(toPage) {
      if (this.singleConditionExperiment) {
        return "ParticipationSummary";
      }

      return toPage;
    },
    saveExit() {
      this.$router.push({
        name: this.saveExitPage,
        params: {
          experiment: this.experiment.experimentId
        }
      })
    }
  }
};
</script>
