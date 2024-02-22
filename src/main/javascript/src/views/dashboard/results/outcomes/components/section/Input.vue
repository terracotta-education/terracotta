<template>
  <div
    class="container-input"
  >
    <v-row
      class="description"
    >
      <h3>Select Outcomes</h3>
      <InfoTooltip
        :header="`What is an outcome?`"
        :message="`An outcome (also known as a dependent variable) is a variable that may be affected by the experimental manipulation.`"
        :activator="tooltipActivator"
        :linkClass="`tooltip-outcome`"
      />
      <span>{{ experimentDetailsText }}</span>
      <span>
        {{ selectOutcomesText }} Outcomes are
        <a
          @click="handleStatusPageNav()"
        >
          added on the status page.
        </a>
      </span>
      <span>
        <Selector
          @hasCleared="handleClearedSelection"
          @hasSelections="handleGetOutcomes"
        />
      </span>
    </v-row>
  </div>
</template>

<script>
import { mapGetters, mapActions } from "vuex"
import { EventBus } from "@/helpers/event-bus"
import InfoTooltip from "../../../elements/InfoTooltip.vue"
import Selector from "./subsection/input/Selector.vue"

export default {
  name: "Input",
  components: {
    InfoTooltip,
    Selector
  },
  data: () => ({
    hasSelectedOption: false
  }),
  computed: {
    ...mapGetters({
      experiment: "experiment/experiment",
      conditions: "experiment/conditions",
      exposures: "exposures/exposures",
      outcomes: "resultsDashboard/outcomes"
    }),
    experimentId() {
      return this.experiment.experimentId;
    },
    experimentConditions() {
      return this.conditions || [];
    },
    experimentExposures() {
      return this.exposures || [];
    },
    experimentConditionCount() {
      return this.experimentConditions.length;
    },
    experimentExposureCount() {
      return this.experimentExposures.length;
    },
    resultsOutcomes() {
      return this.outcomes;
    },
    tooltipActivator() {
      return {"type": "link", "text": "What is an outcome?"};
    },
    experimentDetailsText() {
      let text = "Your experiment has " + this.experimentConditionCount + " conditions";
      if (this.experimentExposureCount > 1) {
        text += " and " + this.experimentExposureCount + " exposure sets";
      }
      return text + ".";
    },
    selectOutcomesText() {
      let text = "Select the outcomes you want to compare between conditions";
      if (this.experimentExposureCount > 1) {
        text += "/exposure sets";
      }
      return text + "."
    }
  },
  watch: {
    resultsOutcomes: {
      handler(newValue) {
        this.loaded = newValue != null;
      }
    },
    hasSelectedOption: {
      handler(newValue) {
        this.$emit("hasSelection", newValue);
      }
    }
  },
  methods: {
    ...mapActions({
      clearOutcomes: "resultsDashboard/clearOutcomes",
      getOutcomes: "resultsDashboard/getOutcomes",
      saveEditMode: "navigation/saveEditMode"
    }),
    async handleGetOutcomes(outcomes) {
      this.hasSelectedOption = true;
      // outcomes = {[outcomeId,...], alternateIds: {id: string, exposures: [exposureId,...]}}
      console.log("getOutcomes called: " + JSON.stringify(outcomes));
      await this.getOutcomes([
        this.experimentId,
        outcomes
      ]);
    },
    handleClearedSelection() {
      this.clearOutcomes();
      this.hasSelectedOption = false;
    },
    handleStatusPageNav() {
      EventBus.$emit("statusPageNav");
    },
  }
}
</script>

<style scoped>
div.container-input {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  > .row {
    margin: 0 !important;
    &.description {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      & > * {
        padding: 8px;
        max-width: 100%;
      }
      > h3 {
        font-weight: bold;
      }
    }
  }
  & a.tooltip-outcome {
    text-decoration: none;
    border-bottom:1px dotted;
  }
}
</style>
