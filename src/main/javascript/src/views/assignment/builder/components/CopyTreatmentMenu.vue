<template>
  <v-menu
    v-if="assignments.length > 0"
    close-on-content-click
    close-on-click
    location="top"
  >
    <template #activator="{ props: menuProps }">
      <v-btn
        v-bind="menuProps"
        elevation="0"
        class="treatment-type-selected"
        variant="text"
      >
        Copy Content From
        <v-icon>mdi-chevron-down</v-icon>
      </v-btn>
    </template>

    <v-list>
      <v-list-item
        v-for="assignment in assignments"
        :key="assignment.assignmentId"
        :aria-label="`copy content from ${assignment.title}`"
        @click="emit('duplicate', assignment)"
      >
        <v-list-item-title>
          {{ assignment.title }}
        </v-list-item-title>
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<script setup>
defineOptions({
  name: "CopyTreatmentMenu"
});

defineProps({
  assignments: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits([
  "duplicate"
]);
</script>
