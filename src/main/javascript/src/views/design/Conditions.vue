<template>
  <div>
    <h1>Name your conditions</h1>
    <p>These will be used to label the different experimental versions of your assignments.</p>
    <v-form
      @submit.prevent="saveConditions(nextPage, true)"
      class="my-5 mb-15"
      ref="conditionsForm"
    >
      <v-container class="pa-0">
        <v-row
            v-for="(condition, i) in conditions"
            :key="condition.conditionId"
        >
          <template>
            <v-col
              class="py-0"
            >
              <v-text-field
                  v-model="condition.name"
                  :name="'condition-' + condition.conditionId"
                  :rules="[duplicateRule(condition), requiredRule(condition), maxLengthRule(condition)]"
                  label="Condition name"
                  placeholder="e.g. Condition Name"
                  outlined
                  required
              >
              </v-text-field>
            </v-col>
            <v-col
              v-if="deleteAllowed && i > 0"
              class="py-0"
              cols="4"
              sm="2"
            >
              <v-btn
                icon
                outlined
                tile
                class="delete_condition"
                @click="handleDeleteCondition(condition)"
              >
                <v-icon>mdi-delete</v-icon>
              </v-btn>
            </v-col>
          </template>
        </v-row>
      </v-container>

      <div
        v-if="addAllowed"
      >
        <v-btn
          v-if="experiment.conditions.length < 16"
          @click="createNewCondition()"
          color="blue"
          class="add_condition px-0 mb-10"
          text
        >
          Add another condition
        </v-btn>
        <v-alert
          v-else
          type="error"
        >
          You have reached the maximum number of conditions (16) allowed by the experiment builder.
        </v-alert>
      </div>

      <v-btn
        :disabled="hasFieldErrors"
        elevation="0"
        color="primary"
        class="mr-4"
        type="submit"
      >
        Next
      </v-btn>
    </v-form>
    <v-card
      v-if="singleConditionExperiment && deleteAllowed"
      class="mt-15 pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
      outlined
    >
      <p>
        Once you click NEXT to leave this screen, you will be able to add, but not delete conditions,
        so please double-check that you have included what you need. To change your decisions beyond this point,
        you will need to create a new experiment.
      </p>
    </v-card>
    <v-card
      v-if="!deleteAllowed"
      class="mt-15 pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
      outlined
    >
      <p>
        Please note that you are not able to {{ !addAllowed ? "add or" : "" }} delete conditions,
        as you have previously completed {{ !addAllowed ? "your experiment design and participation settings" : "this section"}}.
        To {{ !addAllowed ? "add or" : "" }} delete conditions, please create a new experiment.
      </p>
    </v-card>
  </div>
</template>

<script>
import {mapActions, mapGetters} from 'vuex';
import ConditionDeleteAlert from '@/components/ConditionDeleteAlert';
import store from '@/store';
import Vue from 'vue';

