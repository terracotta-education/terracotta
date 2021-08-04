<template>
  <div v-if="experiment && exposure_id && outcome">
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
                  v-for="participant in participants"
                  :key="participant.participantId"
                >
                  <td>{{ participant.user.displayName }}</td>
                  <td v-if="participantScoreList.length">
                    <v-text-field
                      type="number"
                      :name="participant.participantId"
                      v-model="participantScoreList.filter(psl=>psl.participantId===participant.participantId && psl.experimentId===experiment_id)[0].scoreNumeric"
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
        return this.outcome.title.length<1 || this.outcome.maxPoints<0
      },
      exposure_id() {
        console.log("exposure id: ", this.$route.params.exposure_id)
        return parseInt(this.$route.params.exposure_id)
      },
      exposure_title() {
        return this.exposures.filter(o=>o.exposureId===this.exposure_id)[0].title
      },
      experiment_id() {
        return parseInt(this.$route.params.experiment_id)
      },
      outcome_id() {
        console.log("outcome id:", this.$route.params.outcome_id)
        return parseInt(this.$route.params.outcome_id)
      },
      participantScoreList() {
        console.log("this.exposures: ", this.exposures, this.outcome_id)
        let arr = []
        this.participants.map(p=>{
          console.log("outcome scores: ", JSON.stringify(this.outcomeScores))
          const score = this.outcomeScores.filter(o=>o.participantId===p.participantId)[0]
          let item = {
            experimentId: this.experiment_id,
            participantId: p.participantId,
            scoreNumeric: 0
          }

          if (typeof score !== "undefined") {
            console.log("score:", JSON.stringify(score))
            item.outcomeScoreId = score?.outcomeScoreId
            item.outcomeId = score?.outcomeId
            item.scoreNumeric = parseInt(score?.scoreNumeric)
          }

          arr.push(item)
        })
        return arr
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
         console.log("outcomescorint.vue -> saveExit: ", JSON.stringify(this.outcome))
        if(!this.exitDisabled){
            await this.updateOutcome([this.experiment_id, this.exposure_id, this.outcome])
            await this.updateOutcomeScores([this.experiment_id, this.exposure_id, this.outcome_id, this.participantScoreList])
            this.$router.push({ name: this.$router.currentRoute.meta.previousStep })
        }
      }
    },
    async created() {
        console.log("created: ", this.experiment_id, this.exposure_id, this.outcome_id)
      await this.fetchOutcomeById([this.experiment_id, this.exposure_id, this.outcome_id])
      await this.fetchParticipants(this.experiment_id)
      console.log("outcome id:", this.outcome_id)
      await this.fetchOutcomeScores([this.experiment_id, this.exposure_id, this.outcome_id])
    }
  }
</script>