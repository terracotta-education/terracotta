<template>
  <ul class="component-steps">
    <li
      v-for="section in sectionList"
      :key="section.key"
    >
      <template
        v-if="section.key === currentSection"
      >
        <strong
          :class="{
            'text-green':
              isSummary &&
              section.key === route.meta.currentSection
          }"
        >
          {{ section.name }}
        </strong>
      </template>

      <template v-else>
        <span
          :class="{
            'text-green':
              (
                isSummary &&
                section.key === route.meta.currentSection
              ) ||
              completedSections[currentSection]?.includes(
                section.key
              )
          }"
        >
          {{ section.name }}
        </span>
      </template>

      <div
        v-if="section.key === currentSection"
        class="steps-list"
        :class="{ finished: isSummary }"
      >
        <div
          v-for="(step, index) in section.steps"
          :key="step.key"
          class="steps-list__step"
          :class="{
            complete:
              isSummary ||
              index <= currentStepNum
          }"
        >
          <span
            class="steps-list__indicator"
            :class="{
              complete:
                isSummary ||
                index <= currentStepNum
            }"
          />

          <span
            class="steps-list__label"
          >
            {{ step.name }}
          </span>
        </div>
      </div>
    </li>
  </ul>
</template>

<script setup>
import {
  computed,
  ref
} from "vue";

import { useRoute } from "vue-router";

defineOptions({
  name: "StepsProgress"
});

const props = defineProps({
  currentSection: {
    type: String,
    required: true
  },
  currentStep: {
    type: String,
    required: true
  },
  participationType: {
    type: String,
    required: true
  }
});

const route = useRoute();

const completedSections = ref({
  design: [],
  participation: ["design"],
  assignments: []
});

const generateSteps = () => {
  const selectionType =
    route.meta.selectionType;

  const steps = [
    {
      key: "participation_selection_method",
      name: "Selection Method"
    }
  ];

  if (
    selectionType === "consent" ||
    props.participationType === "CONSENT"
  ) {
    steps.push(
      {
        key:
          "participation_selection_consent_title",
        name: "Assignment Title"
      },
      {
        key:
          "participation_selection_consent_file",
        name: "Informed Consent"
      }
    );
  } else if (
    selectionType === "manual" ||
    props.participationType === "MANUAL"
  ) {
    steps.push({
      key: "select_participants",
      name: "Select Participants"
    });
  }

  if (
    selectionType === "any" ||
    selectionType === "consent" ||
    selectionType === "manual" ||
    selectionType === "auto"
  ) {
    steps.push({
      key: "participation_distribution",
      name: "Distribution"
    });
  }

  return steps;
};

const sectionList = computed(() => [
  {
    key: "design",
    name: "Section 1: Design",
    steps: [
      {
        key: "design_title",
        name: "Title"
      },
      {
        key: "design_description",
        name: "Description"
      },
      {
        key: "design_conditions",
        name: "Conditions"
      },
      {
        key: "design_type",
        name: "Experiment Type"
      }
    ]
  },
  {
    key: "participation",
    name: "Section 2: Participation",
    steps: generateSteps()
  }
]);

const isSummary = computed(() => {
  return (["ExperimentDesignSummary", "ParticipationSummary"].includes(route.name));
});

const currentStepNum = computed(() => {
  const section =
    sectionList.value.find(
      s => s.key === props.currentSection
    );

  return (
    section?.steps.findIndex(
      step => step.key === props.currentStep
    ) ?? -1
  );
});
</script>

<style lang="scss">
ul.component-steps {
  list-style: none;
  padding: 0 !important;
  font-size: 15px;

  > li {
    text-align: left;
  }
}

.steps-list {
  background: map.get($grey, "lightest");
  margin-top: 30px;

  &.finished {
    .steps-list__step.complete {
      &::before {
        background: map.get($green, "base");
      }

      .steps-list__indicator {
        border-color: map.get($green, "base");
      }
    }
  }

  &__step {
    position: relative;
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 0 0 28px 0;

    &.complete {
      &::before {
        content: "";
        position: absolute;
        left: 0;
        bottom: 30px;
        width: 14px;
        height: 115%;
        background: map.get($blue, "primary");
        border-bottom-left-radius: 999px;
        border-bottom-right-radius: 999px;
        z-index: 0;
      }

      &:first-child::before {
        display: none;
      }
    }
  }

  &__indicator {
    position: relative;
    z-index: 1;

    display: block;
    width: 14px;
    min-width: 14px;
    height: 14px;

    background: white;
    border: 5px solid #e2e2e2;
    border-radius: 50%;

    &.complete {
      border: 5px solid map.get($blue, "primary");
    }
  }

  &__label {
    color: black;
  }
}
</style>
