<template>
  <div v-if="experiment && exposure_id && outcome">
    <h1 class="mb-6">{{ 'Treatment Set 1 Outcome' }}</h1>
    <form @submit.prevent="handleOutcomeSubmission">
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
            :rules="rules"
            label="Total Points"
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
                  <td>
                    <v-text-field
                      type="number"
                      :name="participant.participantId"
                      v-model="participantScoreList.filter(psl=>psl.participantId===participant.participantId && psl.experimentId===experiment_id)[0].score"
                      placeholder="---"
                      style="max-width: 50px;"
                    ></v-text-field>
                  </td>
                </tr>
              </tbody>
            </template>
          </v-simple-table>
        </v-col>
      </v-row>

      <v-btn type="submit" elevation="0" color="primary">
        Finish
      </v-btn>
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
        outcome: 'outcome/outcome',
        participants: 'participants/participants'
      }),
      exposure_id() {
        return parseInt(this.$route.params.exposure_id)
      },
      experiment_id() {
        return parseInt(this.$route.params.experiment_id)
      },
      outcome_id() {
        return parseInt(this.$route.params.outcome_id)
      },
      participantScoreList() {
        let arr = []
        this.participants.map(p=>{
          p.score = 0
          arr.push(p)
        })
        return arr
      }
    },
    data: () => ({
      rules: [
        v => v && !!v.trim() || 'required',
        v => (v || '').length <= 255 || 'A maximum of 255 characters is allowed'
      ],
      titleProxy: ''
    }),
    methods: {
      ...mapActions({
        fetchParticipants: 'participants/fetchParticipants',
        fetchOutcomeById: 'outcome/fetchOutcomeById'
      }),
      handleOutcomeSubmission() {
        console.log(this.outcome)
        console.log(this.participantScoreList)
      }
    },
    created() {
      this.fetchOutcomeById([this.experiment_id, this.exposure_id, this.outcome_id])
      this.fetchParticipants(this.experiment_id)
    }
  }
</script>