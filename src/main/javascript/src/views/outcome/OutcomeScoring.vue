<template>
  <div v-if="experiment && exposureId && outcome">
    <h1 class="mb-6">{{ exposure_title }}</h1>
    <form @submit.prevent="saveExit">
      <v-row>
        <v-col cols="12">
          <v-text-field
            v-model="outcome.title"
            name="outcomeTitle"
            class="pb-0 mb-0"
            :rules="rules"
            label="Outcome name"
            autofocus
            outlined
            required
          ></v-text-field>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="4">
          <v-text-field
            type="number"
            name="outcomeMaxPoints"
            v-model="outcome.maxPoints"
            label="Total Points"
            :rules="numberRule"
            outlined
            required
          ></v-text-field>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="12">
          <v-simple-table class="mb-9 v-data-table--light-header">
            <template v-slot:default>
              <thead>
              <tr>
                <th class="text-left">Student Name</th>
                <th class="text-left" width="250">Numeric Score</th>
              </tr>
              </thead>
              <tbody>
                <tr
                  v-for="participant in participantFilteredList"
                  :key="participant.participantId"
                >
                  <td>{{ participant.user.displayName }}</td>
                  <td v-if="participantScoreList.length">
                    <v-text-field
                      type="number"
                      :name="participant.participantId"
                      v-model="participantScoreList.filter(psl=>psl.participantId===participant.participantId && psl.experimentId===experimentId)[0].scoreNumeric"
                      placeholder="---"
                      style="max-width: 50px;"
                      required
                    ></v-text-field>
                  </td>
                </tr>
              </tbody>
            </template>
          </v-simple-table>
        </v-col>
      </v-row>
    </form>
  </div>
</template>

<script>
import {mapActions, mapGetters} from 'vuex'

  export default {
    name: 'OutcomeScoring',
    computed: {
      ...mapGetters({
        experiment: 'experiment/experiment',
        exposures: 'exposures/exposures',
        outcome: 'outcome/outcome',
        outcomeScores: 'outcome/outcomeScores',
        participants: 'participants/participants'
      }),
      exitDisabled() {
        return this.outcome.title.length<1 || this.outcome.title.length > 255 || this.outcome.maxPoints<0 || this.outcomeScores.filter((os) => os.outcomeId === this.outcomeId).some((score) => score.scoreNumeric > this.outcome.maxPoints)
      },
      exposureId() {
        return parseInt(this.$route.params.exposureId)
      },
      exposure_title() {
        return this.exposures.filter(o=>o.exposureId===this.exposureId)[0].title
      },
      experimentId() {
        return parseInt(this.$route.params.experimentId)
      },
      outcomeId() {
        return parseInt(this.$route.params.outcomeId)
      },
      participantScoreList() {
        let arr = []
        const scoresAssociatedwithOutcome = this.outcomeScores.filter((score) => score.outcomeId === this.outcomeId)
        this.participantFilteredList.map(p=>{
          const score = scoresAssociatedwithOutcome.filter(o=>o.participantId===p.participantId)[0]
          let item = {
            experimentId: this.experimentId,
            participantId: p.participantId,
            scoreNumeric: 0
          }
          if (typeof score !== "undefined") {
            item.outcomeScoreId = score?.outcomeScoreId
            item.outcomeId = this.outcomeId
            item.scoreNumeric = parseInt(score?.scoreNumeric)
          }
          arr.push(item);
        })
        return arr
      },
      participantFilteredList() {
        let sortedparticipantFilteredList = this.participants.filter(x => x.user.displayName !== null);
        let soratableNameAddedParticipants = []
        sortedparticipantFilteredList.map(x => {
          let dispName = x.user.displayName;
          let sortableName = dispName;
          if (dispName.includes(" ")) {
            let result = dispName.split(" ");
            if (result.length > 1) {
              sortableName = result[result.length - 1]+result[result.length - 2]; // Assume only two names
            }
          }
          x.user.sortableName = sortableName;
          soratableNameAddedParticipants.push(x);
        })
        return  soratableNameAddedParticipants.sort((a, b)=> (a.user.sortableName .toLowerCase()>
            b.user.sortableName.toLowerCase())?1:-1);
        }
    },
    data: () => ({
      rules: [
        v => v && !!v.trim() || 'required',
        v => (v || '').length <= 255 || 'A maximum of 255 characters is allowed'
      ],
      numberRule: [
        v => v && !isNaN(v) || 'required',
        v => (!isNaN(parseFloat(v))) && v >= 0 || 'The point value cannot be negative'
      ],
      titleProxy: ''
    }),
    methods: {
      ...mapActions({
        fetchParticipants: 'participants/fetchParticipants',
        fetchOutcomeById: 'outcome/fetchOutcomeById',
        fetchOutcomeScores: 'outcome/fetchOutcomeScores',
        updateOutcome: 'outcome/updateOutcome',
        updateOutcomeScores: 'outcome/updateOutcomeScores'
      }),
      async saveExit() {
        if(!this.exitDisabled){
            await this.updateOutcome([this.experimentId, this.exposureId, this.outcome])
            await this.updateOutcomeScores([this.experimentId, this.exposureId, this.outcomeId, this.participantScoreList])
            this.$router.push({ name: this.$router.currentRoute.meta.previousStep })
        } else {
          this.$swal({
            text: 'Could not update outcome due to entered data.',
            icon: 'error'
          })
        }
      }
    },
    async created() {
      await this.fetchOutcomeById([this.experimentId, this.exposureId, this.outcomeId])
      await this.fetchParticipants(this.experimentId)
      await this.fetchOutcomeScores([this.experimentId, this.exposureId, this.outcomeId])
    }
  }
</script>
