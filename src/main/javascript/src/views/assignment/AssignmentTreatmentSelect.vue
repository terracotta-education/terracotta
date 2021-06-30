<template>
  <div>
    <h1 class="pa-0 mb-7">Now, let’s upload your treatments for each condition for <strong>{{ assignment.title }}</strong></h1>

    <v-expansion-panels class="v-expansion-panels--outlined mb-7" flat>
      <v-expansion-panel class="py-3">
        <v-expansion-panel-header>{{ assignment.title }} (0/{{ conditions.length }})</v-expansion-panel-header>
        <v-expansion-panel-content>
          <v-list class="pa-0">

            <v-list-item class="justify-center px-0"
              v-for="condition in conditions"
              :key="condition.conditionId">
              <v-list-item-content>
                <p class="ma-0 pa-0">{{ condition.name }}</p>
              </v-list-item-content>

              <v-list-item-action>
                <v-btn
                  color="primary"
                  outlined
                  @click="goToBuilder(condition.conditionId)"
                >Select</v-btn>
              </v-list-item-action>
            </v-list-item>

          </v-list>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>

    <v-btn
      color="primary">
      Next
    </v-btn>
  </div>
</template>

<script>
import {mapGetters} from 'vuex';

export default {
  name: 'AssignmentTreatmentSelect',
  props: ['experiment'],
  computed: {
    ...mapGetters({
      assignments: 'assignment/assignments',
      conditions: 'experiment/conditions',
    }),
    assignment() {
      return this.assignments.filter(a => parseInt(a.assignmentId) === parseInt(this.$route.params.assignment_id))[0]
    }
  },
  methods: {
    goToBuilder(cid) {
      this.$router.push({
        name: 'TerracottaBuilder',
        params: {
          experiment_id: this.experiment.experimentId,
          condition_id: cid
        },
      });
    }
  },
};
</script>
