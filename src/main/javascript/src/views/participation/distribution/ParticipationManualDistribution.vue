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
          {{ condition.name }} ({{ arrayDataProxy[index] ? arrayDataProxy[index].length : 0 }})
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <ListParticipants
            :listOfParticipants="arrayDataProxy[index] ? arrayDataProxy[index] : []"
            :moveToOptions="getConditionNames"
            :moveToHandler="moveToHandler"
            :selectedOption="'' + index"
          />
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>

    <!-- Unassigned -->
    <ListParticipants
      :listOfParticipants="arrayDataProxy[this.getConditionNames.length - 1] ? arrayDataProxy[this.getConditionNames.length - 1] : []"
      :moveToOptions="getConditionNames"
      :moveToHandler="moveToHandler"
      :selectedOption="'' + (this.getConditionNames.length - 1)"
    />

    <v-btn
      elevation="0"
      class="mt-10"
      color="primary"
      @click="submitDistribution('ParticipationSummary')"
      >Continue</v-btn
    >
  </div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex'
import { participantService } from '@/services'
import ListParticipants from '../../../components/ListParticipants.vue'
import store from '@/store'

export default {
  name: 'ParticipationManualDistribution',
  props: ['experiment'],
  components: {
    ListParticipants,
  },
  data() {
    return {
      arrayDataProxy: []
    };
  },
  watch: {
    participants: {
      // Watcher is required for keeping track of changes made in Participants
      deep: true,
      handler() {
        // All the participant will go to 'Unparticipate' section
        const participatingStudents = this.participants.filter(({consent}) => consent === true)
        const conditionGroupIDMap = this.getConditionGroupIDMap()

        // This will only required when the page is loaded
        const newArray = []
        for (let i = 0; i < this.conditions.length; i++) {
          const studentsAssignedToCondition = participatingStudents.filter((student) => student.groupId === conditionGroupIDMap[i])
          newArray.push(studentsAssignedToCondition)
        }
        const unAssignedStudents = participatingStudents.filter((student) => student.groupId === null)
        newArray.push(unAssignedStudents)
        this.arrayData = newArray
      },
    },
  },
  computed: {
    ...mapGetters({
      participants: 'participants/participants',
      exposures: 'exposures/exposures',
      editMode: 'navigation/editMode'
    }),
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || 'Home';
    },
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
        this.arrayDataProxy = newValue
      },
    },
  },
  methods: {
    ...mapActions({
      fetchExposures: 'exposures/fetchExposures',
      fetchParticipants: 'participants/fetchParticipants'
    }),

    getExposure() {
      this.fetchExposures(this.experiment.experimentId);
    },

    getConditionGroupIDMap() {
      const conditionGroupIDMap = {};

      const firstExposureId = this.exposures
        .map((expo) => expo.exposureId)
        .sort((a, b) => a - b)[0];

      const firstExposure = this.exposures.filter(
        (expo) => expo.exposureId === firstExposureId
      )[0];

      firstExposure.groupConditionList.map(
        ({ groupId }, index) => (conditionGroupIDMap[index] = groupId)
      );

      return conditionGroupIDMap;
    },

    submitDistribution(path) {
      const requestBody = []
      const conditionGroupIDMap = this.getConditionGroupIDMap()

      this.arrayDataProxy.map((arrData, index) =>
        arrData.map((participant) => {
            const temp = {
              participantId: participant.participantId,
              consent: participant.consent,
              dropped: participant.dropped,
              groupId: conditionGroupIDMap[index] ? conditionGroupIDMap[index] : null,
            }
            requestBody.push(temp)
        })
      )

      participantService
        .updateParticipants(this.experiment.experimentId, requestBody)
        .then((response) => {
          if (response?.status === 200) {
            this.$router.push({
              name: path,
              params: { experiment: this.experiment.experimentId },
            });
          } else {
            this.$swal(response.error)
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
    saveExit() {
      this.submitDistribution(this.getSaveExitPage)
    }
  },
  async created() {
    await this.fetchExposures(this.experiment.experimentId);
    await this.fetchParticipants(this.experiment.experimentId);
  },
  beforeRouteUpdate(to, from, next) {
    //  load participant data before selection screen
    return (
      store
        .dispatch("participants/fetchParticipants", to.params.experimentId)
        .then(next, next)
    );
  },
};
</script>
