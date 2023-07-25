<template>
  <div>
    <PageLoading
      :display="!isLoaded"
      :message="'Loading experiments. Please wait.'"
    >
    </PageLoading>
    <v-container
      v-show="isLoaded && !hasExperiments"
    >
      <div class="terracotta-appbg"></div>
      <v-row
        justify="center"
        class="text-center"
      >
        <v-col
          md="6"
          class="mt-15"
        >
          <v-img
            src="@/assets/terracotta_logo.svg"
            alt="Terracotta Logo"
            class="mb-13 mx-auto"
            max-width="400"
          />
          <h1>Experimental research in the LMS</h1>
          <p class="mb-10">
            Welcome to Terracotta, the platform that supports teachers' and researchers' abilities to easily run experiments in live classes.<br>
            New to Terracotta?
            <a
              href="https://terracotta.education/terracotta-overview"
              target="_blank"
            >
              Read an overview of the tool
            </a>.
          </p>
          <p class="mb-0">Ready to get started?</p>
          <v-btn
            @click="startExperiment"
            color="primary"
            elevation="0"
          >
            Create your first experiment
          </v-btn>
        </v-col>
      </v-row>
    </v-container>
    <v-container
      v-show="isLoaded && hasExperiments"
    >
      <v-row
        class="mb-5"
        justify="space-between"
      >
        <v-col cols="6">
          <v-img
            src="@/assets/terracotta_logo.svg"
            alt="Terracotta Logo"
            max-width="138"
          />
        </v-col>
        <v-col
          cols="6"
          class="text-right"
        >
          <v-btn
            @click="startExperiment"
            color="primary"
            elevation="0"
          >
            New Experiment
          </v-btn>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="12">
          <h1 class="pl-4 mb-3">Experiments</h1>
          <v-data-table
            :headers="headers"
            :items="experiments || []"
            class="table-experiments"
          >
            <template v-slot:item.title="{ item }">
              <button
                v-if="item"
                class="v-data-table__link"
                @click="handleNavigate(item.experimentId)"
              >
                <template v-if="item.title">
                  {{ item.title }}
                </template>
                <template v-else>
                  <em>No Title</em>
                </template>
              </button>
            </template>
            <template v-slot:item.createdAt="{ item }">
              <span v-if="item.createdAt">
                {{ item.createdAt | formatDate }}
              </span>
            </template>
            <template v-slot:item.actions="{ item }">
              <v-menu offset-y>
                <template v-slot:activator="{ on, attrs }">
                  <v-icon
                    color="black"
                    v-bind="attrs"
                    v-on="on"
                    :aria-label="`actions for experiment ${item.title}`"
                  >
                    mdi-dots-horizontal
                  </v-icon>
                </template>
                <v-list dense>
                  <v-list-item
                      @click="handleExport(item)"
                      :aria-label="`export experiment ${item.title}`"
                  >
                    <v-list-item-icon class="mr-3">
                      <v-icon color="black">mdi-download</v-icon>
                    </v-list-item-icon>
                    <v-list-item-content>
                      <v-list-item-title>Export</v-list-item-title>
                    </v-list-item-content>
                  </v-list-item>
                    <v-tooltip
                      :disabled="!item.started"
                      top
                    >
                      <template
                        #activator="{ on }"
                      >
                        <span v-on="on">
                          <v-list-item
                            @click="handleDelete(item)"
                            :aria-label="`delete experiment ${item.title}`"
                            :disabled="item.started"
                          >
                            <v-list-item-icon class="mr-3">
                              <v-icon
                                :color="item.started ? 'grey' : 'black'"
                              >
                                mdi-delete
                              </v-icon>
                            </v-list-item-icon>
                            <v-list-item-content>
                              <v-list-item-title>Delete</v-list-item-title>
                            </v-list-item-content>
                          </v-list-item>
                        </span>
                      </template>
                      <span>You cannot delete this experiment because at least one student has completed an assignment.</span>
                    </v-tooltip>
                </v-list>
              </v-menu>
            </template>
          </v-data-table>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script>
import {mapActions, mapGetters} from 'vuex';
import {saveAs} from 'file-saver';
import moment from 'moment';
import PageLoading from "@/components/PageLoading";

