<template>
  <v-menu location="top">
    <template #activator="{ props: menuProps }">
      <v-btn
        v-bind="menuProps"
        elevation="0"
        class="treatment-type-selected"
        variant="text"
      >
        ADD QUESTION
        <v-icon>mdi-chevron-down</v-icon>
      </v-btn>
    </template>

    <v-list>
      <v-list-item @click="emit('add-question', 'ESSAY')">
        <v-list-item-title>
          <v-icon class="mr-1">mdi-text</v-icon>
          Short answer
        </v-list-item-title>
      </v-list-item>

      <v-list-item @click="emit('add-question', 'MC')">
        <v-list-item-title>
          <v-icon class="mr-1">mdi-radiobox-marked</v-icon>
          Multiple choice
        </v-list-item-title>
      </v-list-item>

      <v-list-item @click="emit('add-question', 'FILE')">
        <v-list-item-title>
          <v-icon class="mr-1">mdi-file-upload-outline</v-icon>
          File submission
        </v-list-item-title>
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<script setup>
defineOptions({
  name: "QuestionTypeMenu"
});

const emit = defineEmits([
  "add-question"
]);
</script>
