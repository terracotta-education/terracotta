<template>
  <div class="copy-candidates-dialog">
    <p class="copy-candidates-intro">
      This course was copied from a previous course, <b>{{ sourceCourseTitle }}</b>, that had one
      or more Terracotta experiments. Choose which one(s) to recreate here; each is rebuilt as a
      brand new experiment.
    </p>

    <div class="copy-candidates-stage">
      <!-- inert while a choice is pending confirmation, so the confirmation overlay below
           (which covers this area rather than replacing the whole dialog) also owns focus
           and screen-reader navigation, matching how a native modal-over-modal would behave -->
      <div
        class="copy-candidates-content"
        :inert="pendingAction ? true : null"
      >
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
            :class="{ 'copy-candidate-option--selected': isSelected(candidate.id) }"
            role="checkbox"
            :aria-checked="isSelected(candidate.id)"
            :aria-label="candidate.experimentTitle || '(untitled experiment)'"
            tabindex="0"
            @click="toggleSelected(candidate.id)"
            @keydown.space.prevent="toggleSelected(candidate.id)"
            @keydown.enter.prevent="toggleSelected(candidate.id)"
          >
            <div class="copy-candidate-title">
              <!-- selection is also shown via border/background color below, but that alone
                   shouldn't be the only signal (WCAG 1.4.1) - this icon gives a non-color one -->
              <v-icon
                v-if="isSelected(candidate.id)"
                icon="mdi-check-circle"
                color="primary"
                size="small"
                class="copy-candidate-check"
                aria-hidden="true"
              />
              {{ candidate.experimentTitle || "(untitled experiment)" }}
            </div>

            <div class="copy-candidate-meta">
              {{ candidate.conditionCount }} condition{{ candidate.conditionCount === 1 ? "" : "s" }}
              &middot;
              {{ candidate.assignmentCount }} assignment{{ candidate.assignmentCount === 1 ? "" : "s" }}
            </div>
          </div>
        </div>

        <div class="copy-candidates-actions">
          <button
            type="button"
            class="copy-candidates-btn copy-candidates-btn--tertiary"
            @click="requestDefer"
          >
            I'll decide later
          </button>
          <button
            type="button"
            class="copy-candidates-btn copy-candidates-btn--secondary"
            @click="requestDecline"
          >
            No thank you
          </button>
          <button
            type="button"
            class="copy-candidates-btn copy-candidates-btn--primary"
            :disabled="noneSelected"
            @click="requestCreate"
          >
            Create selected
          </button>
        </div>
      </div>

      <!-- shown over the checkbox grid (which stays visible, dimmed, underneath) rather than
           replacing the whole dialog with a separate popup - SweetAlert2 has no native support
           for stacking a second modal on top of one that's already open, so this is handled as
           an in-place overlay within the same dialog/popup instead -->
      <div
        v-if="pendingAction"
        class="copy-candidates-confirm-overlay"
        role="alertdialog"
        aria-modal="true"
        :aria-label="pendingAction.text"
      >
        <div class="copy-candidates-confirm-panel">
          <p>{{ pendingAction.text }}</p>
          <div class="copy-candidates-confirm-buttons">
            <button
              type="button"
              class="copy-candidates-btn copy-candidates-btn--tertiary"
              @click="cancelPendingAction"
            >
              Go back to selection
            </button>
            <button
              ref="gotItButtonRef"
              type="button"
              class="copy-candidates-btn copy-candidates-btn--primary"
              @click="confirmPendingAction"
            >
              Got it!
            </button>
          </div>
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
import { computed, nextTick, ref } from "vue";

defineOptions({
  name: "CopyCandidatesDialog"
});

const props = defineProps({
  candidates: {
    type: Array,
    required: true
  }
});

const emit = defineEmits(["create", "decline", "defer"]);

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

const isSelected = candidateId => selectedIds.value.includes(candidateId);

const toggleSelected = candidateId => {
  selectedIds.value = isSelected(candidateId)
    ? selectedIds.value.filter(id => id !== candidateId)
    : [...selectedIds.value, candidateId];
};

