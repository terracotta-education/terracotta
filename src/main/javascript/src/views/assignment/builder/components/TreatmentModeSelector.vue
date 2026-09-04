<template>
  <div class="bottom-menu">
    <div
      v-if="!treatmentOptionSelected"
      class="treatment-mode-container d-flex flex-row"
    >
      <v-btn
        elevation="0"
        class="add-treatment-type"
        variant="text"
        @click="emit('add-terracotta-builder')"
      >
        Use Terracotta Builder
      </v-btn>

      <v-menu
        close-on-content-click
        close-on-click
        location="top"
      >
        <template #activator="{ props: menuProps }">
          <v-btn
            v-bind="menuProps"
            elevation="0"
            class="add-treatment-type"
            variant="text"
          >
            Add External Integration
            <v-icon>mdi-chevron-down</v-icon>
          </v-btn>
        </template>

        <v-list>
          <v-list-item
            v-for="client in integrationClients"
            :key="client.name"
            @click="emit('add-integration', client.name)"
          >
            <v-list-item-title>
              {{ client.name }}
            </v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </div>

    <div
      v-else
      class="treatment-mode-selected-container d-flex flex-row"
    >
      <div>
        <QuestionTypeMenu
          v-if="!isIntegrationType"
          @add-question="emit('add-question', $event)"
        />

        <CopyTreatmentMenu
          v-if="!isIntegrationType"
          :assignments="assignmentsAvailableToCopy"
          @duplicate="emit('duplicate', $event)"
        />
      </div>

      <v-btn
        v-if="displayBackToTreatmentModeSelection"
        elevation="0"
        class="treatment-type-selected"
        variant="text"
        @click="emit('back-to-treatment-mode-selection')"
      >
        BACK TO TREATMENT MODE SELECTION
      </v-btn>
    </div>
  </div>
</template>

<script setup>
import QuestionTypeMenu from "./QuestionTypeMenu.vue";
import CopyTreatmentMenu from "./CopyTreatmentMenu.vue";

defineOptions({
  name: "TreatmentModeSelector"
});

defineProps({
  treatmentOptionSelected: {
    type: Boolean,
    default: false
  },
  isIntegrationType: {
    type: Boolean,
    default: false
  },
  assignmentsAvailableToCopy: {
    type: Array,
    default: () => []
  },
  integrationClients: {
    type: Array,
    default: () => []
  },
  displayBackToTreatmentModeSelection: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  "add-terracotta-builder",
  "add-integration",
  "add-question",
  "duplicate",
  "back-to-treatment-mode-selection"
]);
</script>

<style scoped lang="scss">
// 636px matches this app's existing mobile-table breakpoint (see ComponentTable.vue)
@media (max-width: 636px) {
  .treatment-mode-container,
  .treatment-mode-selected-container {
    flex-direction: column;
  }
}
</style>
