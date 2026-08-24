<template>
  <div>
    <h1>Select the default condition for your experiment</h1>

    <p>
      This is the condition students will receive if they do not consent to
      participate in the experiment or you mark them to be excluded.
    </p>

    <form
      v-if="experiment"
      class="my-5 mx-auto"
      @submit.prevent="saveConditions"
    >
      <fieldset
        v-if="experiment.conditions"
        class="rounded-lg p-5 mb-7"
      >
        <label
          v-for="condition in experiment.conditions"
          :key="condition.conditionId"
          :for="`condition-${condition.conditionId}`"
        >
          <span>{{ condition.name }}</span>

          <span class="radio-check">
            <input
              :id="`condition-${condition.conditionId}`"
              v-model="selectedDefault"
              :value="condition.conditionId"
              type="radio"
              name="selectedDefault"
              class="radio-default-condition"
              required
              @change="saveConditions"
            />

            <span
              class="rounded-pill px-3 py-1"
              :class="{
                'is-selected-default':
                  selectedDefault === condition.conditionId
              }"
            >
              <v-icon
                v-show="selectedDefault === condition.conditionId"
              >
                mdi-check
              </v-icon>

              <span>Default</span>
            </span>
          </span>
        </label>
      </fieldset>

      <v-btn
        v-if="!editMode"
        :disabled="!selectedDefault"
        :to="{
          name: getNextPage,
          params: {
            experiment: experiment.experimentId
          }
        }"
        elevation="0"
        color="primary"
        class="mr-4"
      >
        Next
      </v-btn>
    </form>

    <v-card
      class="mt-15 pt-5 px-5 mx-auto bg-blue-lighten-5 rounded-lg"
      variant="outlined"
    >
      <p>
        <strong>Note:</strong>
        It's important to specify a default condition so that we know which
        version of components students not participating in the experiment
        should receive. This condition should be the one closest to the sort of
        component students would complete during the normal conduct of the
        course.
      </p>
    </v-card>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import { useRouter } from "vue-router";

import { condition as conditionModule } from "@/store/condition.module";
import { navigation as navigationModule } from "@/store/navigation.module";

defineOptions({
  name: "DefaultCondition"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const conditionStore = conditionModule();
const navigationStore = navigationModule();

const inputConditionId = ref(null);

const editMode = computed(() => {
  return navigationStore.editMode;
});

const selectedDefault = computed({
  get() {
    const defaultCondition = props.experiment?.conditions.find(
      condition => condition.defaultCondition === true
    );

    if (inputConditionId.value !== null) {
      return inputConditionId.value;
    }

    if (defaultCondition) {
      return defaultCondition.conditionId;
    }

    return false;
  },

  set(value) {
    inputConditionId.value = value;
  }
});

const getNextPage = computed(() => {
  return editMode.value?.callerPage?.name || "ExperimentDesignSummary";
});

const getSaveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "Home";
});

const saveConditions = async () => {
  const conditions = props.experiment.conditions;
  const defaultConditionId = selectedDefault.value;

  await conditionStore.setDefaultCondition({
    conditions,
    defaultConditionId
  });
};

const saveExit = async () => {
  await saveConditions();

  router.push({
    name: getSaveExitPage.value,
    params: {
      experiment: props.experiment.experimentId
    }
  });
};

defineExpose({
  saveExit
});
</script>

<style lang="scss" scoped>
fieldset {
  padding: 10px 15px 10px 20px;
  border: 1px solid map.get($grey, "lighter");

  > label {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    font-size: 16px;

    &:not(:last-child) {
      border-bottom: 1px solid map.get($grey, "lighter");
    }

    > span {
      display: block;
      padding: 15px 0;
    }
  }
}

.radio-check {
  font-size: 14px;
  cursor: pointer;

  input[type="radio"] {
    opacity: 0;
    filter: alpha(opacity=0);
    position: absolute;

    + span {
      display: block;
      background-color: map.get($grey, "lighter");
      padding: 15px 0;
    }

    &:checked {
      + span {
        background-color: map.get($blue, "base");
        color: white;

        .v-icon {
          color: white;
          margin-right: 5px;
          font-size: 18px;
          vertical-align: text-bottom;
        }
      }
    }

    &:focus-visible {
      + span {
        border: 1px solid orangered;
      }
    }
  }

  & .is-selected-default {
    background-color: map.get($blue, "primary") !important;
  }
}
</style>