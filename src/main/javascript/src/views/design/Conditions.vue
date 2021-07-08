<template>
  <div>
    <h1>Name your conditions</h1>
    <p>These will be used to label the different experimental versions of your assignments.</p>
    <form
      @submit.prevent="saveConditions('ExperimentDesignType')"
      class="my-5 mb-15"
      v-if="experiment"
    >

      <v-container class="pa-0">
        <v-row
          v-for="(condition, i) in experiment.conditions"
          :key="condition.conditionId"
        >
          <template v-if="i < 2">
            <v-col class="py-0">
              <v-text-field
                v-model="condition.name"
                :name="'condition-'+condition.conditionId"
                :rules="rules"
                label="Condition name"
                placeholder="e.g. Condition Name"
                outlined
                required
              ></v-text-field>
            </v-col>
          </template>
          <template v-else>
            <v-col class="py-0">
              <v-text-field
                v-model="condition.name"
                :name="'condition-'+condition.conditionId"
                :rules="rules"
                label="Condition name"
                placeholder="e.g. Condition Name"
                outlined
                required
              ></v-text-field>
            </v-col>
            <v-col class="py-0" cols="4" sm="2">
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

      <div>
        <v-btn
          @click="createCondition({name:'',experiment_experiment_id:experiment.experimentId})"
          color="blue"
          class="add_condition px-0 mb-10"
          text
        >Add another condition
        </v-btn>
      </div>

      <v-btn
        :disabled="!experiment.conditions.length > 0 || !experiment.conditions.every(c => c.name && c.name.trim())"
        elevation="0"
        color="primary"
        class="mr-4"
        type="submit"
      >
        Next
      </v-btn>
    </form>


  </div>
</template>

<script>
import {mapActions} from "vuex";
import store from "@/store";

export default {
  name: 'DesignConditions',
  props: ['experiment'],
  data: () => ({
    rules: [
      v => v && !!v.trim() || 'Condition name is required',
      v => (v || '').length <= 255 || 'A maximum of 255 characters is allowed'
    ]
  }),

  methods: {
    ...mapActions({
      createCondition: 'condition/createCondition',
      deleteCondition: 'condition/deleteCondition',
      updateConditions: 'condition/updateConditions',
    }),
    async saveConditions(path) {
      const e = this.experiment

      await this.updateConditions(e.conditions)
          .then(response => {
            if (response?.every(obj => obj.status === 200)) {
              // IF all responses return STATUS 200
              this.$router.push({name: path, params: {experiment: this.experiment.experimentId}})
            } else if (response?.some(obj => Object.prototype.hasOwnProperty.call(obj, 'message'))) {
              // IF one response contains message -> alert with message
              alert(`Error: ${response.filter(obj => (typeof obj.message !== 'undefined'))[0].message}`)
            } else {
              alert('There was an error saving your conditions.')
            }
          })
          .catch(response => {
            console.log("updateConditions | catch", {response})
          })
    },
    handleDeleteCondition(condition) {
      const { defaultCondition } = condition;
      if (defaultCondition) {
        alert('You are attempting to delete the default condition. You must set one of the other existing conditions as the default before deleting this condition.')
      } else {
        this.deleteCondition(condition);
      }
    },
    async saveExit() {
        this.saveConditions('Home')
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