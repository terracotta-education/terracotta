<template>
  <v-select
    v-model="activeSubmissionId"
    :items="selectableSubmissions"
    label="Submissions"
    item-title="label"
    item-value="value"
    class="select-submissions"
    tabindex="0"
    variant="outlined"
    hide-details
  />
</template>

<script setup>
import {
  ref,
  computed,
  watch,
  nextTick,
  onMounted
} from "vue";

import { byDateSubmitted } from "@/helpers/date-time";
import {
  deleteAttributesFromElement,
  addAttributesToElement,
  getAttributeFromElement
} from "@/helpers/ui-utils.js";

defineOptions({
  name: "SubmissionSelector"
});

const props = defineProps({
  submissions: {
    type: Array,
    required: true
  }
});

const emit = defineEmits([
  "select"
]);

const activeSubmissionId = ref(null);

const allSubmissions = computed(() => {
  return props.submissions || [];
});

const orderedSubmissions = computed(() => {
  // newest first. The dates arrive as ISO text, so subtracting them directly gave NaN and left the
  // attempts in the API's oldest first order, labelled the wrong way round
  return [...allSubmissions.value].sort((a, b) => byDateSubmitted(b, a));
});

const selectableSubmissions = computed(() => {
  return orderedSubmissions.value.map((submission, index) => ({
    value: submission.submissionId,
    label: `Attempt ${allSubmissions.value.length - index}`
  }));
});

const setLatestSubmission = () => {
  activeSubmissionId.value =
    orderedSubmissions.value[0]?.submissionId || null;
};

const patchSubmissionSelectAria = async () => {
  await nextTick();

  const ariaOwnsId = getAttributeFromElement(
    ".v-select.select-submissions .v-field:first-of-type",
    "aria-owns"
  );

  deleteAttributesFromElement(
    ".v-select.select-submissions .v-field",
    ["role"]
  );

  addAttributesToElement(
    ".v-select.select-submissions .v-field",
    [
      {
        name: "role",
        value: "combobox"
      },
      {
        name: "aria-controls",
        value: ariaOwnsId
      }
    ]
  );
};

watch(activeSubmissionId, newValue => {
  emit("select", newValue);
});

watch(
  allSubmissions,
  async () => {
    setLatestSubmission();
    await patchSubmissionSelectAria();
  },
  {
    immediate: true
  }
);

onMounted(() => {
  patchSubmissionSelectAria();
});
</script>
