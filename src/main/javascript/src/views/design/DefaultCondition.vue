<template>
  <div>
    <h1>Select the default condition for your experiment</h1>
    <p>This is the condition students will receive if they do not consent to participate in the experiment or you mark
      them to be excluded.</p>

    <form
      @submit.prevent="saveConditions"
      class="my-5"
      v-if="experiment"
    >
      <fieldset class="rounded-lg p-5 mb-7" v-if="experiment.conditions">
        <label v-for="condition in experiment.conditions" :key="condition.conditionId"
               :for="`condition-${condition.conditionId}`">
          <span>{{ condition.name }}</span>
          <span class="radio-check">
						<input type="radio" name="selectedDefault" v-model="selectedDefault" :value="condition.conditionId"
                   :id="`condition-${condition.conditionId}`" @change="saveConditions" required/>
						<span class="rounded-pill px-3 py-1">
							<v-icon v-show="selectedDefault === condition.conditionId">mdi-check</v-icon>
							<span>Default</span>
						</span>
					</span>
        </label>
      </fieldset>

      <v-btn
        :disabled="!selectedDefault"
        :to="{name: 'ExperimentDesignSummary', params:{experiment: experiment.experimentId}}"
        elevation="0"
        color="primary"
        class="mr-4"
      >
        Next
      </v-btn>
    </form>

    <v-card
      class="mt-15 pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
      outlined
    >
      <p><strong>Note:</strong> It's important to specify a default condition so that we know what version of
        assignments students should receive if they're not participating in the experiment. This condition should be
        similar to business-as-usual; the sort of assignment that students would complete during the normal conduct of
        the course.</p>
    </v-card>
  </div>
</template>

<script>
import {mapActions} from "vuex";

export default {
  name: 'DefaultCondition',
  props: ['experiment'],
  data() {
    return {
      inputConditionId: null
    }
  },
  computed: {
    selectedDefault: {
      get() {
        const defaultCondition = this.experiment?.conditions.find(condition => condition.defaultCondition === true)

        if (this.inputConditionId !== null) {
          return this.inputConditionId
        } else if (defaultCondition) {
          return defaultCondition.conditionId
        } else {
          return true
        }
      },
      set(val) {
        this.inputConditionId = val
      }
    }
  },
  methods: {
    ...mapActions({
      setDefaultCondition: 'condition/setDefaultCondition',
    }),
    saveConditions() {
      const conditions = this.experiment.conditions
      const defaultConditionId = this.selectedDefault

      this.setDefaultCondition({conditions, defaultConditionId})
          .catch(response => {
            console.log("catch", {response})
          })
    },
  }
}
</script>

<style lang="scss" scoped>
@import '~@/styles/variables';

fieldset {
  padding: 10px 15px 10px 20px;
  border: 1px solid map-get($grey, 'lighten-2');

  > label {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    font-size: 16px;

    &:not(:last-child) {
      border-bottom: 1px solid map-get($grey, 'lighten-2');
    }

    > span {
      display: block;
      padding: 15px 0;
    }
  }
}

.radio-check {
  font-size: 14px;

  input[type="radio"] {
    display: none;

    + span {
      display: block;
      background-color: map-get($grey, 'lighten-2');
      padding: 15px 0;
    }

    &:checked {
      + span {
        background-color: map-get($light-blue, 'base');
        color: white;

        .v-icon {
          color: white;
          margin-right: 5px;
          font-size: 18px;
          vertical-align: text-bottom;
        }
      }
    }
  }
}
</style>