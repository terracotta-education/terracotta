<template>
  <div>
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
            <b>{{ assignmentName }}</b>.
          </p>
        </div>
      </template>

      <v-radio
        v-for="treatment in treatments"
        :key="treatment.treatmentId"
        :value="treatment.treatmentId"
        class="treatment-radio-option"
        color="primary"
        ripple
      >
        <template #label>
          <div class="treatment-radio-option-label">
            Treatment

            <v-chip
              v-if="treatments.length > 1"
              :color="treatment.conditionColor"
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
          </div>
        </template>
      </v-radio>
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
  assignmentName: {
    type: String,
    required: true
  },
  treatments: {
    type: Array,
    required: true
  }
});

const selectedTreatmentOption = ref(null);

watch(
  () => props.treatments,
  treatments => {
    if (treatments.length === 1) {
      selectedTreatmentOption.value = treatments[0].treatmentId;
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
