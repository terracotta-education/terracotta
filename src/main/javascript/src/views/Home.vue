<template>
  <v-container v-if="!experiments || experiments.length<1">
    <div class="terracotta-appbg"/>
    <v-row justify="center" class="text-center">
      <v-col md="6" class="mt-15 ">
        <v-img src="@/assets/terracotta_logo.svg" alt="Terracotta Logo" class="mb-13 mx-auto" max-width="400"/>
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
            <button v-if="item"
                         class="v-data-table__link"
                         @click="handleNavigate(item.experimentId)">
              <template v-if="item.title">
                {{ item.title }}
              </template>
              <template v-else>
                <em>No Title</em>
              </template>
            </button>
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
                <v-list-item
                    @click="handleExport(item)"
                >
                <v-list-item-icon class="mr-3">
                <v-icon color="black">mdi-download</v-icon>
                </v-list-item-icon>
                <v-list-item-content>
                <v-list-item-title>Export</v-list-item-title>
                </v-list-item-content>
                </v-list-item>
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
import {saveAs} from 'file-saver'
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
      experiments: 'experiment/experiments',
      exportdata: 'exportdata/exportData'
    })
  },
  methods: {
    ...mapActions({
      fetchExperiments: 'experiment/fetchExperiments',
      createExperiment: 'experiment/createExperiment',
      deleteExperiment: 'experiment/deleteExperiment',
      resetConsent: 'consent/resetConsent',
      getZip: 'exportdata/fetchExportData'
    }),
    async handleExport(item) {
      await this.getZip(item.experimentId)
      saveAs(this.exportdata, `Terracotta Experiment ${item.title} Export.zip`);
    },
    async handleDelete(e) {
      if (e?.experimentId) {
        const reallyDelete = await this.$swal({
          icon: 'question',
          text: `Do you really want to delete "${e.title}"?`,
          showCancelButton: true,
          confirmButtonText: 'Yes, delete it',
          cancelButtonText: 'No, cancel',
        })
        // if confirmed, delete experiment
        if (reallyDelete.isConfirmed) {
          try {
            this.deleteExperiment(e.experimentId)
          } catch (error) {
            this.$swal({
              text: 'Could not delete experiment.',
              icon: 'error'
            })
          }
        }
      }
    },
    handleNavigate(experimentId) {
        const selectedExperiment =  this.experiments.filter((experiment) => experiment.experimentId === experimentId)
        const {exposureType, participationType, distributionType} = selectedExperiment[0]
        const isExperimentInComplete = [exposureType, participationType, distributionType].some((value) => value === 'NOSET')
        if(isExperimentInComplete) {
          this.$router.push({name: 'ExperimentDesignIntro', params: {experiment_id: experimentId}})
        } else {
          this.$router.push({name: 'ExperimentSummary', params: {experiment_id: experimentId}})
        }
    },
    startExperiment() {
      const _this = this
      this.createExperiment()
          .then(response => {
            if (response?.data?.experimentId) {
              _this.$router.push({name: 'ExperimentDesignIntro', params: {experiment_id: response.data.experimentId}})
            } else {
              this.$swal({
                text: `Error Status: ${response?.status} - There was an issue creating an experiment`,
                icon: 'error'
              })
            }
          }).catch(response => {
            console.log('startExperiment -> createExperiment | catch', {response})
          })
    }
  },
  async created() {
    // get experiments list
    await this.fetchExperiments()

    // reset consent data when loading the dashboard
    await this.resetConsent()
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

.terracotta-appbg {
	background: url('~@/assets/terracotta_appbg.jpg') no-repeat center center;
	background-size: cover;
	height: 100%;
	width: 100%;
	position: fixed;
	top: 0;
	left: 0;
	opacity: 0.5;
}
.terracotta-appbg + * {
	position: relative; /*place the content above the terracotta-appbg*/
}
</style>
