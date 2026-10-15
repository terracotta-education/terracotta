<template>
  <div class="copy-from-dialog">
    <v-radio-group
      id="copy-radio-group"
      v-model="selectedTreatmentOption"
    >
      <template #label>
        <div class="copy-radio-label">
          <h2>
            <b>Copy Treatment Options</b>
          </h2>

          <p>
            Choose the treatment from which you wish to copy content to
            <span class="copy-target-treatment">
              Treatment

              <v-chip
                v-if="!targetSingleVersion"
                :color="targetConditionColor"
                variant="flat"
                label
              >
                {{ targetConditionName }}
              </v-chip>

              <v-chip
                v-else
                label
                color="lightgrey"
                class="v-chip--only-one"
              >Only One Version</v-chip></span>.
          </p>
        </div>
      </template>

      <v-tooltip
        v-for="treatment in treatments"
        :key="treatment.treatmentId"
        :disabled="!treatment.disabled"
        location="end"
        content-class="tool-tip-content"
      >
        <template #activator="{ props: tooltipProps }">
          <!-- a disabled radio doesn't get pointer events, so the tooltip hangs off this wrapper -->
          <div
            v-bind="tooltipProps"
            class="treatment-radio-option-wrapper"
          >
            <v-radio
              :value="treatment.treatmentId"
              :disabled="!!treatment.disabled"
              class="treatment-radio-option"
              color="primary"
              ripple
            >
              <template #label>
                <div class="treatment-radio-option-label">
                  Treatment

                  <v-chip
                    v-if="!singleVersion"
                    :color="treatment.conditionColor"
                    variant="flat"
                    label
                  >
                    {{ treatment.conditionName }}
                  </v-chip>

                  <v-chip
                    v-else
                    label
                    color="lightgrey"
                    class="v-chip--only-one"
                  >
                    Only One Version
                  </v-chip>

                  <span
                    v-if="treatment.disabled"
                    class="d-sr-only"
                  >
                    This treatment has no content.
                  </span>
                </div>
              </template>
            </v-radio>
          </div>
        </template>

        <div class="tool-tip-content-body">
          This treatment has no content.
        </div>
      </v-tooltip>
    </v-radio-group>

    <input
      id="treatment-option-selected"
      :value="selectedTreatmentOption"
      type="hidden"
    />
  </div>
</template>

<script setup>
import {
  ref,
  watch
} from "vue";

defineOptions({
  name: "CopyFromDialog"
});

const props = defineProps({
  // the treatment being edited, which the content is copied to
  targetConditionName: {
    type: String,
    default: ""
  },
  targetConditionColor: {
    type: String,
    default: undefined
  },
  targetSingleVersion: {
    type: Boolean,
    default: false
  },
  treatments: {
    type: Array,
    required: true
  },
  // whether the source assignment really has only one version. Not the same as one treatment
  // being listed: treatments with nothing to copy, and the one being edited, aren't listed
  singleVersion: {
    type: Boolean,
    default: false
  }
});

const selectedTreatmentOption = ref(null);

watch(
  () => props.treatments,
  treatments => {
    // nothing to choose between when only one treatment has content
    const selectable = treatments.filter(treatment => !treatment.disabled);

    if (selectable.length === 1) {
      selectedTreatmentOption.value = selectable[0].treatmentId;
    }
  },
  {
    immediate: true
  }
);

watch(
  selectedTreatmentOption,
  value => {
    const confirmButton = document.getElementsByClassName(
      "response-option-confirm"
    )[0];

    if (confirmButton) {
      confirmButton.disabled = value === null;
    }
  },
  {
    immediate: true
  }
);
</script>

<style lang="scss" scoped>
div.swal2-popup.swal2-modal.move-assignment-popup {
  width: 52em !important;
}

// the radio group's label (the title and instructions) is a v-label, which doesn't wrap and clips
// what doesn't fit. It renders outside #copy-radio-group, so this hangs off the dialog's own root
.copy-from-dialog :deep(.v-label) {
  white-space: normal;
  overflow: visible;
  // v-label fades its text by default, which left the title and instructions washed out
  opacity: 1;
}

.copy-from-dialog .copy-radio-label {
  color: rgba(0, 0, 0, 0.87);
}

// unavailable, but still readable (Vuetify's own 0.38 made the condition unreadable)
.copy-from-dialog :deep(.v-selection-control--disabled) {
  opacity: 0.6;
}

// only as wide as the option, so the tooltip sits beside the treatment it's about
.treatment-radio-option-wrapper {
  display: flex;
  width: fit-content;
}

#copy-radio-group {
  display: grid !important;

  .copy-radio-label {
    h2 {
      text-align: left !important;
    }

    p {
      display: block !important;
      text-align: left !important;
      font-size: 1.125em;
    }
  }

  & .treatment-radio-option-label {
    margin-left: 8px !important;
    font-weight: 400 !important;
  }

  & .treatment-radio-option {
    min-width: 80%;
    margin: 8px auto;
  }
}
</style>
