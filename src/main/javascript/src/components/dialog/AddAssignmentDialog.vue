<template>
  <div v-if="isSingleConditionExperiment">
    <v-btn
      v-if="hasExisting"
      color="primary"
      elevation="0"
      @click="handleVersionSelection('single')"
    >
      Add Assignment
    </v-btn>

    <v-btn
      v-else
      class="btn-create-first-assignment"
      elevation="0"
      @click="handleVersionSelection('single')"
    >
      Create Assignment
    </v-btn>
  </div>

  <div v-else>
    <v-menu
      v-model="addAssignmentDialogOpen"
      :close-on-content-click="false"
      :open-on-click="true"
      :open-on-hover="false"
      content-class="add-assignment-dialog"
      transition="scale-transition"
      location="bottom end"
    >
      <template #activator="{ props }">
        <v-btn
          v-if="hasExisting"
          v-bind="props"
          :disabled="disableAddAssignmentButton"
          color="primary"
          elevation="0"
          tabindex="0"
        >
          Add Assignment
        </v-btn>

        <v-btn
          v-else
          v-bind="props"
          class="btn-create-first-assignment"
          elevation="0"
          tabindex="0"
        >
          Create Assignment
        </v-btn>
      </template>

      <div class="add-assignment-dialog-content">
        <div class="add-assignment-version-option">
          <v-btn
            role="menuitem"
            color="primary"
            elevation="0"
            tabindex="0"
            @click="handleVersionSelection('multiple')"
            @keyup.enter="handleVersionSelection('multiple')"
            @keyup.space="handleVersionSelection('multiple')"
          >
            With Different Versions
          </v-btn>

          <p>
            Create <u>multiple</u> treatments of your assignment so your
            students can experience different conditions.
          </p>
        </div>

        <div class="add-assignment-version-option">
          <v-btn
            role="menuitem"
            color="primary"
            elevation="0"
            tabindex="0"
            @click="handleVersionSelection('single')"
            @keyup.enter="handleVersionSelection('single')"
            @keyup.space="handleVersionSelection('single')"
          >
            With Only One Version
          </v-btn>

          <p>
            Create <u>one</u> assignment so all students experience the same
            condition, such as a questionnaire.
          </p>
        </div>
      </div>
    </v-menu>
  </div>
</template>

<script setup>
import {
  ref,
  watch
} from "vue";

defineOptions({
  name: "AddAssignmentDialog"
});

defineProps({
  hasExisting: {
    type: Boolean,
    default: false
  },
  isSingleConditionExperiment: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  "single",
  "multiple"
]);

const addAssignmentDialogOpen = ref(false);
const disableAddAssignmentButton = ref(false);

watch(addAssignmentDialogOpen, isOpen => {
  disableAddAssignmentButton.value = isOpen;
});

const handleVersionSelection = version => {
  emit(version);
  addAssignmentDialogOpen.value = false;
};
</script>

<style lang="scss" scoped>
.btn-create-first-assignment {
  border-radius: 24px;
  width: fit-content;
  min-height: 48px;
  background-color: white !important;
  border: 1px solid;
}

:deep(.add-assignment-dialog) {
  width: 350px;
  background-color: white;
  padding: 5px 5px 0 5px;
  border: thin solid lightgrey;
}

.add-assignment-dialog-content {
  > div.add-assignment-version-option {
    border: thin solid lightgrey;
    border-radius: 5px;
    padding: 5px;
    margin-bottom: 5px;
    text-align: center;

    > p {
      margin-bottom: 0 !important;
      padding-bottom: 0 !important;
      text-align: left;
    }
  }
}
</style>
