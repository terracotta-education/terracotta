<template>
  <div>
    <h1>Name your conditions</h1>
    <p>These will be used to label the different experimental versions of your assignments.</p>
    <v-form
      @submit.prevent="saveConditions('ExperimentDesignType')"
      class="my-5 mb-15"
      ref="conditionsForm"
    >
      <v-container class="pa-0">
        <v-row
            v-for="(condition, i) in conditions"
            :key="condition.conditionId"
        >
          <template
            v-if="i < 2"
          >
            <v-col class="py-0">
              <v-text-field
                  v-model="condition.name"
                  :name="'condition-' + condition.conditionId"
                  :rules="[duplicateRule(condition), requiredRule(condition), maxLengthRule(condition)]"
                  label="Condition name"
                  placeholder="e.g. Condition Name"
                  outlined
                  required
              ></v-text-field>
            </v-col>
          </template>
          <template
            v-else
          >
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
              v-if="deleteAllowed"
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
  </div>
</template>

<script>
import {mapActions, mapGetters} from 'vuex';
import store from '@/store';

export default {
  name: 'DesignConditions',
  props: ['experiment'],
  data() {
    return {
      fieldErrors: {
        duplicateName: {conditionIds: [], message: "Multiple conditions have the same name."},
        requiredName: {conditionIds: [], message: "A name is required for each condition."},
        maxLengthName: {conditionIds: [], message: "A maximum of 255 characters is allowed for condition names."}
      }
    }
  },
  computed: {
    ...mapGetters({
      editMode: 'navigation/editMode'
    }),
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || 'Home';
    },
    addAllowed() {
      return !this.editMode && this.conditions.length < 16;
    },
    deleteAllowed() {
      return this.experiment.exposureType === 'NOSET';
    },
    conditions() {
      return this.experiment.conditions;
    },
    errorMessage() {
      if (this.fieldErrors.duplicateName.conditionIds.length) {
        return this.fieldErrors.duplicateName.message;
      }
      if (this.fieldErrors.requiredName.conditionIds.length) {
        return this.fieldErrors.requiredName.message;
      }
      if (this.fieldErrors.maxLengthName.conditionIds.length) {
        return this.fieldErrors.maxLengthName.message;
      }
      return "Unspecified error."
    },
    countErrors() {
      var count = 0;
      Object.keys(this.fieldErrors).forEach((fe) => count += this.fieldErrors[fe].conditionIds.length);
      return count;
    },
    hasFieldErrors() {
      return this.countErrors > 0;
    }
  },
  methods: {
    ...mapActions({
      createCondition: 'condition/createCondition',
      deleteCondition: 'condition/deleteCondition',
      updateConditions: 'condition/updateConditions',
    }),
    async createNewCondition() {
      await this.createCondition({name: "", experiment_experiment_id: this.experiment.experimentId});
      this.$refs.conditionsForm.validate();
    },
    async saveConditions(path) {
      if (this.hasFieldErrors) {
        this.$swal('There was an error saving your conditions. ' + this.errorMessage);
        return;
      }
      const e = this.experiment
      e.conditions.experimentId = this.experiment.experimentId
      await this.updateConditions(e.conditions)
          .then(response => {
            if (response.status === 200) {
              // IF all responses return STATUS 200
              this.$router.push({name: path, params: {experiment: this.experiment.experimentId}})
            } else {
              this.$swal('There was an error saving your conditions.')
            }
          })
          .catch(response => {
            console.log("updateConditions | catch", {response})
            this.$swal('There was an error saving your conditions.')
          })
    },
    async handleDeleteCondition(condition) {
      const {defaultCondition} = condition;
      if (defaultCondition) {
        this.$swal('You are attempting to delete the default condition. You must set one of the other existing conditions as the default before deleting this condition.');
        return;
      } else {
        if (condition?.conditionId) {
          const reallyDelete = await this.$swal({
            icon: 'question',
            text: `Do you really want to delete "${condition.name}"?`,
            showCancelButton: true,
            confirmButtonText: 'Yes, delete it',
            cancelButtonText: 'No, cancel',
          })
          if (reallyDelete.isConfirmed) {
            try {
              this.deleteCondition(condition)
            } catch (error) {
              this.$swal({
                text: 'Could not delete condition.',
                icon: 'error'
              })
            }
          }
        }
      }
    },
    async saveExit() {
      if (this.conditions.every((c) => !(c.name && c.name.trim()))) {
        // all conditions are blank, exit w/o saving
        this.$router.push({
          name: this.getSaveExitPage,
          params: {
            experiment: this.experiment.experimentId
          }
        });
        return;
      }
      if (this.hasFieldErrors) {
        this.$swal("There was an error saving your conditions. " + this.errorMessage);
        this.$refs.conditionsForm.validate();
        return;
      }
      this.saveConditions(this.getSaveExitPage);
    },
    duplicateRule(condition) {
      this.handleRule(this.fieldErrors.duplicateName, condition, this.conditions.some((c) => c.conditionId !== condition.conditionId && condition.name && c.name === condition.name));
      return !this.fieldErrors.duplicateName.conditionIds.includes(condition.conditionId) || "Duplicate condition name.";
    },
    requiredRule(condition) {
      this.handleRule(this.fieldErrors.requiredName, condition, !(condition.name && condition.name.trim()));
      return !this.fieldErrors.requiredName.conditionIds.includes(condition.conditionId) || "Condition name is required";
    },
    maxLengthRule(condition) {
      this.handleRule(this.fieldErrors.maxLengthName, condition, (condition.name || '').length > 255);
      return !this.fieldErrors.maxLengthName.conditionIds.includes(condition.conditionId) || "A maximum of 255 characters is allowed";
    },
    handleRule(fieldError, condition, hasError) {
      if (hasError) {
        if (!fieldError.conditionIds.includes(condition.conditionId)) {
          fieldError.conditionIds.push(condition.conditionId);
        }
      } else {
        var idx = fieldError.conditionIds.indexOf(condition.conditionId);
        if (idx !== -1) {
          fieldError.conditionIds.splice(idx);
        }
      }
    }
  },
  beforeRouteEnter(to, from, next) {
    if (store.state.experiment.experiment.conditions.length < 2) {
      store.dispatch('condition/createDefaultConditions', to.params.experiment_id).then(() => next())
    } else {
      next()
    }
  },
  beforeRouteUpdate(to, from, next) {
    if (store.state.experiment.experiment.conditions.length < 2) {
      store.dispatch('condition/createDefaultConditions', to.params.experiment_id).then(() => next())
    } else {
      next()
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
