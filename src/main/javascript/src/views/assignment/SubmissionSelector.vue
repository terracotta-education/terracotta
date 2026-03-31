<template>
<v-select
  v-model="activeSubmissionId"
  :items="selectableSubmissions"
  label="Submissions"
  item-text="label"
  item-value="value"
  class="select-submissions"
  tabindex="0"
  outlined
  hide-details
></v-select>
</template>

<script>
import { mapGetters } from "vuex";
import { deleteAttributesFromElement, addAttributesToElement, getAttributeFromElement } from "@/helpers/ui-utils.js";

export default {
  name: "SubmissionSelector",
  props: {
    submissions: {
      type: Array,
      required: true
    }
  },
  emits: ["select"],
  data: () => ({
    activeSubmissionId: null
  }),
  watch: {
    activeSubmissionId: {
      handler(newValue) {
        this.$emit("select", newValue);
      }
    },
    allSubmissions: {
      handler() {
        const latestSubmission = this.orderedSubmissions[0];
        this.activeSubmissionId = latestSubmission?.submissionId;
      }
    }
  },
  computed: {
    ...mapGetters({
    }),
    allSubmissions() {
      return this.submissions;
    },
    activeSubmission() {
      return this.submissions.find(s => s.submissionId === this.activeSubmissionId);
    },
    selectableSubmissions() {
      return this.orderedSubmissions.map((s, idx) => ({value: s.submissionId, label: `Attempt ${this.submissions.length - idx}`}))
    },
    orderedSubmissions() {
      return [...this.allSubmissions].sort((a, b) => a.dateSubmitted - b.dateSubmitted).reverse();
    }
  },
  mounted () {
    const latestSubmission = this.orderedSubmissions[0];
    this.activeSubmissionId = latestSubmission?.submissionId;
    this.$nextTick(() => {
      // ensure aria-controls is added to the correct submission select element after loading
      const ariaOwnsId = getAttributeFromElement(".v-select.select-submissions .v-input__slot:first-of-type", "aria-owns");
      deleteAttributesFromElement(".v-select.select-submissions .v-input__slot", ["role"]);
      addAttributesToElement(".v-select.select-submissions .v-input__slot", [
        { name: "role", value: "combobox" },
        { name: "aria-controls", value: ariaOwnsId }
      ]);
    });
  }
}
</script>
