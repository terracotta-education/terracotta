<template>
  <div>
    <h4>
      {{ messages.headerText }}
    </h4>

    <p class="text-medium-emphasis pb-0">
      {{ messages.headerInstructionsText }}
    </p>

    <v-card
      variant="outlined"
      class="reveal-responses-card"
    >
      <v-card-title
        :class="{ 'bg-blue-lighten-5': allowStudentViewResponses }"
      >
        <v-checkbox
          v-model="allowStudentViewResponses"
          :label="messages.allowStudentViewResponsesLabel"
          class="mt-0"
          hide-details
          @update:model-value="changeAllowStudentViewResponses"
        />
      </v-card-title>

      <v-card-text
        v-if="allowStudentViewResponses"
        class="text-primary"
      >
        <div class="response-date-controls d-flex flex-wrap align-baseline mt-5">
          <div>
            {{ messages.allowStudentViewResponsesDatesLabel }}
          </div>

          <DateTimePicker
            :model-value="studentViewResponsesAfter"
            :max="addDays(studentViewResponsesBefore, -1)"
            id="show-responses-after"
            name="show-responses-after"
            aria-label="Show responses and points on date time picker"
            @update:model-value="studentViewResponsesAfter = $event"
          />

          <div>
            and hide on
          </div>

          <DateTimePicker
            :model-value="studentViewResponsesBefore"
            :min="addDays(studentViewResponsesAfter, 1)"
            id="show-responses-before"
            name="show-responses-before"
            aria-label="Hide responses and points on date time picker"
            @update:model-value="studentViewResponsesBefore = $event"
          />
        </div>

        <v-checkbox
          v-if="!isIntegration"
          v-model="allowStudentViewCorrectAnswers"
          class="allow-students-view-correct-answers"
          hide-details
        >
          <template #label>
            <span>
              Allow students to see correct answers and any comments
            </span>
          </template>
        </v-checkbox>

        <div
          v-if="!isIntegration && allowStudentViewCorrectAnswers"
          class="correct-answer-date-controls d-flex flex-wrap align-baseline mt-5"
        >
          <div>
            Show correct answers and comments on
          </div>

          <DateTimePicker
            :model-value="studentViewCorrectAnswersAfter"
            :min="convertDateToDateString(studentViewResponsesAfter)"
            :max="
              addDays(studentViewCorrectAnswersBefore, -1) ||
              convertDateToDateString(studentViewResponsesBefore)
            "
            id="show-correct-answers-after"
            name="show-correct-answers-after"
            aria-label="Show correct answers on date time picker"
            @update:model-value="studentViewCorrectAnswersAfter = $event"
          />

          <div>
            and hide on
          </div>

          <DateTimePicker
            :model-value="studentViewCorrectAnswersBefore"
            :min="
              addDays(studentViewCorrectAnswersAfter, 1) ||
              convertDateToDateString(studentViewResponsesAfter)
            "
            :max="convertDateToDateString(studentViewResponsesBefore)"
            id="show-correct-answers-before"
            name="show-correct-answers-before"
            aria-label="Hide correct answers on date time picker"
            @update:model-value="studentViewCorrectAnswersBefore = $event"
          />
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { computed } from "vue";

import DateTimePicker from "@/components/picker/DateTimePicker.vue";

defineOptions({
  name: "RevealResponsesSetting"
});

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
    default: () => ({})
  }
});

const emit = defineEmits([
  "update:modelValue"
]);

const updateValue = value => {
  emit("update:modelValue", {
    ...props.modelValue,
    ...value
  });
};

const createDateGetterSetter = prop => {
  return computed({
    get() {
      return props.modelValue[prop];
    },

    set(date) {
      updateValue({
        [prop]: date
      });
    }
  });
};

const integration = computed(() => {
  return props.modelValue?.integration || null;
});

const isIntegration = computed(() => {
  return integration.value !== null;
});

const integrationConfiguration = computed(() => {
  return isIntegration.value
    ? integration.value.configuration
    : {};
});

const integrationClient = computed(() => {
  return isIntegration.value
    ? integrationConfiguration.value.client
    : {};
});

const integrationClientName = computed(() => {
  return integrationClient.value?.name;
});

const messages = computed(() => {
  const defaultMessages = {
    headerText: "Reveal treatment responses",
    headerInstructionsText: "Decide if students should see their treatment responses and points once a treatment question is answered",
    allowStudentViewResponsesLabel: "Allow students to see their treatment responses and points earned for each response",
    allowStudentViewResponsesDatesLabel: "Show responses and points on"
  };

  if (!isIntegration.value) {
    return defaultMessages;
  }

  if (integrationClientName.value === "Custom Web Activity") {
    return defaultMessages;
  }

  return {
    headerText: "Reveal treatment scores",
    headerInstructionsText: "Decide if students should see their treatment points once a treatment question is answered",
    allowStudentViewResponsesLabel: "Allow students to see their treatment points earned for each response",
    allowStudentViewResponsesDatesLabel: "Show points on"
  };
});

const allowStudentViewResponses = computed({
  get() {
    return props.modelValue?.allowStudentViewResponses;
  },

  set(value) {
    updateValue({
      allowStudentViewResponses: value
    });
  }
});

const studentViewResponsesAfter = createDateGetterSetter(
  "studentViewResponsesAfter"
);

const studentViewResponsesBefore = createDateGetterSetter(
  "studentViewResponsesBefore"
);

const allowStudentViewCorrectAnswers = computed({
  get() {
    return props.modelValue?.allowStudentViewCorrectAnswers;
  },

  set(value) {
    updateValue({
      allowStudentViewCorrectAnswers: value
    });
  }
});

const studentViewCorrectAnswersAfter = createDateGetterSetter(
  "studentViewCorrectAnswersAfter"
);

const studentViewCorrectAnswersBefore = createDateGetterSetter(
  "studentViewCorrectAnswersBefore"
);

const convertDateToDateString = date => {
  if (!date) {
    return date;
  }

  const convertedDate = new Date(date);
  const month = String(convertedDate.getMonth() + 1).padStart(2, "0");
  const day = String(convertedDate.getDate()).padStart(2, "0");

  return `${convertedDate.getFullYear()}-${month}-${day}`;
};

const addDaysToDate = (date, days) => {
  const updated = new Date(date);

  updated.setDate(updated.getDate() + days);

  return updated;
};

const addDays = (date, days) => {
  if (!date) {
    return date;
  }

  return convertDateToDateString(
    addDaysToDate(date, days)
  );
};

const changeAllowStudentViewResponses = value => {
  updateValue({
    allowStudentViewResponses: value,
    allowStudentViewCorrectAnswers: allowStudentViewCorrectAnswers.value && value
  });
};
</script>

<style lang="scss" scoped>
.reveal-responses-card {
  :deep(.v-card-text) {
    font-size: 16px;
    margin-left: 32px;
  }
}

.allow-students-view-correct-answers {
  margin-top: 30px;
}

.response-date-controls,
.correct-answer-date-controls {
  // flex-wrap's gap only applies between wrapped lines - without it, the label/pickers/label
  // that wrap onto their own lines on narrow screens sit flush against each other
  row-gap: 12px;
}

.correct-answer-date-controls {
  margin-left: 32px;
}
</style>
