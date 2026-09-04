<template>
  <template v-if="!singleConditionExperiment">
    <v-divider />

    <v-tabs
      :model-value="modelValue"
      elevation="0"
      show-arrows
      height="89"
      @update:model-value="$emit('update:modelValue', $event)"
    >
      <v-tab
        v-for="(exposure, index) in exposures"
        :key="exposure.exposureId"
        :value="index"
      >
        <div class="d-flex flex-column align-start py-1">
          <div class="section-tab-set">
            Set {{ index + 1 }}
          </div>

          <div
            class="d-block mt-4"
            :class="balanced ? 'section-tab-components-balanced' : 'section-tab-components-unbalanced'"
          >
            {{ rows[index]?.length || 0 }} Component{{ rows[index]?.length === 1 ? '' : 's' }}
          </div>
        </div>
      </v-tab>
    </v-tabs>
  </template>

  <v-divider />
</template>

<script setup>
defineOptions({
  name: "ExposureTabs"
});

defineProps({
  modelValue: {
    type: Number,
    required: true
  },
  exposures: {
    type: Array,
    required: true
  },
  rows: {
    type: Array,
    required: true
  },
  balanced: {
    type: Boolean,
    default: false
  },
  singleConditionExperiment: {
    type: Boolean,
    default: false
  }
});

defineEmits(["update:modelValue"]);
</script>
