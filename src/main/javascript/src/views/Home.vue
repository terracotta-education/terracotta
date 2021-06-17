<template>
  <v-container v-if="!experiments || experiments.length<1">
    <v-row justify="center" class="text-center">
      <v-col md="6" class="mt-15 ">
        <v-img src="@/assets/terracotta_logo.svg" alt="Terracotta Logo" class="mb-13 mx-auto" max-width="173"/>
        <h1>Experimental research in the LMS</h1>

        <p class="mb-10">Welcome to Terracotta, the platform that supports teachers' and researchers' abilities to
          easily run experiments in live classes.
          <br>New to Terracotta? <a href="https://terracotta.education/terracotta-overview" target="_blank">Read an overview of the tool</a>.</p>

        <p class="mb-0">Ready to get started?</p>
        <v-btn @click="startExperiment" color="primary" elevation="0">Create your first experiment</v-btn>
      </v-col>
    </v-row>
  </v-container>
  <v-container v-else>
    <v-row class="mb-5" justify="space-between">
      <v-col cols="2">
        <v-img src="@/assets/terracotta_logo.svg" alt="Terracotta Logo" max-width="138"/>
      </v-col>
      <v-col cols="2">
        <v-btn @click="startExperiment" color="primary" elevation="0">New Experiment</v-btn>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="12">
        <h1 class="pl-3 mb-3">Experiments</h1>
        <v-data-table
          :headers="headers"
          :items="experiments"
        >
          <template v-slot:item.title="{ item }">
            <router-link v-if="item.title"
                         :to="{name: 'ExperimentDesignIntro', params: {experiment_id: item.experimentId}}">
              {{ item.title }}
            </router-link>
          </template>
          <template v-slot:item.updatedAt="{ item }">
            <span v-if="item.updatedAt">{{ item.updatedAt | formatDate }}</span>
          </template>
          <template v-slot:item.actions="{ item }">
            <v-menu offset-y>
              <template v-slot:activator="{ on, attrs }">
                <v-icon
                  color="black"
                  v-bind="attrs"
                  v-on="on"
                >
                  mdi-dots-horizontal
                </v-icon>
              </template>
              <v-list dense>
                <v-list-item
                  @click="exportExperiment(item)"
                >
                  <v-list-item-icon class="mr-3">
                    <v-icon>mdi-download</v-icon>
                  </v-list-item-icon>
                  <v-list-item-content>
                    <v-list-item-title>Export</v-list-item-title>
                  </v-list-item-content>
                </v-list-item>
                <v-list-item
                  @click="deleteExperiment(item)"
                >
                  <v-list-item-icon class="mr-3">
                    <v-icon>mdi-delete</v-icon>
                  </v-list-item-icon>
                  <v-list-item-content>
                    <v-list-item-title>Delete</v-list-item-title>
                  </v-list-item-content>
                </v-list-item>
              </v-list>
            </v-menu>
          </template>
        </v-data-table>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import {mapActions, mapGetters} from "vuex"
import moment from "moment"

export default {
  name: 'Home',
  data() {
    return {
      headers: [
        {text: 'Experiment name', value: 'title'},
        {text: 'Last modified', value: 'updatedAt'},
        {text: 'Actions', value: 'actions', sortable: false},
      ]
    }
  },
  filters: {
    formatDate: function (date) {
      return moment(date).fromNow()
    }
  },
  computed: {
    ...mapGetters({
      experiments: 'experiment/experiments'
    })
  },
  methods: {
    ...mapActions({
      createExperiment: 'experiment/createExperiment',
      fetchExperiments: 'experiment/fetchExperiments'
    }),
    exportExperiment(e) {
      // TODO - add export functionality
      console.log("export", {e})
    },
    deleteExperiment(e) {
      // TODO - add delete functionality
      console.log("delete", {e})
    },
    startExperiment() {
      const _this = this
      this.createExperiment()
      .then(response => {
        _this.$router.push({name: 'ExperimentDesignIntro', params: {experiment_id: response.experimentId}})
      }).catch(response => {
        // TODO - Error, couldn't create experiment
        console.log("startExperiment -> createExperiment | catch", {response})
      })
    }
  },
  created() {
    this.fetchExperiments()
  }
}
</script>

<style lang="scss">
.v-data-table {

  *:not(.v-icon) {
    color: black;
    font-size: 16px !important;
  }

  &__wrapper {
    border: 1px solid #E0E0E0;
    border-radius: 10px;
  }

  .v-data-footer {
    border-top: none !important;
  }
}
</style>