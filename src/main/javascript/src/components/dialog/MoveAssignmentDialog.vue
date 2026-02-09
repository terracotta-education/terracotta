<template>
<div>
  <v-radio-group
    v-model="selectedExposureOption"
    column="true"
    id="move-radio-group"
  >
    <template
      v-slot:label
    >
      <h2><b>Move Assignment Options</b></h2>
      <p>
        Choose the exposure set you wish to move <b>{{ assignmentName }}</b> to.
      </p>
    </template>
    <v-radio
      v-for="(exposure) in exposures"
      :value="exposure.exposureId"
      :key="exposure.exposureId"
      class="exposure-radio-option"
      color="primary"
      ripple="true"
    >
      <template
        v-slot:label
      >
        <div
          class="exposure-radio-option-label"
        >
          {{ exposure.title }}
        </div>
      </template>
    </v-radio>
  </v-radio-group>
  <input
    id="exposure-option-selected"
    type="hidden"
  />
</div>
</template>

<script>
  import Vuetify from "vuetify/lib";

  export default {
    name: "MoveAssignmentDialog",
    vuetify: new Vuetify(),
    props: {
      assignmentName: {
        type: String,
        required: true
      },
      exposures: {
        type: Array,
        required: true
      }
    },
    data: () => ({
      selectedExposureOption: null
    }),
    watch: {
      selectedExposureOption: {
        handler(newValue) {
          if (document.getElementById("exposure-option-selected")) {
            document.getElementById("exposure-option-selected").value = newValue;
          }

          if (document.getElementsByClassName("response-option-confirm")[0]) {
            document.getElementsByClassName("response-option-confirm")[0].disabled = newValue === null;
          }
        },
        immediate: true
      },
      exposures: {
        handler(newValue) {
          if (newValue.length === 1) {
            // only one exposure, pre-select it for the user
            this.selectedExposureOption = newValue[0].exposureId;
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

#move-radio-group {
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
  & .exposure-radio-option-label {
    margin-left: 8px !important;
    font-weight: 400 !important;
  }
  & .exposure-radio-option {
    min-width: 80%;
    margin: 8px auto;
  }
}
</style>
