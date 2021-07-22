<template>
  <div>
    <v-container v-if="experiment">
      <v-row class="my-1" justify="space-between">
        <v-col cols="8">
          <p class="header ma-0 pa-0">
            <v-img src="@/assets/terracotta_logo_mark.svg" class="mr-6" alt="Terracotta Logo" max-height="30" max-width="27"/>
            <span>{{ experiment.title }}</span>
          </p>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="12">
          <v-divider></v-divider>
          <v-tabs
            v-model="tab"
            elevation="0"
          >
            <v-tab
              v-for="item in items"
              :key="item"
            >
              {{ item }}
            </v-tab>
          </v-tabs>
          <v-divider class="mb-6"></v-divider>
          <v-tabs-items v-model="tab">
            <v-tab-item class="py-3"
              v-for="item in items"
              :key="item">
              <template v-if="item==='status'">
                <experiment-summary-status :experiment="experiment" />
              </template>
              <template v-if="item==='setup'">
                <v-card
                  class="pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
                  outlined
                >
                  <p class="pb-0"><strong>Note:</strong> You are currently collecting assignment submissions. Some setup functionality may be disabled to not disrupt the experiment.</p>
                </v-card>
              </template>
            </v-tab-item>
          </v-tabs-items>
        </v-col>
      </v-row>
    </v-container>
    <v-container v-else>
      no experiment
    </v-container>
  </div>
</template>

<script>
  import store from '@/store'
  import {mapGetters} from 'vuex'
  import ExperimentSummaryStatus from '@/views/ExperimentSummaryStatus'

  export default {
    name: 'ExperimentSummary',
    components: { ExperimentSummaryStatus },
    computed: {
      ...mapGetters({
        experiment: 'experiment/experiment'
      }),
    },
    data: () => ({
      tab: null,
      items: ['status','setup']
    }),
    methods: {

    },
    created() {
      this.tab = (this.$router.currentRoute.name==='ExperimentSummary')? 1 : 0
    },
    beforeRouteEnter (to, from, next) {
      return store.dispatch('experiment/fetchExperimentById', to.params.experiment_id).then(next, next)
    },
    beforeRouteUpdate (to, from, next) {
      return store.dispatch('experiment/fetchExperimentById', to.params.experiment_id).then(next, next)
    },
  }
</script>

<style lang="scss" scoped>
  .header {
    display: flex;
    flex-direction: row;
    align-items: center;
  }
</style>