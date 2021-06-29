<template>
  <div>
    <h1 class="mb-5">
      Select which students you would like for each condition.
    </h1>

    <!-- Conditions Section -->
    <p>Conditions</p>
    <v-expansion-panels class="v-expansion-panels--icon" flat>
      <v-expansion-panel
        v-for="(condition, index) in this.conditions"
        :key="condition.conditionId"
      >
        <v-expansion-panel-header>
          {{ condition.name }} ({{ arrayDataProxy[index].length }})
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <ListParticipants
            :listOfParticipants="arrayDataProxy[index]"
            :moveToOptions="getConditionNames"
            :moveToHandler="moveToHandler"
            :selectedOption="'' + index"
          />
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>

    <!-- Unassigned -->
    <ListParticipants
      :listOfParticipants="arrayDataProxy[this.getConditionNames.length - 1]"
      :moveToOptions="getConditionNames"
      :moveToHandler="moveToHandler"
      :selectedOption="'' + (this.getConditionNames.length - 1)"
    />

    <v-btn
      elevation="0"
      class="mt-10"
      color="primary"
      @click="submitDistribution"
      >Continue</v-btn
    >
  </div>
</template>

<script>
import ListParticipants from '../../../components/ListParticipants.vue'
import store from '@/store'
import { mapGetters, mapActions } from 'vuex'
import { participantService } from '@/services'

export default {
  name: 'ParticipationManualDistribution',
  props: ['experiment'],
  components: {
    ListParticipants,
  },
  data() {
    return {
      arrayDataProxy: [],
    };
  },
  watch: {
    participants: {
      // Watcher is required for keeping track of changes
      // made in Participants
      deep: true,
      immediate: true,
      handler(newValue) {
        // This will only required when the page is loaded
        const newArray = []
        for (let i = 0; i < this.conditions.length; i++) {
          newArray.push([])
        }
        // All the participant will go to 'Unparticipate' section
        newArray.push(newValue)
        this.arrayData = newArray
      },
    },
  },
  computed: {
    ...mapGetters({
      participants: 'participants/participants',
      exposures: 'exposures/exposures',
    }),

    conditions() {
      return this.experiment.conditions
    },

    getConditionNames() {
      return [
        ...this.experiment.conditions.map((condition) => condition.name),
        'Unassigned',
      ]
    },

    arrayData: {
      get: function() {
        const newArray = []
        for (let i = 0; i < this.conditions.length; i++) {
          newArray.push([])
        }
        // All the participant will go to 'Unparticipate' section
        newArray.push(this.participants)
        return newArray
      },
      set: function(newValue) {
        this.arrayDataProxy = newValue;
      },
    },
  },
  methods: {
    ...mapActions({
      fetchExposures: 'exposures/fetchExposures'
    }),

    getExposure() {
      this.fetchExposures(this.experiment.experimentId)
    },

    submitDistribution() {
      const requestBody = []
      const conditionGroupIDMap = {}

      this.getExposure()
      const firstExposureId = this.exposures
        .map((expo) => expo.exposureId)
        .sort((a, b) => a - b)[0]

      const firstExposure = this.exposures.filter(
        (expo) => expo.exposureId === firstExposureId
      )[0]

      firstExposure.groupConditionList.map(
        ({ groupId }, index) => (conditionGroupIDMap[index] = groupId)
      )

      this.arrayDataProxy.map((arrData, index) =>
        arrData.map((participant) => {
          if (conditionGroupIDMap[index] !== undefined) {
            const temp = {
              participantId: participant.participantId,
              consent: participant.consent,
              dropped: participant.dropped,
              groupId: conditionGroupIDMap[index],
            }
            requestBody.push(temp)
          }
        })
      )

      participantService
        .updateParticipants(this.experiment.experimentId, requestBody)
        .then((response) => {
          if (response?.status === 200) {
            this.$router.push({
              name: 'ParticipationSummary',
              params: { experiment: this.experiment.experimentId },
            });
          } else {
            alert(response.error)
          }
        })
        .catch((response) => {
          console.log('submitParticipants | catch', { response });
        })
    },

    moveToHandler(option, tempSelected) {
      const selectedParticipantIDs = tempSelected.map(
        (participant) => participant.participantId
      )

      const filteredParticipants = this.arrayDataProxy.map(
        (conditionParticipantMap) =>
          conditionParticipantMap.filter(
            (participant) =>
              !selectedParticipantIDs.includes(participant.participantId)
          )
      )

      const idx = this.getConditionNames.indexOf(option)
      filteredParticipants[idx] = [
        ...filteredParticipants[idx],
        ...tempSelected,
      ]

      this.arrayData = filteredParticipants
    },
  },
  beforeRouteEnter(to, from, next) {
    //  load participant data before selection screen
    return (
      store
        .dispatch("participants/fetchParticipants", to.params.experiment_id)
        .then(next, next)
    );
  },
  beforeRouteUpdate(to, from, next) {
    //  load participant data before selection screen
    return (
      store
        .dispatch("participants/fetchParticipants", to.params.experiment_id)
        .then(next, next)
    );
  },
};
</script>