const selectAll = () => {
  selectedIds.value = props.candidates.map(candidate => candidate.id);
};

const unselectAll = () => {
  selectedIds.value = [];
};

// three distinct outcomes, not just confirm/cancel: creating imports the selected
// candidates; declining ("No thanks") dismisses every candidate currently shown, so the
// prompt stops appearing for good; deferring ("I'll decide later") leaves everything
// PENDING so the prompt simply asks again next visit. Each is confirmed first, in place,
// since none of them can be undone once chosen.
const pendingAction = ref(null);
const gotItButtonRef = ref(null);

const requestConfirmation = (type, text) => {
  pendingAction.value = { type, text };
  nextTick(() => gotItButtonRef.value?.focus());
};

const requestDefer = () => requestConfirmation(
  "defer",
  "Experiment selection will be available until you either selected one from this list or have created a new one yourself."
);

const requestDecline = () => requestConfirmation(
  "decline",
  "You will not be able to return to this screen to select experiments. You will need to export and import manually."
);

const requestCreate = () => requestConfirmation(
  "create",
  "Ensure you've selected all experiments you wish to create now. You will not be able to make another selection."
);

const cancelPendingAction = () => {
  pendingAction.value = null;
};

const confirmPendingAction = () => {
  const { type } = pendingAction.value;
  pendingAction.value = null;

  if (type === "create") {
    emit("create", selectedIds.value);
    return;
  }

  emit(type);
};
</script>

<style lang="scss" scoped>
.copy-candidates-intro {
  text-align: left;
  margin-bottom: 16px;
}

.copy-candidates-stage {
  position: relative;
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
  // 2px on every state (not just selected) so selecting/unselecting doesn't shift layout
  // by changing border width
  border: 2px solid rgba(0, 0, 0, 0.12);
  border-radius: 8px;
  padding: 8px 12px;
  cursor: pointer;
  transition: border-color 0.15s ease, background-color 0.15s ease;

  &:hover {
    border-color: rgba(0, 0, 0, 0.3);
  }

  // a visible focus ring is the one part of this that must never depend on color alone
  // being enough - this is a custom (div-based) checkbox, so it gets no native outline
  &:focus-visible {
    outline: 2px solid map.get($blue, "base");
    outline-offset: 2px;
  }

  &--selected {
    border-color: map.get($blue, "base");
    background-color: map.get($blue, "lighten-5");

    &:hover {
      border-color: map.get($blue, "base");
    }
  }
}

.copy-candidate-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}

.copy-candidate-check {
  flex: none;
}

.copy-candidate-meta {
  font-size: 0.85em;
  color: rgba(0, 0, 0, 0.6);
  // lines up under the title text, which no longer has a checkbox indenting it
  margin-left: 0;
}

.copy-candidates-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
}

.copy-candidates-btn {
  border: none;
  border-radius: 6px;
  padding: 10px 20px;
  font-size: 0.95em;
  font-weight: 500;
  cursor: pointer;

  &:focus-visible {
    outline: 2px solid map.get($blue, "base");
    outline-offset: 2px;
  }

  &--primary {
    background-color: map.get($blue, "primary");
    color: #fff;

    &:disabled {
      background-color: rgba(0, 0, 0, 0.12);
      color: rgba(0, 0, 0, 0.38);
      cursor: default;
    }
  }

  &--secondary {
    background: #fff;
    color: map.get($red, "base");
    border: 1px solid map.get($red, "base");
  }

  &--tertiary {
    background-color: map.get($swal, "cancel");
    color: #fff;
  }
}

.copy-candidates-confirm-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  // translucent, not opaque - the grid stays visible (if dimmed) underneath, since the
  // point of this overlay is to sit "over" the selection instead of hiding it
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  z-index: 2;
}

.copy-candidates-confirm-panel {
  max-width: 420px;
  text-align: center;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.15);
  border-radius: 8px;
  padding: 20px 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.copy-candidates-confirm-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}
</style>
