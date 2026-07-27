<template>
  <div>
    <h1
      class="mb-5"
    >
      Select the percent of students you would like to receive each condition
    </h1>
    <div
      class="row mx-2"
    >
      <div
        class="col-9 label"
      >
        Condition
      </div>
      <div
        class="col-3 label text-right"
      >
        Distribution
      </div>
    </div>
    <v-card
      class="mt-2 mb-3 py-3 mx-auto lighten-5 rounded-lg"
      outlined
    >
      <v-card-text
        v-for="(condition, index) in conditions"
        :key="condition.conditionId"
        class="pa-5"
      >
        <v-row
          class="justify-space-between align-center"
        >
          <v-col
            cols="9"
            class="py-0"
          >
            <v-card-title
              class="ma-0 pa-0 body-1"
            >
              {{ condition.name }} will receive
            </v-card-title>
          </v-col>
          <v-col
            cols="3"
            class="py-0"
          >
            <v-text-field
              v-model="distributionValue[index]"
              :error="touched && !isValidAt(index)"
              :aria-label="`Input distribution percentage for ${condition.name}`"
              class="pa-0 ma-0 text-right"
              suffix="%"
              inputmode="decimal"
              hide-details
              outlined
              required
              @input="touched = true"
            ></v-text-field>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>
    <p
      v-if="touched && isDisabled"
      class="errorMessage mt-3"
    >
      Enter a percentage of zero or greater for each condition. The values must add up to 100%
      &mdash; they currently add up to {{ displayTotal }}%.
    </p>
    <v-btn
      :disabled="isDisabled"
      @click="updateDistribution('ParticipationSummary')"
      elevation="0"
      class="mt-3"
      color="primary"
    >
      Continue
    </v-btn>
  </div>
</template>

<script>
import { mapActions, mapGetters } from "vuex";

export default {
  name: "ParticipationCustomDistribution",
  props: {
    experiment: {
      type: Object,
      required: true
    }
  },
  // NOTE: must be a regular function, not an arrow function. An arrow function
  // does not receive the component instance as `this`, which is what caused
  // "Cannot read properties of undefined (reading 'experiment')".
  data() {
    return {
      distributionValue: this.seedDistribution(this.experiment.conditions),
      touched: false
    };
  },
  computed: {
    ...mapGetters({
      editMode: "navigation/editMode"
    }),
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || "Home";
    },
    conditions() {
      return this.experiment.conditions || [];
    },
    experimentId() {
      return this.experiment.experimentId;
    },
    /**
     * Parsed inputs. Blank/garbage entries become NaN rather than silently
     * coercing to 0 (`Number("")` is 0) or being truncated (`parseInt("50.5")`
     * is 50, `parseInt("50abc")` is 50).
     */
    numericValues() {
      return this.distributionValue.map((value) => {
        const trimmed = String(value ?? "").trim();
        return trimmed === "" ? NaN : Number(trimmed);
      });
    },
    totalDistribution() {
      return this.numericValues.reduce(
        (acc, curr) => acc + (Number.isFinite(curr) ? curr : 0),
        0
      );
    },
    /** Rounded to 2dp so float sums like 99.99999999999999 still pass. */
    roundedTotal() {
      return Math.round(this.totalDistribution * 100) / 100;
    },
    displayTotal() {
      return Number.isInteger(this.roundedTotal)
        ? this.roundedTotal
        : this.roundedTotal.toFixed(2);
    },
    isDisabled() {
      return (
        !this.conditions.length ||
        this.numericValues.some((_, index) => !this.isValidAt(index)) ||
        this.roundedTotal !== 100
      );
    }
  },
  watch: {
    // Re-seed if the parent swaps in a freshly fetched experiment after mount.
    // Not deep, so this only fires when the conditions array itself changes.
    "experiment.conditions": function (conditions) {
      this.distributionValue = this.seedDistribution(conditions);
      this.touched = false;
    }
  },
  methods: {
    ...mapActions({
      updateConditions: "condition/updateConditions"
    }),
    seedDistribution(conditions) {
      return (conditions || []).map((condition) =>
        condition.distributionPct ?? ""
      );
    },
    isValidAt(index) {
      const value = this.numericValues[index];
      return Number.isFinite(value) && value >= 0;
    },
    updateDistribution(path) {
      const updatedConditions = this.conditions.map((condition, index) => {
        return {
          ...condition,
          distributionPct: this.numericValues[index],
          experimentId: this.experimentId
        };
      });

      this.updateConditions(updatedConditions)
        .then((response) => {
          if (response?.status === 200) {
            this.$router.push({
              name: path,
              params: { experiment: this.experimentId }
            });
          } else {
            this.$swal(response?.error || "Could not save the distribution.");
          }
        })
        .catch((error) => {
          console.error("updateConditions | catch", { error });
          this.$swal("Could not save the distribution. Please try again.");
        });
    },
    saveExit() {
      if (this.isDisabled) {
        this.$router.push({
          name: this.getSaveExitPage,
          params: {
            experiment: this.experimentId
          }
        });
      } else {
        this.updateDistribution(this.getSaveExitPage);
      }
    }
  }
};
</script>

<style lang="scss" scoped>
@import "~@/styles/variables";

.label {
  font-weight: 500;
  font-size: 12px;
  line-height: 16px;
  letter-spacing: 1.25px;
  text-transform: uppercase;
  color: #5f6368;
}

// These target Vuetify's internal markup, which lives inside a child
// component, so `scoped` blocks them without a deep selector.
// Vue 2: ::v-deep — Vue 3: :deep(...)
::v-deep .v-input__slot {
  margin: 0;
}

::v-deep .v-text-field__details {
  display: none;
}

::v-deep .text-right input {
  text-align: right;
}

.errorMessage {
  color: map-get($red, "base");
}
</style>
