<template>
  <div class="multiple-attempts-setting-container">
    <h4>Multiple attempts</h4>

    <p class="text-medium-emphasis pb-0">
      Decide if students should be able to submit the component more than once.
    </p>

    <v-card
      class="multiple-attempts-card"
      variant="outlined"
    >
      <v-card-title
        :class="{ 'bg-blue-lighten-5': allowMultipleAttempts }"
      >
        <v-checkbox
          v-model="allowMultipleAttempts"
          class="mt-0"
          label="Allow multiple attempts"
          hide-details
        />
      </v-card-title>

      <v-card-text
        v-if="allowMultipleAttempts"
        class="mt-2 mx-0 px-5"
      >
        <div class="d-flex flex-wrap align-baseline">
          <v-radio-group
            v-model="allowInfiniteSubmissions"
            aria-label="Submission attempts allowed selector"
          >
            <!-- color="blue" resolves to Vuetify's own Material Design blue
                 (#2196f3), not this app's blue ($blue.base, #00599e) - neither
                 branch's theme config customizes "blue" to redirect it, so
                 the literal hex is needed here to match development's actual
                 look. -->
            <v-radio
              :value="false"
              color="#00599e"
              class="mb-5"
            >
              <template #label>
                <div
                  class="d-flex align-center"
                >
                  A student is allowed up to

                  <v-text-field
                    v-model="numOfSubmissions"
                    :required="!allowInfiniteSubmissions"
                    :disabled="allowInfiniteSubmissions"
                    type="number"
                    class="d-inline-block mx-2"
                    min="2"
                    style="width: 100px"
                    single-line
                    hide-details
                    variant="outlined"
                    density="compact"
                    aria-label="Number of attempts a student is allowed"
                  />

                  attempts
                </div>
              </template>
            </v-radio>

            <v-radio
              :value="true"
              label="A student is allowed an infinite number of attempts"
              color="#00599e"
            />
          </v-radio-group>
        </div>

        <div class="mb-4">
          <v-divider class="mx-0 px-0" />
        </div>

        <div
          class="d-flex flex-column flex-wrap align-baseline"
        >
          <div
            class="d-flex align-center mb-5"
          >
            Minimum time between submissions:

            <v-text-field
              v-model="hoursBetweenSubmissions"
              type="number"
              class="d-inline-block mx-2"
              style="width: 100px"
              aria-label="assignment multiple submission minimum time between submissions"
              single-line
              hide-details
              variant="outlined"
              density="compact"
            />

            hours
          </div>

          <div
            class="d-flex align-center mb-5"
          >
            Keep the

            <v-select
              v-model="multipleSubmissionScoringScheme"
              :items="scoringOptions"
              class="d-inline-block keep-treatment-score-select mx-2"
              item-title="label"
              item-value="value"
              aria-label="assignment multiple submission scoring scheme"
              variant="outlined"
              single-line
              hide-details
              density="compact"
            />

            treatment score
          </div>

          <div
            v-if="multipleSubmissionScoringScheme === 'CUMULATIVE'"
            class="mb-0"
          >
            <div class="mb-4">
              Proportion earned on first attempt:

              <v-text-field
                v-model="cumulativeScoringInitialPercentage"
                type="number"
                class="d-inline-block"
                style="width: 100px"
                variant="outlined"
                density="compact"
                single-line
                hide-details
                aria-label="Proportion earned on first attempt, percent"
              />

              %
            </div>

            <p class="text-body-small text-medium-emphasis">
              Choose the % the first attempt should be worth. The remaining
              {{ remainingPercentage }}% will be distributed
              <span v-if="numOfSubmissions > 2">
                evenly among the other {{ numOfSubmissions - 1 }} attempts
                ({{ distributionPercentage }}% per attempt).
              </span>
              <span v-if="numOfSubmissions === 2">
                to the other attempt.
              </span>
            </p>
          </div>
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import {
  computed,
  watch,
  nextTick
} from "vue";

import {
  deleteAttributesFromElement,
  addAttributesToElement,
  getAttributeFromElement
} from "@/helpers/ui-utils.js";

defineOptions({
  name: "MultipleAttemptsSetting"
});

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
    default: () => ({})
  }
});

