<template>
<div>
  <v-radio-group
    v-model="selectedTreatmentOption"
    column="true"
    id="copy-radio-group"
  >
    <template
      v-slot:label
    >
      <h2><b>Copy Treatment Options</b></h2>
      <p>
        Choose the treatment from which you wish to copy content to <b>{{ assignmentName }}</b>.
      </p>
    </template>
    <v-radio
      v-for="(treatment) in treatments"
      :value="treatment.treatmentId"
      :key="treatment.treatmentId"
      class="treatment-radio-option"
      color="primary"
      ripple="true"
    >
      <template
        v-slot:label
      >
        <div
          class="treatment-radio-option-label"
        >
          Treatment
          <v-chip
            v-if="treatments.length > 1"
            :color="treatment.conditionColor"
            label
          >
            {{ treatment.conditionName }}
          </v-chip>
          <v-chip
            v-if="treatments.length === 1"
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
    type="hidden"
  />
</div>
</template>

<script>
import Vuetify from "vuetify/lib";

export default {
  name: "CopyFromDialog",
  vuetify: new Vuetify(),
  props: {
    assignmentName: {
      type: String,
      required: true
    },
    treatments: {
      type: Array,
      required: true
    }
  },
  data: () => ({
    selectedTreatmentOption: null
  }),
  watch: {
    selectedTreatmentOption: {
      handler(newValue) {
        if (document.getElementById("treatment-option-selected")) {
          document.getElementById("treatment-option-selected").value = newValue;
        }

        if (document.getElementsByClassName("response-option-confirm")[0]) {
          document.getElementsByClassName("response-option-confirm")[0].disabled = newValue === null;
        }
      },
      immediate: true
    },
    treatments: {
      handler(newValue) {
        if (newValue.length === 1) {
          // only one treatment, pre-select it for the user
          this.selectedTreatmentOption = newValue[0].treatmentId;
        }
      },
      immediate: true
    }
  }
}
</script>

<style lang="scss" scoped>
div.swal2-popup.swal2-modal.move-assignment-popup {
  width: 52em !important;
}

#copy-radio-group {
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
