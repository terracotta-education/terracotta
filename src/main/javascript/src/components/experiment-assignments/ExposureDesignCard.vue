<template>
  <div>
    <h3 class="my-4">
      Design
    </h3>

    <v-card
      class="data-table-design px-5 py-5 rounded-lg mx-3 mb-5 d-inline-block"
      variant="outlined"
    >
      <div
        v-for="group in visibleGroups"
        :key="group"
        class="groupNames"
      >
        {{ group }} will receive

        <v-chip
          :color="conditionColorMapping[groupConditionMap[group]]"
          variant="flat"
          density="compact"
          class="ma-2"
          label
        >
          {{ groupConditionMap[group] }}
        </v-chip>
      </div>

      <a
        v-if="allGroups.length > maxDesignGroups"
        class="text-blue"
        @click="designExpanded = !designExpanded"
      >
        <v-icon color="blue">
          {{ designExpanded ? 'mdi-minus' : 'mdi-plus' }}
        </v-icon>

        <span>{{ designExpanded ? "Less" : "More" }}</span>
      </a>
    </v-card>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";

const props = defineProps({
  exposure: {
    type: Object,
    required: true
  },
  conditionColorMapping: {
    type: Object,
    required: true
  },
  maxDesignGroups: {
    type: Number,
    default: 2
  }
});

const designExpanded = ref(false);

const groupConditionMap = computed(() => {
  const map = {};

  props.exposure.groupConditionList?.forEach(group => {
    map[group.groupName] = group.conditionName;
  });

  return map;
});

const allGroups = computed(() => {
  return props.exposure.groupConditionList
    ?.map(group => group.groupName)
    .sort() || [];
});

const visibleGroups = computed(() => {
  if (designExpanded.value) {
    return allGroups.value;
  }

  return allGroups.value.slice(0, props.maxDesignGroups);
});
</script>
