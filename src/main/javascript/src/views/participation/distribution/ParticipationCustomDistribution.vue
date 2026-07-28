<template>
  <div>
    <h1 class="mb-5">
      Select the percent of students you would like to receive each condition
    </h1>

    <div
      class="w-50 mx-auto my-0"
    >
      <v-row class="mx-2">
        <v-col cols="9" class="label">
          Condition
        </v-col>

        <v-col cols="3" class="label text-right">
          Distribution
        </v-col>
      </v-row>

      <v-card
        class="mt-2 mb-3 py-3 mx-auto lighten-5 rounded-lg"
        variant="outlined"
      >
        <v-card-text
          v-for="(condition, index) in conditions"
          :key="condition.conditionId"
          class="pa-5"
        >
          <v-row class="justify-space-between align-center">
            <v-col
              cols="9"
              class="py-0"
            >
              <v-card-title class="ma-0 pa-0 body-1">
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
                variant="outlined"
                hide-details
                required
                @input="touched = true"
              />
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
        elevation="0"
        class="mt-3"
        color="primary"
        @click="updateDistribution('ParticipationSummary')"
      >
        Continue
      </v-btn>
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  watch
} from "vue";

import { useRouter } from "vue-router";
import Swal from "sweetalert2";

import { condition as conditionModule } from "@/store/condition.module";
import { navigation as navigationModule } from "@/store/navigation.module";

defineOptions({
  name: "ParticipationCustomDistribution"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const conditionStore = conditionModule();
const navigationStore = navigationModule();

const seedDistribution = conditions => {
  return (conditions || []).map(
    condition => condition.distributionPct ?? ""
  );
};

const distributionValue = ref(seedDistribution(props.experiment.conditions));
const touched = ref(false);

// Re-seed if the parent swaps in a freshly fetched experiment after mount.
watch(
  () => props.experiment.conditions,
  conditions => {
    distributionValue.value = seedDistribution(conditions);
    touched.value = false;
  }
);

const editMode = computed(() => {
  return navigationStore.editMode;
});

const getSaveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "Home";
});

const conditions = computed(() => {
  return props.experiment.conditions || [];
});

const experimentId = computed(() => {
  return props.experiment.experimentId;
});

// Blank/garbage entries become NaN rather than silently coercing to 0
// (`Number("")` is 0) or being truncated (`parseInt("50.5")` is 50,
// `parseInt("50abc")` is 50).
const numericValues = computed(() => {
  return distributionValue.value.map(value => {
    const trimmed = String(value ?? "").trim();

    return trimmed === "" ? NaN : Number(trimmed);
  });
});

const totalDistribution = computed(() => {
  return numericValues.value.reduce(
    (accumulator, current) =>
      accumulator + (Number.isFinite(current) ? current : 0),
    0
  );
});

// Rounded to 2dp so float sums like 99.99999999999999 still pass.
const roundedTotal = computed(() => {
  return Math.round(totalDistribution.value * 100) / 100;
});

const displayTotal = computed(() => {
  return Number.isInteger(roundedTotal.value)
    ? roundedTotal.value
    : roundedTotal.value.toFixed(2);
});

const isValidAt = index => {
  const value = numericValues.value[index];

  return Number.isFinite(value) && value >= 0;
};

const isDisabled = computed(() => {
  return (
    !conditions.value.length ||
    numericValues.value.some((_, index) => !isValidAt(index)) ||
    roundedTotal.value !== 100
  );
});

const updateDistribution = async path => {
  const updatedConditions = conditions.value.map(
    (condition, index) => ({
      ...condition,
      distributionPct: numericValues.value[index],
      experimentId: experimentId.value
    })
  );

  try {
    const response =
      await conditionStore.updateConditions(
        updatedConditions
      );

    if (response?.status === 200) {
      router.push({
        name: path,
        params: {
          experiment: experimentId.value
        }
      });

      return;
    }

    await Swal.fire(
      response?.error ||
      "There was an error updating the condition distribution."
    );
  } catch (error) {
    console.error(
      "updateConditions | catch",
      error
    );
  }
};

const saveExit = () => {
  if (isDisabled.value) {
    router.push({
      name: getSaveExitPage.value,
      params: {
        experiment: experimentId.value
      }
    });

    return;
  }

  updateDistribution(getSaveExitPage.value);
};

defineExpose({
  saveExit
});
</script>

<style lang="scss" scoped>
.label {
  font-weight: 500;
  font-size: 12px;
  line-height: 16px;
  letter-spacing: 1.25px;
  text-transform: uppercase;
  color: #5f6368;
}

:deep(.v-input__control) {
  margin: 0;
}

:deep(.v-field__input) {
  text-align: right;
}

:deep(.v-messages) {
  display: none;
}

.errorMessage {
  color: map.get($red, "base");
}
</style>
