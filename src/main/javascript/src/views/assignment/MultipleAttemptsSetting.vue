<template>
<div
  class="multiple-attempts-setting-container"
>
  <h4>Multiple attempts</h4>
  <p
    class="grey--text text--darken-2 pb-0"
  >
    Decide if students should be able to submit the component more than once.
  </p>
  <v-card
    class="multiple-attempts-card"
    outlined
  >
    <v-card-title
      :class="{ 'blue lighten-5': allowMultipleAttempts }"
    >
      <v-checkbox
        v-model="allowMultipleAttempts"
        class="mt-0"
        label="Allow multiple attempts"
        hide-details
      ></v-checkbox>
    </v-card-title>
    <v-card-text
      v-if="allowMultipleAttempts"
      class="text--primary mx-0 px-5"
    >
      <div
        class="d-flex flex-wrap align-baseline"
      >
        <v-radio-group
            v-model="allowInfiniteSubmissions"
            aria-label="Submission attempts allowed selector"
            column
          >
            <v-radio
              :value="false"
              label="blue"
              color="blue"
              class="mb-5"
            >
              <template
                v-slot:label
              >
                  <div>
                    A student is allowed up to
                    <v-text-field
                      v-model="numOfSubmissions"
                      :required="!allowInfiniteSubmissions"
                      :disabled="allowInfiniteSubmissions"
                      type="number"
                      class="d-inline-block"
                      min="2"
                      style="width:100px"
                      single-line
                      hide-details
                      outlined
                      dense
                    >
                    </v-text-field>
                    attempts
                  </div>
              </template>
            </v-radio>
            <v-radio
              :value="true"
              label="A student is allowed an infinite number of attempts"
              color="blue"
            ></v-radio>
        </v-radio-group>
      </div>
      <div
        class="mb-4"
      >
        <v-divider
          class="mx-0 px-0"
        />
      </div>
      <div
        class="d-flex flex-column flex-wrap align-baseline"
      >
        <div
          class="mb-5"
        >
          Minimum time between submissions:
          <v-text-field
            v-model="hoursBetweenSubmissions"
            type="number"
            class="d-inline-block"
            style="width:100px"
            aria-label="assignment multiple submission minimum time between submissions"
            single-line
            hide-details
            outlined
            dense
          ></v-text-field>
          hours
        </div>
        <div
          class="mb-5"
        >
          Keep the
          <v-select
            v-model="multipleSubmissionScoringScheme"
            :items="scoringOptions"
            class="d-inline-block keep-treatment-score-select"
            item-text="label"
            item-value="value"
            aria-label="assignment multiple submission scoring scheme"
            outlined
            single-line
            hide-details
            dense
            ></v-select>
            treatment score
        </div>
        <div
          v-if="multipleSubmissionScoringScheme === 'CUMULATIVE'"
          class="mb-0"
        >
          <div
            class="mb-4"
          >
            Proportion earned on first attempt:
            <v-text-field
              v-model="cumulativeScoringInitialPercentage"
              type="number"
              class="d-inline-block"
              style="width:100px"
              outlined
              dense
              single-line
              hide-details
            ></v-text-field>
            %
          </div>
          <p
            class="text-caption text--secondary"
          >
            Choose the % the first attempt should be worth.
            The remaining {{ remainingPercentage }}% will be distributed <span v-if="numOfSubmissions > 2">evenly among the other {{ numOfSubmissions - 1 }} attempts ({{ distributionPercentage }}% per attempt).</span>
            <span v-if="numOfSubmissions === 2">to the other attempt.</span>
          </p>
        </div>
      </div>
    </v-card-text>
  </v-card>
</div>
</template>

<script>
import { deleteAttributesFromElement, addAttributesToElement, getAttributeFromElement } from "@/helpers/ui-utils.js";

export default {
  props: {
    value: {
      type: Object,
      required: true
    }
  },
  data: () => ({
    cumulativeOptions: [
      {value: "MOST_RECENT", label: "Most Recent"},
      {value: "HIGHEST", label: "Highest"},
      {value: "AVERAGE", label: "Average"},
      {value: "CUMULATIVE", label: "Cumulative"},
    ]
  }),
  watch: {
    allowInfiniteSubmissions (newValue) {
      if (newValue === true && this.multipleSubmissionScoringScheme === "CUMULATIVE") {
        this.cumulativeScoringInitialPercentage = null;
        this.multipleSubmissionScoringScheme = "MOST_RECENT";
      }
    },
    allowMultipleAttempts: {
      handler(newValue) {
        if (newValue) {
          setTimeout(() => {
            const ariaOwnsId = getAttributeFromElement(".keep-treatment-score-select .v-input__slot:first-of-type", "aria-owns");
            deleteAttributesFromElement(".keep-treatment-score-select .v-input__slot", ["role"]);
            addAttributesToElement(".keep-treatment-score-select .v-input__slot", [
              { name: "role", value: "combobox" },
              { name: "aria-controls", value: ariaOwnsId }
            ]);
          }, 1000);
        }
      }
    }
  },
  computed: {
    remainingPercentage () {
      return 100 - this.cumulativeScoringInitialPercentage;
    },
    distributionPercentage () {
      return ((100 - this.cumulativeScoringInitialPercentage) / (this.numOfSubmissions - 1)).toFixed(2);
    },
    scoringOptions: {
      get() {
        return this.cumulativeOptions.filter(o => o.value === "CUMULATIVE" && this.numOfSubmissions === 0 ? false : true );
      }
    },
    allowMultipleAttempts: {

      get() {
        return this.value.numOfSubmissions === null ? false : true;
      },
      set(value) {
        if (value === true) {
            this.numOfSubmissions = 0;
        } else {
            this.numOfSubmissions = null;
        }
      },
    },
    allowInfiniteSubmissions: {
      get() {
        return this.value.numOfSubmissions > 0 ? false : true;
      },
      set(value) {
        if (value === true) {
            this.numOfSubmissions = 0;
        } else {
            this.numOfSubmissions = 2;
        }
      },
    },
    numOfSubmissions: {
      get() {
        return this.value.numOfSubmissions;
      },
      set(value) {
        this.$emit("input", {
          ...this.value,
          numOfSubmissions: value === null ? null : parseInt(value),
        });
      },
    },
    multipleSubmissionScoringScheme: {
      get() {
        return this.value.multipleSubmissionScoringScheme;
      },
      set(value) {
        this.$emit("input", {
          ...this.value,
          multipleSubmissionScoringScheme: value,
        });
      },
    },
    hoursBetweenSubmissions: {
      get() {
        return this.value.hoursBetweenSubmissions;
      },
      set(value) {
        this.$emit("input", {
          ...this.value,
          hoursBetweenSubmissions: parseFloat(value),
        });
      },
    },
    cumulativeScoringInitialPercentage: {
      get() {
        return this.value.cumulativeScoringInitialPercentage;
      },
      set(value) {
        this.$emit("input", {
          ...this.value,
          cumulativeScoringInitialPercentage: parseFloat(value),
        });
      },
    },
  }
};
</script>

<style lang="scss" scoped>
.multiple-attempts-card .v-card__text {
  font-size: 16px;
  margin-left: 32px;
}
</style>
