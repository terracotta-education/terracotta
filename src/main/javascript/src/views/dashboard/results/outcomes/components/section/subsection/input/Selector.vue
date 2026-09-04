<template>
<v-col
  class="container-selector"
>
  <v-row
    v-for="(_, i) in experimentExposures"
    :key="i"
    class="input-selector mb-5"
  >
    <v-select
      v-model="selected[i]"
      :items="options[i]"
      :label="selectorLabel(i)"
      @update:modelValue="onOutcomeSelect(i)"
      @keydown.backspace="clearOutcomeSelection(i)"
      @keydown.delete="clearOutcomeSelection(i)"
      item-title="title"
      item-value="outcomeId"
      class="select-outcomes"
      variant="outlined"
      density="compact"
      hide-details
      clearable
    >
      <template
        v-slot:selection="{ item }"
      >
        <span
          class="truncated"
        >
          {{ item.title }}
        </span>
      </template>
    </v-select>
  </v-row>
</v-col>
</template>

<script setup>
import { ref, computed, watch, onMounted } from "vue";
import { experiment as useExperimentStore } from "@/store/experiment.module";
import { exposures as useExposuresStore } from "@/store/exposures.module";
import { outcome as useOutcomeStore } from "@/store/outcome.module";

defineOptions({
  name: "OutcomeSelector"
});

const emit = defineEmits(["hasSelections", "hasCleared"]);

const loaded = ref(false);

const staticOutcomes = [
  { outcomeId: "AVERAGE_ASSIGNMENT_SCORE", title: "Average component score" },
  { outcomeId: "TIME_ON_TASK", title: "Time on task" }
];

const experiment = computed(() => useExperimentStore().experiment);
const exposures = computed(() => useExposuresStore().exposures);
const outcomes = computed(() => useOutcomeStore().outcomes);

const experimentId = computed(() => experiment.value?.experimentId);

const experimentExposures = computed(() =>
  [...(exposures.value || [])].sort((a, b) =>
    a.title.localeCompare(b.title, undefined, { sensitivity: "base" })
  )
);

const experimentOutcomes = computed(() => outcomes.value || []);

const options = computed(() =>
  experimentExposures.value.map(exposure => [
    ...staticOutcomes,
    ...experimentOutcomes.value.filter(o => o.exposureId === exposure.exposureId)
  ])
);

const fetchOutcomes = () =>
  useOutcomeStore().fetchOutcomesByExperimentId([experimentId.value]);

const selected = ref([]);
const selectedExposureIds = ref([]);
const staticOutcomeIds = staticOutcomes.map(o => o.outcomeId);

const selectorLabel = index => `Exposure Set ${index + 1}`;

const initSelected = () => {
  selected.value = [];
  selectedExposureIds.value = [];
  experimentExposures.value.forEach((exposure, index) => {
    selected.value[index] = null;
    selectedExposureIds.value[index] = exposure.exposureId;
  });
};

const emitSelection = selections => {
  if (
    selections.outcomeIds.length ||
    (selections.alternateId.id && selections.alternateId.exposures.length)
  ) {
    emit("hasSelections", selections);
  } else {
    emit("hasCleared");
  }
};

const createEmptySelections = () => ({
  outcomeIds: [],
  alternateId: { id: null, exposures: [] }
});

const isStaticOutcome = outcomeId => staticOutcomeIds.includes(outcomeId);

const handleStaticOutcomeSelection = (index, selections) => {
  const selectedValue = selected.value[index];

  let isNewAltId = selected.value
    .filter((_, i) => i !== index)
    .every(v => v === null);

  selected.value.forEach((value, i) => {
    if (value === null || i === index) return;
    isNewAltId = isNewAltId || value !== selectedValue;
  });

  if (isNewAltId) {
    selections.alternateId.id = selectedValue;
    selected.value = options.value.map((_, optionIndex) => {
      selections.alternateId.exposures.push(selectedExposureIds.value[optionIndex]);
      return selectedValue;
    });
    return;
  }

  const hasSameAltIdElsewhere = selected.value.some(
    (value, i) => i !== index && value === selectedValue
  );

  if (hasSameAltIdElsewhere) {
    selections.alternateId.id = selectedValue;
    selected.value.forEach((value, i) => {
      if (value !== null) selections.alternateId.exposures.push(selectedExposureIds.value[i]);
    });
  }
};

const handleClearedOutcomeSelection = selections => {
  const altId = selected.value.find(v => isStaticOutcome(v));
  if (altId) {
    selections.alternateId.id = altId;
    selected.value.forEach((value, i) => {
      if (value !== null) selections.alternateId.exposures.push(selectedExposureIds.value[i]);
    });
    return;
  }
  selections.outcomeIds = selected.value.filter(v => v !== null);
};

const handleStandardOutcomeSelection = selections => {
  selected.value = selected.value.map(v => isStaticOutcome(v) ? null : v);
  selections.outcomeIds = selected.value.filter(v => v !== null);
};

const onOutcomeSelect = index => {
  const selections = createEmptySelections();
  const selectedValue = selected.value[index];

  if (isStaticOutcome(selectedValue)) {
    handleStaticOutcomeSelection(index, selections);
  } else if (selectedValue == null) {
    handleClearedOutcomeSelection(selections);
  } else {
    handleStandardOutcomeSelection(selections);
  }

  emitSelection(selections);
};

const clearOutcomeSelection = index => {
  selected.value = [
    ...selected.value.slice(0, index),
    null,
    ...selected.value.slice(index + 1)
  ];
  onOutcomeSelect(index);
};

watch(outcomes, () => {
  loaded.value = true;
});

onMounted(async () => {
  await fetchOutcomes();
  initSelected();
});
</script>

<style scoped>
div.container-selector {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: fit-content;
  max-width: 100%;
  & div.input-selector {
    width: 100%;
    min-width: 100%;
    align-items: center;
    > span.input-selector-label {
      font-weight: 500;
    }
  }
  & .v-select {
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