export default {
  name: 'Home',
  components: {
    PageLoading
  },
  data() {
    return {
      headers: [
        {text: 'Experiment name', value: 'title'},
        {text: 'Created', value: 'createdAt'},
        {text: 'Actions', value: 'actions', sortable: false},
      ],
      isLoaded: false
    }
  },
  filters: {
    formatDate: function (date) {
      return moment(date).fromNow();
    }
  },
  computed: {
    ...mapGetters({
      experiments: 'experiment/experiments',
      exportdata: 'exportdata/exportData'
    }),
    hasExperiments() {
      return this.experiments && this.experiments.length > 0;
    }
  },
  watch: {
    // this is necessary, as vuejs doesn't allow tabbing + keyboard selection of column sorting
    hasExperiments: {
      handler() {
        if (!this.hasExperiments) {
          this.isLoaded = true;
          return;
        }

        const table = this.$el.querySelector(".table-experiments");

        if (!table) {
          this.isLoaded = true;
          return;
        }

        const sortableColumns = table.querySelectorAll('th.sortable > span:not(.v-icon)');

        sortableColumns.forEach(
          col => {
            col.setAttribute('tabindex', '0');
            col.addEventListener(
              'keyup',
              (event) => {
                if (event.key !== 'Enter') {
                  return;
                }

                event.target.click();
              }
            );
          }
        )
      }
    }
  },
  methods: {
    ...mapActions({
      fetchExperiments: 'experiment/fetchExperiments',
      createExperiment: 'experiment/createExperiment',
      deleteExperiment: 'experiment/deleteExperiment',
      resetConsent: 'consent/resetConsent',
      getZip: 'exportdata/fetchExportData',
      resetAssessments: 'assessments/resetAssessments',
      resetAssignment: 'assignments/resetAssignment',
      resetAssignments: 'assignments/resetAssignments',
      resetConditions: 'conditions/resetConditions',
      resetExportData: 'exportData/resetExportData',
      resetExposures: 'exposures/resetExposures',
      resetOutcome: 'outcomes/resetOutcome',
      resetOutcomePotentials: 'outcomes/resetOutcomePotentials',
      resetParticipants: 'participants/resetParticipants',
      resetSubmissions: 'submissions/resetSubmissions',
      resetTreatments: 'treatments/resetTreatments',
      deleteEditMode: 'navigation/deleteEditMode'
    }),
    async handleExport(item) {
      await this.getZip(item.experimentId);
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
        });
        // if confirmed, delete experiment
        if (reallyDelete.isConfirmed) {
          try {
            this.deleteExperiment(e.experimentId);
          } catch (error) {
            this.$swal({
              text: 'Could not delete experiment.',
              icon: 'error'
            });
          }
        }
      }
    },
    handleNavigate(experimentId) {
      const selectedExperiment =  this.experiments.filter((experiment) => experiment.experimentId === experimentId);
      const {exposureType, participationType, distributionType} = selectedExperiment[0];
      const isExperimentInComplete = [exposureType, participationType, distributionType].some((value) => value === 'NOSET');

      if(isExperimentInComplete) {
        this.$router.push({
          name: 'ExperimentDesignIntro',
          params: {
            experiment_id: experimentId
          }
        });
      } else {
        this.$router.push({
          name: 'ExperimentSummary',
          params: {
            experiment_id: experimentId
          }
        });
      }
    },
    startExperiment() {
      const _this = this;
      this.createExperiment()
        .then(response => {
          if (response?.data?.experimentId) {
            _this.$router.push({
              name: 'ExperimentDesignIntro',
              params: {
                experiment_id: response.data.experimentId
              }
            });
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
    // reset consent data when loading the dashboard
    await this.resetConsent();

    // reset data in state
    this.resetAssessments();
    this.resetAssignments();
    this.resetAssignment();
    this.resetConditions();
    this.resetExportData();
    this.resetExposures();
    this.resetOutcome();
    this.resetOutcomePotentials();
    this.resetParticipants();
    this.resetSubmissions();
    this.resetTreatments();
    this.deleteEditMode();

    // get experiments list
    await this.fetchExperiments();

    this.isLoaded = true;
  }
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
div.v-tooltip__content {
  max-width: 400px;
  opacity: 1.0 !important;
  background-color: rgba(55,61,63, 1.0) !important;
  a {
    color: #afdcff;
  }
}
</style>
