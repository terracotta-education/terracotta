<template>
  <div>
    <p class="copy-candidates-intro">
      This course was copied from a previous course, <b>{{ sourceCourseTitle }}</b>, that had one
      or more Terracotta experiments. Choose which one(s) to recreate here - each is rebuilt as a
      brand new experiment, exactly like importing an exported experiment file.
    </p>

    <div class="copy-candidates-select-all">
      <button
        type="button"
        class="copy-candidates-select-all-link"
        :disabled="allSelected"
        @click="selectAll"
      >
        Select All
      </button>
      &middot;
      <button
        type="button"
        class="copy-candidates-select-all-link"
        :disabled="noneSelected"
        @click="unselectAll"
      >
        Unselect All
      </button>
    </div>

    <div class="copy-candidates-grid">
      <div
        v-for="candidate in candidates"
        :key="candidate.id"
        class="copy-candidate-option"
      >
        <v-checkbox
          v-model="selectedIds"
          :value="candidate.id"
          :label="candidate.experimentTitle || '(untitled experiment)'"
          color="primary"
          density="compact"
          hide-details
        />

        <div class="copy-candidate-meta">
          {{ candidate.conditionCount }} condition{{ candidate.conditionCount === 1 ? "" : "s" }}
          &middot;
          {{ candidate.assignmentCount }} assignment{{ candidate.assignmentCount === 1 ? "" : "s" }}
        </div>
      </div>
    </div>

    <input
      id="copy-candidates-selected"
      :value="JSON.stringify(selectedIds)"
      type="hidden"
    />
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";

defineOptions({
  name: "CopyCandidatesDialog"
});

const props = defineProps({
  candidates: {
    type: Array,
    required: true
  }
});

const emit = defineEmits(["selectionChange"]);

// all candidates are staged from the same course-copy notice, so they share one source
// course - see ExperimentCopyCandidateServiceImpl.stageFromNotice
const sourceCourseTitle = computed(() => {
  return props.candidates[0]?.sourceCourseTitle || "(unknown course)";
});

const selectedIds = ref([]);

const allSelected = computed(() => {
  return selectedIds.value.length === props.candidates.length;
});

const noneSelected = computed(() => {
  return selectedIds.value.length === 0;
});

watch(
  selectedIds,
  value => emit("selectionChange", value),
  { immediate: true }
);

const selectAll = () => {
  selectedIds.value = props.candidates.map(candidate => candidate.id);
};

const unselectAll = () => {
  selectedIds.value = [];
};
</script>

<style lang="scss" scoped>
.copy-candidates-intro {
  text-align: left;
  margin-bottom: 16px;
}

.copy-candidates-select-all {
  text-align: left;
  margin-bottom: 12px;
}

.copy-candidates-select-all-link {
  background: none;
  border: none;
  padding: 0;
  color: map.get($blue, "primary");
  cursor: pointer;
  font-size: 0.9em;
  text-decoration: underline;

  &:hover,
  &:focus-visible {
    text-decoration: none;
  }

  &:disabled {
    color: rgba(0, 0, 0, 0.38);
    cursor: default;
    text-decoration: none;
  }
}

.copy-candidates-grid {
  display: grid;
  // fills as many ~250px columns as fit (3 at the dialog's own widened size), and
  // collapses down to 2, then 1 (fully stacked) as the available width shrinks -
  // this dialog renders inside the LTI iframe, whose width varies with the host page
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 12px;
}

.copy-candidate-option {
  text-align: left;
  border: thin solid rgba(0, 0, 0, 0.12);
  border-radius: 8px;
  padding: 8px 12px;
}

.copy-candidate-meta {
  font-size: 0.85em;
  color: rgba(0, 0, 0, 0.6);
  margin-left: 32px;
}
</style>
