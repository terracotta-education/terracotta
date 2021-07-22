<template>
  <v-container v-if="!experiments || experiments.length<1">
    <v-row justify="center" class="text-center">
      <v-col md="6" class="mt-15 ">
        <v-img src="@/assets/terracotta_logo.svg" alt="Terracotta Logo" class="mb-13 mx-auto" max-width="173"/>
        <h1>Experimental research in the LMS</h1>

        <p class="mb-10">
          Welcome to Terracotta, the platform that supports teachers' and researchers' abilities to easily
          run experiments in live classes. <br>
          New to Terracotta?
          <a href="https://terracotta.education/terracotta-overview" target="_blank">Read an overview of the tool</a>.
        </p>

        <p class="mb-0">Ready to get started?</p>
        <v-btn @click="startExperiment" color="primary" elevation="0">Create your first experiment</v-btn>
      </v-col>
    </v-row>
  </v-container>
  <v-container v-else>
    <v-row class="mb-5" justify="space-between">
      <v-col cols="6">
        <v-img src="@/assets/terracotta_logo.svg" alt="Terracotta Logo" max-width="138"/>
      </v-col>
      <v-col cols="6" class="text-right">
        <v-btn @click="startExperiment" color="primary" elevation="0">New Experiment</v-btn>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="12">
        <h1 class="pl-4 mb-3">Experiments</h1>
        <v-data-table
          :headers="headers"
          :items="experiments"
        >
          <template v-slot:item.title="{ item }">
            <router-link v-if="item"
                         class="v-data-table__link"
                         :to="{name: 'ExperimentDesignIntro', params: {experiment_id: item.experimentId}}">
              <template v-if="item.title">
                {{ item.title }}
              </template>
              <template v-else>
                <em>No Title</em>
              </template>
            </router-link>
          </template>
          <template v-slot:item.createdAt="{ item }">
            <span v-if="item.createdAt">{{ item.createdAt | formatDate }}</span>
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
                <!--                UNCOMMENT WHEN API EXPORT FUNCTIONALITY IS READY -->
                <!--                <v-list-item-->
                <!--                  @click="handleExport(item)"-->
                <!--                >-->
                <!--                  <v-list-item-icon class="mr-3">-->
                <!--                    <v-icon color="black">mdi-download</v-icon>-->
                <!--                  </v-list-item-icon>-->
                <!--                  <v-list-item-content>-->
                <!--                    <v-list-item-title>Export</v-list-item-title>-->
                <!--                  </v-list-item-content>-->
                <!--                </v-list-item>-->
                <v-list-item
                  @click="handleDelete(item)"
                >
                  <v-list-item-icon class="mr-3">
                    <v-icon color="black">mdi-delete</v-icon>
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
import {mapActions, mapGetters} from 'vuex'
import moment from 'moment'

export default {
  name: 'Home',
  data() {
    return {
      headers: [
        {text: 'Experiment name', value: 'title'},
        {text: 'Created', value: 'createdAt'},
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
      fetchExperiments: 'experiment/fetchExperiments',
      createExperiment: 'experiment/createExperiment',
      deleteExperiment: 'experiment/deleteExperiment',
      resetConsent: 'consent/resetConsent'
    }),
    handleExport() {
      // TODO - add API export functionality when it's ready
    },
    handleDelete(e) {
      if (e?.experimentId && confirm(`Do you really want to delete "${e.title}"?`)) {
        this.deleteExperiment(e.experimentId)
            .then(response => {
              if (response?.status !== 200) {
                alert('Could not delete experiment.')
              }
            })
      }
    },
    startExperiment() {
      const _this = this
      this.createExperiment()
          .then(response => {
            if (response?.data?.experimentId) {
              _this.$router.push({name: 'ExperimentDesignIntro', params: {experiment_id: response.data.experimentId}})
            } else {
              alert(`Error Status: ${response?.status} - There was an issue creating an experiment`)
            }
          }).catch(response => {
            console.log('startExperiment -> createExperiment | catch', {response})
          })
    }
  },
  created() {
    // get experiments list
    this.fetchExperiments()

    // reset consent data when loading the dashboard
    this.resetConsent()
  },
}
</script>

<style lang="scss">
.v-data-table {

  * {
    color: black !important;
  }

  *:not(.v-icon) {
    font-size: 16px !important;
  }

  &__wrapper {
    border: 1px solid #E0E0E0;
    border-radius: 10px;
  }

  &__link {
    text-decoration: none;

    &:focus,
    &:hover {
      text-decoration: underline;
    }
  }

  .v-data-footer {
    border-top: none !important;
  }
}
</style>