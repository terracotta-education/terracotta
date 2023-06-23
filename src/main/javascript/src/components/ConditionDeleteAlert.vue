<template>
  <div id="alert-delete-condition-template">
    <template v-if="singleConditionRemainsAfterDelete">
      <h3>Are you sure you want to delete {{ calculateConditionName }}?</h3>
      <p>
        If you delete this, your experiment will have only one condition, so you won't be able to make comparisons between conditions.
        If you continue, all students will experience the same thing. Do you want to move forward with only one condition?
      </p>

      <v-tooltip
        top
        contained="true"
        attach="#alert-delete-condition-template"
      >
        <template v-slot:activator="{ on, attrs }">
          <a
            v-bind="attrs"
            v-on="on"
          >
            Why would I want only one condition?
          </a>
        </template>
        <span>
          <h4><strong class="d-block">Why would I want only one condition?</strong></h4>
          <p>
            You may want only one condition if you're using Terracotta to collect data and/or informed consent,
            and want all participants in the experiment to complete the same assignment (e.g., a survey).
          </p>
        </span>
      </v-tooltip>
    </template>
    <template v-else>
      <h3>Do you really want to delete {{ calculateConditionName }}?</h3>
    </template>
  </div>
</template>

<script>
  export default {
    name: "ConditionDeleteAlert",
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

        return '"' + this.conditionName + '"';
      }
    }
  }
</script>

<style lang="scss" scoped>
div.swal2-container {
  > div.swal2-popup {
    width: fit-content !important;
    max-width: 52em !important;
    > .swal2-html-container {
      h3 {
        text-align: center !important;
      }
      a,
      p {
        display: block !important;
        text-align: left !important;
      }
      a {
        display: flex;
        width: fit-content;
        color: #0077d2;
      }
      div#alert-delete-condition {
        div#alert-delete-condition-template {
          > div.v-tooltip__content {
            width: fit-content;
            max-width: 90%;
            margin: 0 auto;
            left: 0 !important;
            right: 0 !important;
            opacity: 1.0 !important;
            background-color: rgba(55,61,63, 1.0) !important;
            p {
              text-align: left;
            }
          }
        }
      }
    }
  }
}
</style>
