<template>
<div>
  <v-radio-group
    v-model="selectedRegradeOption"
    column="true"
    id="regrade-radio-group"
  >
    <template
      v-slot:label
    >
      <h2><b>Regrade Options</b></h2>
      <p>
        Choose a regrade option for the <b>{{ studentCountLabel }}</b> who {{ studentCountHaveLabel }} already completed the <b>{{ conditionName }} version</b> of the {{ assignmentName }} assignment.
        {{ lmsTitle }} will regrade all submissions for this version of the assignment after you save the treatment (students' scores MAY be affected).
        Scores for short answer and file submission questions that have already been graded will remain the same.
      </p>
    </template>
    <v-radio
      v-for="(option, i) in regradeOptions"
      :value="option.value"
      :key="i"
      class="regrade-radio-option"
      color="primary"
      ripple="true"
    >
      <template
        v-slot:label
      >
        <div class="regrade-radio-option-label">{{ option.label }}</div>
      </template>
    </v-radio>
  </v-radio-group>
  <input
    id="regrade-option-selected"
    type="hidden"
  />
</div>
</template>

<script setup>
import { ref, computed, watch } from "vue";

import { configuration } from "@/store/configuration.module";

defineOptions({
  name: "RegradeAssignmentDialog"
});

const props = defineProps({
  assignmentName: {
    type: String
  },
  conditionName: {
    type: String
  },
  studentCount: {
    type: Number
  },
  editedQuestionCount: {
    type: Number
  }
});

const configurationStore = configuration();

const selectedRegradeOption = ref(null);

const configurations = computed(() => {
  return configurationStore.get;
});

const questionLabel = computed(() => {
  return props.editedQuestionCount === 1
    ? "this question"
    : "the questions you've changed";
});

const regradeOptions = computed(() => {
  return [
    {
      value: "BOTH",
      label: "Award points for both corrected and previously correct answers (no scores will be reduced)"
    },
    {
      value: "CURRENT",
      label: "Only award points for the correct answer (some students' scores may be reduced)"
    },
    {
      value: "FULL",
      label: `Give everyone full credit for ${questionLabel.value}`
    },
    {
      value: "NONE",
      label: `Update ${questionLabel.value} without regrading`
    }
  ];
});

const studentCountLabel = computed(() => {
  return `${props.studentCount} student${props.studentCount > 1 ? "s" : ""}`;
});

const studentCountHaveLabel = computed(() => {
  return props.studentCount > 1 ? "have" : "has";
});

const lmsTitle = computed(() => {
  return configurations.value?.lmsTitle || "LMS";
});

watch(
  selectedRegradeOption,
  newValue => {
    const selectedInput = document.getElementById("regrade-option-selected");

    if (selectedInput) {
      selectedInput.value = newValue;
    }

    const confirmButton = document.getElementsByClassName("response-option-confirm")[0];

    if (confirmButton) {
      confirmButton.disabled = newValue === null;
    }
  },
  { immediate: true }
);
</script>

<style lang="scss" scoped>
div.swal2-popup.swal2-modal.regrade-assignment-popup {
  width: 52em !important;
}

#regrade-radio-group {
  display: grid !important;
  > legend.v-label {
    > h2 {
      text-align: left !important;
    }
    > p {
      display: block !important;
      text-align: left !important;
      font-size: 1.125em;
    }
  }
  .regrade-radio-option-label {
    margin-left: 8px !important;
    font-weight: 400 !important;
  }
}
</style>