export default {
  name: 'DesignConditions',
  props: {
    experiment: {}
  },
  data() {
    return {
      deleteConditionName: null,
      fieldErrors: {
        duplicateName: {conditionIds: [], message: "Multiple conditions have the same name."},
        requiredName: {conditionIds: [], message: "A name is required for each condition."},
        maxLengthName: {conditionIds: [], message: "A maximum of 255 characters is allowed for condition names."}
      },
      hasFieldErrors: false
    }
  },
  watch: {
    conditions: {
      handler: function() {
        this.clearFieldErrors();
        this.$refs.conditionsForm.validate();
      },
      deep: true
    },
    duplicateName: {
      handler: function() {
        this.hasFieldErrors = this.calculateFieldErrors();
      },
      deep: true
    },
    requiredName: {
      handler: function() {
        this.hasFieldErrors = this.calculateFieldErrors();
      },
      deep: true
    },
    maxLengthName: {
      handler: function() {
        this.hasFieldErrors = this.calculateFieldErrors();
      },
      deep: true
    }
  },
  computed: {
    ...mapGetters({
      editMode: 'navigation/editMode'
    }),
    saveExitPage() {
      return this.editMode?.callerPage?.name || 'Home';
    },
    nextPage() {
      return this.singleConditionExperiment ? "ExperimentDesignSummary" : "ExperimentDesignType";
    },
    addAllowed() {
      return !this.editMode;
    },
    deleteAllowed() {
      return this.experiment.exposureType === 'NOSET';
    },
    conditions() {
      return this.experiment.conditions;
    },
    singleConditionRemainsAfterDelete() {
      return this.conditions.length === 2;
    },
    singleConditionExperiment() {
      return this.conditions.length === 1;
    },
    experimentId() {
      return this.experiment.experimentId;
    },
    duplicateName() {
      return this.fieldErrors.duplicateName.conditionIds;
    },
    requiredName() {
      return this.fieldErrors.requiredName.conditionIds;
    },
    maxLengthName() {
      return this.fieldErrors.maxLengthName.conditionIds;
    },
    errorMessage() {
      if (this.duplicateName.length) {
        return this.fieldErrors.duplicateName.message;
      }

      if (this.requiredName.length) {
        return this.fieldErrors.requiredName.message;
      }

      if (this.maxLengthName.length) {
        return this.fieldErrors.maxLengthName.message;
      }

      return "Unspecified error."
    }
  },
  methods: {
    ...mapActions({
      createCondition: "condition/createCondition",
      createExposures: "exposures/createExposures",
      createAndAssignGroups: "groups/createAndAssignGroups",
      deleteCondition: "condition/deleteCondition",
      reportStep: "api/reportStep",
      updateConditions: "condition/updateConditions",
      updateExperiment: "experiment/updateExperiment"
    }),
    async createNewCondition() {
      var doAdd = this.deleteAllowed;

      if (!this.deleteAllowed) {
        const reallyAdd = await this.$swal({
          icon: 'question',
          text: `Do you really want to add a new condition? You will not be able to delete it.`,
          showCancelButton: true,
          confirmButtonText: 'Yes, add it',
          cancelButtonText: 'No, cancel',
        });
        doAdd = reallyAdd.isConfirmed;
      }

      if (doAdd) {
        await this.createCondition({name: "", experiment_experiment_id: this.experimentId});
        this.$refs.conditionsForm.validate();
      }
    },
    async saveConditions(path, updateExperiment) {
      if (this.hasFieldErrors) {
        this.$swal('There was an error saving your conditions. ' + this.errorMessage);
        return;
      }

      const e = this.experiment
      e.conditions.experimentId = this.experimentId

      if (this.singleConditionExperiment) {
        // only one condition; set as default
        this.conditions[0].defaultCondition = true;
      }

      await this.updateConditions(e.conditions)
        .then(async response => {
          if (response.status === 200) {
            if (this.singleConditionExperiment && updateExperiment) {
              // only one condition; save experiment defaults for distribution (BETWEEN) and exposure types (EVEN)
              await this.updateConditionExperiment("BETWEEN", "EVEN", "exposure_type");
            }

            this.$router.push({
              name: path,
              params: {
                experiment: this.experimentId
              }
            });
          } else {
            this.$swal('There was an error saving your conditions.');
          }
        }).catch(response => {
          console.log("updateConditions | catch", {response});
          this.$swal('There was an error saving your conditions.');
        });
    },
    async updateConditionExperiment(exposureType, distributionType, calculatedStep) {
      const e = this.experiment;
      e.exposureType = exposureType;
      e.distributionType = distributionType;
      const experimentId = e.experimentId;
      const step = calculatedStep;

      await this.updateExperiment(e)
        .then(
          async response => {
            if (typeof response?.status !== "undefined" && response?.status === 200) {
              if (!this.editMode) {
                // create the exposures here, as the user cannot for single conditions
                await this.createExposures(experimentId);
                // create and assign exposure groups
                await this.createAndAssignGroups(experimentId);
                // report the current step as exposure type, as we are skipping exposure selection
                await this.reportStep({experimentId, step});
              }
            } else if (response?.message) {
              this.$swal(`Error: ${response.message}`);
            } else {
              this.$swal("There was an error saving your experiment.");
            }
          }
        ).catch(
          response => {
            console.error("updateExperiment | catch", {response});
            this.$swal("There was an error saving the experiment.");
          }
        )
    },
    async handleDeleteCondition(condition) {
      const {defaultCondition} = condition;

      if (defaultCondition) {
        this.$swal('You are attempting to delete the default condition. You must set one of the other existing conditions as the default before deleting this condition.');
        return;
      } else {
        if (condition?.conditionId) {
          this.deleteConditionName = condition.name;
          var reallyDelete = await this.displayDeleteConditionDialog(condition.name);

          if (reallyDelete.isConfirmed) {
            try {
              Object.keys(this.fieldErrors).forEach((fe) => this.handleRule(this.fieldErrors[fe], condition.conditionId, false));
              await this.deleteCondition(condition);
            } catch (error) {
              this.$swal({
                text: 'Could not delete condition.',
                icon: 'error'
              });
            }
          }
        }
      }
    },
    async displayDeleteConditionDialog(conditionName) {
      return this.$swal({
        icon: 'question',
        html: '<div id="alert-delete-condition"></div>',
        showCancelButton: true,
        confirmButtonText: 'Yes, delete it',
        cancelButtonText: 'No, cancel',
        willOpen: () => {
          var ConditionDeleteAlertClass = Vue.extend(ConditionDeleteAlert);
          var conditionDeleteAlert = new ConditionDeleteAlertClass({
            propsData: {
              singleConditionRemainsAfterDelete: this.singleConditionRemainsAfterDelete,
              conditionName: conditionName
            }
          });
          conditionDeleteAlert.$mount();
          document.getElementById("alert-delete-condition").appendChild(conditionDeleteAlert.$el);
        }
      });
    },
    async saveExit() {
      if (this.conditions.every((c) => !(c.name && c.name.trim()))) {
        // all conditions are blank, exit w/o saving
        this.$router.push({
          name: this.saveExitPage,
          params: {
            experiment: this.experimentId
          }
        });
        return;
      }

      if (this.hasFieldErrors) {
        this.$swal("There was an error saving your conditions. " + this.errorMessage);
        this.$refs.conditionsForm.validate();
        return;
      }

      this.saveConditions(this.saveExitPage, this.experiment.exposureType !== "NOSET" && this.experiment.distributionType !== "NOSET");
    },
    duplicateRule(condition) {
      this.handleRule(
        this.fieldErrors.duplicateName,
        condition.conditionId,
        this.conditions.some(
          (c) =>
            c.conditionId !== condition.conditionId &&
            condition.name && c.name &&
            c.name.replace(/\s\s+/g, ' ').toLowerCase().trim() === condition.name.replace(/\s\s+/g, ' ').toLowerCase().trim()
        )
      );
      return !this.fieldErrors.duplicateName.conditionIds.includes(condition.conditionId) || "Duplicate condition name.";
    },
    requiredRule(condition) {
      this.handleRule(this.fieldErrors.requiredName, condition.conditionId, !(condition.name && condition.name.replace(/\s\s+/g, ' ').trim()));
      return !this.fieldErrors.requiredName.conditionIds.includes(condition.conditionId) || "Condition name is required";
    },
    maxLengthRule(condition) {
      this.handleRule(this.fieldErrors.maxLengthName, condition.conditionId, (condition.name || '').length > 255);
      return !this.fieldErrors.maxLengthName.conditionIds.includes(condition.conditionId) || "A maximum of 255 characters is allowed";
    },
    handleRule(fieldError, conditionId, hasError) {
      if (hasError) {
        if (!fieldError.conditionIds.includes(conditionId)) {
          fieldError.conditionIds.push(conditionId);
        }
      } else {
        var idx = fieldError.conditionIds.indexOf(conditionId);

        if (idx !== -1) {
          fieldError.conditionIds.splice(idx);
        }
      }
    },
    calculateFieldErrors() {
      return Object.keys(this.fieldErrors).some((fe) => this.fieldErrors[fe].conditionIds.length > 0);
    },
    clearFieldErrors() {
      Object.keys(this.fieldErrors).forEach((fe) => this.fieldErrors[fe].conditionIds = []);
    }
  },
  beforeRouteEnter(to, from, next) {
    if (store.state.experiment.experiment.conditions.length == 0) {
      store.dispatch('condition/createDefaultConditions', to.params.experiment_id).then(() => next());
    } else {
      next();
    }
  },
  beforeRouteUpdate(to, from, next) {
    if (store.state.experiment.experiment.conditions.length == 0) {
      store.dispatch('condition/createDefaultConditions', to.params.experiment_id).then(() => next());
    } else {
      next();
    }
  },
  mounted() {
    this.$refs.conditionsForm.validate();
  }
}
</script>

<style lang="scss" scoped>
.add_condition {
  text-transform: unset !important;
}
.delete_condition {
  border-radius: 4px;
  width: 100%;
  height: 56px;
}
</style>
