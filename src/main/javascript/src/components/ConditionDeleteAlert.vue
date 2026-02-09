<template>
<div
  id="alert-delete-condition-template"
>
  <template
    v-if="singleConditionRemainsAfterDelete"
  >
    <h3>Are you sure you want to delete {{ calculateConditionName }}?</h3>
    <p>
      If you delete this, your experiment will have only one condition, so you won't be able to make comparisons between conditions.
      If you continue, all students will experience the same thing. Do you want to move forward with only one condition?
    </p>
    <tool-tip
      header="Why would I want only one condition?"
      content="You may want only one condition if you're using Terracotta to collect data and/or informed consent,
          and want all participants in the experiment to complete the same assignment (e.g., a survey)."
      ariaLabel="Only one condition will remain explanation tooltip"
      alignment="top"
      activatorType="link"
      activatorContent="Why would I want only one condition?"
      activatorClass="tool-tip-link"
      contained="true"
      attach="#alert-delete-condition-template"
    />
  </template>
  <template
    v-else
  >
    <h3>Do you really want to delete {{ calculateConditionName }}?</h3>
  </template>
</div>
</template>

<script>
import ToolTip from "@/components/ToolTip.vue";

export default {
  name: "ConditionDeleteAlert",
  components: {
    ToolTip
  },
  props: {
    singleConditionRemainsAfterDelete: {
      type: Boolean
    },
    conditionName: {
      type: String
    }
  },
  computed: {
    calculateConditionName() {
      if (!this.conditionName) {
        return "this condition";
      }

      return `"${this.conditionName}"`;
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/styles/variables";

div.swal2-container {
  > div.swal2-popup {
    width: fit-content !important;
    max-width: 52em !important;
    > .swal2-html-container {
      h3 {
        text-align: left !important;
      }
      a,
      p {
        display: block !important;
        text-align: left !important;
      }
      & #alert-delete-condition-template::v-deep {
        & .tool-tip-link {
          display: flex;
          width: fit-content;
          color: map-get($blue, "base");
        }
      }
    }
  }
}
</style>
