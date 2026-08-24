<template>
  <div>
    <v-row v-if="canTryAgain">
      <v-col>
        <v-btn
          elevation="0"
          color="primary"
          class="mt-4 mb-2"
          type="button"
          @click="$emit('try-again')"
        >
          Try Again
        </v-btn>
        <p>
          <span v-if="scoringScheme === 'HIGHEST'">The highest</span>
          <span v-else-if="scoringScheme === 'MOST_RECENT'">The most recent</span>
          <span v-else-if="scoringScheme === 'AVERAGE'">The average</span>
          <span v-else-if="scoringScheme === 'CUMULATIVE'">A cumulative</span>
          score will be kept
        </p>
      </v-col>
    </v-row>

    <v-row v-if="cantTryAgainMessage">
      <v-col>
        <v-card
          class="pt-5 px-5 mx-auto rounded-lg card-warning"
          variant="outlined"
        >
          <p
            v-if="cantTryAgainMessage === 'MAX_NUMBER_ATTEMPTS_REACHED'"
            class="pb-0"
          >
            You have reached the maximum number of attempts for this assignment.
          </p>
          <p
            v-if="cantTryAgainMessage === 'WAIT_TIME_NOT_REACHED'"
            class="pb-0"
          >
            Wait time not reached... You must wait a period of time before submitting again.
          </p>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script setup>
defineProps({
  canTryAgain: { type: Boolean, default: false },
  cantTryAgainMessage: { type: String, default: null },
  scoringScheme: { type: String, default: null }
});

defineEmits(["try-again"]);
</script>

<style lang="scss" scoped>
// bg-yellow-lighten-5 (a Vuetify color utility class) generates no CSS in this
// project's build - see ExperimentType.vue's .card-warning for the full explanation.
// Vuetify's own yellow-lighten-5 (#fffde7) doesn't have a project $yellow map
// equivalent (the project's $yellow is a different, darker/amber shade), so use the
// literal palette value directly.
.card-warning {
  background-color: #fffde7;
}
</style>
