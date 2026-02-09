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

<script>
import { mapGetters } from "vuex";
  import Vuetify from 'vuetify/lib'

  export default {
    name: "RegradeAssignmentDialog",
    vuetify: new Vuetify(),
    props: {
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
    },
    data: () => ({
      selectedRegradeOption: null
    }),
    watch: {
      selectedRegradeOption: {
        handler(newValue) {
          if (document.getElementById("regrade-option-selected")) {
            document.getElementById("regrade-option-selected").value = newValue;
          }

          if (document.getElementsByClassName("response-option-confirm")[0]) {
            document.getElementsByClassName("response-option-confirm")[0].disabled = newValue === null;
          }
        },
        immediate: true
      }
    },
    computed: {
      ...mapGetters({
        configurations: "configuration/get"
      }),
      regradeOptions() {
        return [
          {value: "BOTH", label: "Award points for both corrected and previously correct answers (no scores will be reduced)"},
          {value: "CURRENT", label: "Only award points for the correct answer (some students' scores may be reduced)"},
          {value: "FULL", label: "Give everyone full credit for " + this.questionLabel},
          {value: "NONE", label: "Update " + this.questionLabel + " without regrading"}
        ];
      },
      studentCountLabel() {
        return this.studentCount + " student" + (this.studentCount > 1 ? "s" : "");
      },
      studentCountHaveLabel() {
        return this.studentCount > 1 ? "have" : "has";
      },
      questionLabel() {
        return this.editedQuestionCount === 1 ? "this question" : "the questions you've changed";
      },
      lmsTitle() {
        return this.configurations?.lmsTitle || "LMS";
      }
    }
  }
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