const emit = defineEmits([
  "update:modelValue"
]);

const cumulativeOptions = [
  {
    value: "MOST_RECENT",
    label: "Most Recent"
  },
  {
    value: "HIGHEST",
    label: "Highest"
  },
  {
    value: "AVERAGE",
    label: "Average"
  },
  {
    value: "CUMULATIVE",
    label: "Cumulative"
  }
];

const updateValue = value => {
  emit("update:modelValue", {
    ...props.modelValue,
    ...value
  });
};

const remainingPercentage = computed(() => {
  return 100 - cumulativeScoringInitialPercentage.value;
});

const distributionPercentage = computed(() => {
  return (
    (100 - cumulativeScoringInitialPercentage.value) /
    (numOfSubmissions.value - 1)
  ).toFixed(2);
});

const scoringOptions = computed(() => {
  return cumulativeOptions.filter(option => {
    if (
      option.value === "CUMULATIVE" &&
      numOfSubmissions.value === 0
    ) {
      return false;
    }

    return true;
  });
});

const allowMultipleAttempts = computed({
  get() {
    return numOfSubmissions.value !== null;
  },

  set(value) {
    updateValue({
      numOfSubmissions: value ? 0 : null
    });
  }
});

const allowInfiniteSubmissions = computed({
  get() {
    return numOfSubmissions.value > 0
      ? false
      : true;
  },

  set(value) {
    updateValue({
      numOfSubmissions: value ? 0 : 2
    });
  }
});

const numOfSubmissions = computed({
  get() {
    return props.modelValue?.numOfSubmissions;
  },

  set(value) {
    updateValue({
      numOfSubmissions:
        value === null || value === ""
          ? null
          : Number.parseInt(value, 10)
    });
  }
});

const multipleSubmissionScoringScheme = computed({
  get() {
    return props.modelValue?.multipleSubmissionScoringScheme;
  },

  set(value) {
    updateValue({
      multipleSubmissionScoringScheme: value
    });
  }
});

const hoursBetweenSubmissions = computed({
  get() {
    return props.modelValue?.hoursBetweenSubmissions;
  },

  set(value) {
    updateValue({
      hoursBetweenSubmissions:
        value === null || value === ""
          ? null
          : Number.parseFloat(value)
    });
  }
});

const cumulativeScoringInitialPercentage = computed({
  get() {
    return props.modelValue?.cumulativeScoringInitialPercentage;
  },

  set(value) {
    updateValue({
      cumulativeScoringInitialPercentage:
        value === null || value === ""
          ? null
          : Number.parseFloat(value)
    });
  }
});

watch(allowInfiniteSubmissions, newValue => {
  if (
    newValue === true &&
    multipleSubmissionScoringScheme.value === "CUMULATIVE"
  ) {
    updateValue({
      cumulativeScoringInitialPercentage: null,
      multipleSubmissionScoringScheme: "MOST_RECENT"
    });
  }
});

watch(allowMultipleAttempts, async newValue => {
  if (!newValue) {
    return;
  }

  await nextTick();

  window.setTimeout(() => {
    const ariaOwnsId = getAttributeFromElement(
      ".keep-treatment-score-select .v-field:first-of-type",
      "aria-owns"
    );

    deleteAttributesFromElement(
      ".keep-treatment-score-select .v-field",
      ["role"]
    );

    addAttributesToElement(
      ".keep-treatment-score-select .v-field",
      [
        {
          name: "role",
          value: "combobox"
        },
        {
          name: "aria-controls",
          value: ariaOwnsId
        }
      ]
    );
  }, 1000);
});
</script>

<style lang="scss" scoped>
.multiple-attempts-card {
  :deep(.v-card-text) {
    font-size: 16px;
    margin-left: 32px;
  }
}
</style>
