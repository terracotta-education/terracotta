<template>
  <v-col
    class="container-selector"
  >
    <!-- v-col
      class="d-flex flex-column flex-wrap align-baseline"
    -->
      <v-row
        v-for="(_, i) in experimentExposures"
        :key="i"
        class="input-selector mb-5"
      >
        <v-select
          v-model="selected[i]"
          :items="options[i]"
          :label="selectorLabel(i)"
          :menu-props="{bottom: true, offsetY: true}"
          item-text="title"
          item-value="outcomeId"
          @change="onOutcomeSelect(i)"
          clearable
          dense
          hide-details
          hide-selected
          outlined
        >
          <template v-slot:selection="{ item }">
            <span class="truncated">{{ item.title }}</span>
          </template>
        </v-select>
      </v-row>
    <!-- /v-col -->
    </v-col>
</template>

<script>
import { mapGetters, mapActions } from "vuex";

export default {
  name: "Selector",
  data: () => ({
    staticOutcomes: [
      {outcomeId: "AVERAGE_ASSIGNMENT_SCORE", title: "Average assignment score"},
      {outcomeId: "TIME_ON_TASK", title: "Time on task"}
    ],
    selected: [], // user-selected outcomes
    selectedExposureIds: [], // corresponding exposure IDs
    loaded: false
  }),
  computed: {
    ...mapGetters({
        conditions: "experiment/conditions",
        experiment: "experiment/experiment",
        exposures: "exposures/exposures",
        outcomes: "outcome/outcomes"
    }),
    experimentId() {
      return this.experiment.experimentId;
    },
    experimentExposures() {
      return this.orderByTitleAsc(this.exposures) || [];
    },
    experimentOutcomes() {
      return this.outcomes || [];
    },
    options() {
      var opts = [];

      this.experimentExposures.forEach((ee, i) => {
        opts[i] = [];
        opts[i].push(...this.staticOutcomes); // add static outcomes to v-select options
        opts[i].push(...this.experimentOutcomes.filter((eo) => eo.exposureId === ee.exposureId)); // add standard outcomes to v-select options
      });

      return opts;
    }
  },
  watch: {
    outcomes: {
      handler() {
        this.loaded = true;
      }
    }
  },
  methods: {
    ...mapActions({
      getOutcomes: "outcome/fetchOutcomesByExperimentId"
    }),
    selectorLabel(i) {
      return "Exposure Set " + (i + 1);
    },
    onOutcomeSelect(index) {
      // index == the v-select index
      var selections = {
        "outcomeIds": [], // standard outcome IDs
        "alternateId": {
          id: null, // the alt ID
          exposures: [] // the exposure IDs to include
        }
      }

      if (this.staticOutcomes.map((so) => so.outcomeId).includes(this.selected[index])) {
        // an alternate outcome is selected
        // check if every other v-select is null
        var isNewAltId = this.selected.filter((_, i) => i !== index).every((s) => s === null);
        this.selected.forEach((s, i) => {
          // check for other
          if (s === null || i === index) {
            // don't check nulls or same v-select
            return;
          }
          isNewAltId = isNewAltId || !this.staticOutcomes.filter((so) => so.outcomeId === this.selected[index]).some((so) => so.outcomeId === s);
        });
        if (isNewAltId) {
          // this is a new alt ID selection; reset all v-selects
          var selectedTemp = [];
          selections.alternateId.id = this.selected[index];
          for (let i = 0; i < this.options.length; i++) {
            selectedTemp[i] = this.selected[index];
            selections.alternateId.exposures.push(this.selectedExposureIds[i]);
          }
          this.selected = [...selectedTemp];
        } else {
          // check for other v-selects with same alt ID; if so, keep other v-selects as-is
          var hasAltId = false;
          this.selected.forEach((s, i) => {
            hasAltId = hasAltId || (i !== index && this.staticOutcomes.filter((so) => so.outcomeId === this.selected[index]).some((so) => so.outcomeId === s));
          });
          if (hasAltId) {
            selections.alternateId.id = this.selected[index];
            for (let i = 0; i < this.options.length; i++) {
              if (this.selected[i] === null) {
                continue;
              }
              selections.alternateId.exposures.push(this.selectedExposureIds[i]);
            }
          }
        }
      } else if (this.selected[index] == null) {
        // option deleted from v-select; handle other alt ID fields appropriately
        hasAltId = false;
        this.selected.forEach((s) => {
          hasAltId = hasAltId || this.staticOutcomes.some((so) => so.outcomeId === s);
          if (hasAltId && !selections.alternateId.id) {
            selections.alternateId.id = s // set the alt ID if not set previously
          }
        });
        if (hasAltId) {
          for (let i = 0; i < this.selected.length; i++) {
            // include only exposure v-selects w/ non-null values
            if (this.selected[i] === null) {
              continue;
            }
            selections.alternateId.exposures.push(this.selectedExposureIds[i]);
          }
        } else {
          // only standard outcome IDs; add to standard array
          selections.outcomeIds = [...this.selected.filter((s) => s !== null)]; // include only exposure v-selects w/ non-null values
        }
      } else {
        // is a standard outcome ID, de-select all alt ID fields
        for (let i = 0; i < this.selected.length; i++) {
          if (this.staticOutcomes.map((so) => so.outcomeId).includes(this.selected[i])) {
            this.selected[i] = null;
          }
        }
        selections.outcomeIds = [...this.selected.filter((s) => s !== null)]; // include only exposure v-selects w/ non-null values
      }

      if (selections.outcomeIds.length || (selections.alternateId.id && selections.alternateId.exposures.length)) {
        this.$emit("hasSelections", selections);
      } else {
        this.$emit("hasCleared");
      }
    },
    initSelected() {
      this.experimentExposures.forEach(
        (ee, i) => {
          this.selected[i] = null;
          this.selectedExposureIds[i] = ee.exposureId;
        }
      );
    },
    orderByTitleAsc(values) {
      return values.sort(
        function(a, b) {
          if (a.title.toUpperCase() < b.title.toUpperCase()) {
            return -1;
          }
          if (a.title.toUpperCase() > b.title.toUpperCase()) {
            return 1;
          }
          return 0;
        }
      )
    }
  },
  mounted() {
    this.getOutcomes([this.experimentId]);
    this.initSelected();
  }
}
</script>

<style scoped>
div.container-selector {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: fit-content;
  max-width: 100%;
  & div.v-select__selections {
    > div.v-select__selection {
      max-width: 100%;
      min-width: 100%;
    }
  }
  & div.input-selector{
    width: 100%;
    min-width: 100%;
    align-items: center;
    > span.input-selector-label {
      font-weight: 500;
    }
    & .v-input__slot {
      background-color: white;
    }
  }
  & .v-select {
    &.v-input--is-dirty {
      & input {
        display: none;
      }
    }
    & span.truncated {
      min-width: 0;
      max-width: 100%;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}
</style>
