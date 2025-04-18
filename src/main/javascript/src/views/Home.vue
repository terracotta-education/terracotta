<template>
  <div>
    <PageLoading
      :display="!isLoaded"
      :message="`Loading experiments. Please wait.`"
    />
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
      <v-row
        v-if="dataExportRequestAlerts.length > 0"
      >
        <div
          v-for="dataExportRequestAlert in dataExportRequestAlerts"
          :key="dataExportRequestAlert.experimentId"
          class="alert-data-export-request pb-2 px-3"
        >
          <v-alert
            v-model="experimentDataExportRequests[dataExportRequestAlert.experimentId].showAlert"
            @input="handleDataExportRequestAlertDismiss(dataExportRequestAlert.experimentId)"
            :aria-label="`data export request alert for experiment ${dataExportRequestAlert.experimentId}`"
            :type="dataExportRequestAlert.type"
            elevation="0"
            dismissible
          >
            {{ dataExportRequestAlert.text }}
            <a
              v-if="dataExportRequestAlert.showDownloadLink"
              @click="handleAlertDataExportDownloadRequest(dataExportRequestAlert.experimentId)"
            >
              <b><i>Click here to download</i></b>.
            </a>
            <a
              v-if="dataExportRequestAlert.showRecreateLink"
              @click="handleDataExportRequest(dataExportRequestAlert.experimentId)"
            >
              <b><i>Click here to request a new data export</i></b>.
            </a>
          </v-alert>
        </div>
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
                      @click="handleDataExportRequest(item.experimentId)"
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
import {mapActions, mapGetters} from "vuex";
import moment from "moment";
import PageLoading from "@/components/PageLoading";

export default {
  name: "Home",
  components: {
    PageLoading
  },
  data: () => ({
    headers: [
      {text: "Experiment name", value: "title"},
      {text: "Created", value: "createdAt"},
      {text: "Actions", value: "actions", sortable: false},
    ],
    isLoaded: false,
    experimentDataExportRequests: {
      downloadLinkClicked: false
      // experimentId: {showAlert, polling: {active, id}}
    }
  }),
  filters: {
    formatDate: function (date) {
      return moment(date).fromNow();
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

        const sortableColumns = table.querySelectorAll("th.sortable > span:not(.v-icon)");

        sortableColumns.forEach(
          col => {
            col.setAttribute("tabindex", "0");
            col.addEventListener(
              "keyup",
              (event) => {
                if (event.key !== "Enter") {
                  return;
                }

                event.target.click();
              }
            );
          }
        )
      }
    },
    experimentDataExportRequests: {
      handler: function (newExperimentDataExportRequests) {
        for (const experimentId in newExperimentDataExportRequests) {
          if (!newExperimentDataExportRequests[experimentId].polling) {
            continue;
          }
          if (newExperimentDataExportRequests[experimentId].polling.active && !newExperimentDataExportRequests[experimentId].polling.id) {
            // create export data request polling scheduler
            this.experimentDataExportRequests[experimentId].polling.id = window.setInterval(() => {
                this.handleDataExportRequestPolling(experimentId)
              }, 5000);
          } else if (!newExperimentDataExportRequests[experimentId].polling.active && newExperimentDataExportRequests[experimentId].polling.id) {
            // clear export data request polling scheduler
            this.experimentDataExportRequests[experimentId].polling.id = window.clearInterval(newExperimentDataExportRequests[experimentId].polling.id);
          }
        }
      },
      immediate: false
    }
  },
  computed: {
    ...mapGetters({
      experiments: "experiment/experiments",
      dataExportRequests: "dataexportrequest/dataExportRequests"
    }),
    hasExperiments() {
      return this.experiments && this.experiments.length > 0;
    },
    showDataExportRequestStatus() {
      let experimentsToShow = [];
      for (const experimentId in this.experimentDataExportRequests) {
        if (this.experimentDataExportRequests[experimentId].showAlert) {
          const dataExportRequest = this.dataExportRequest(experimentId);
          if ([
              dataExportRequest?.processing,
              dataExportRequest?.reprocessing,
              dataExportRequest?.ready,
              dataExportRequest?.downloaded,
              dataExportRequest?.outdated
            ].some(e => e === true)
          ) {
            experimentsToShow.push(experimentId);
          }
        }
      }
      return experimentsToShow;
    },
    dataExportRequestAlerts() {
      let experimentsToShow = [];

      for (const experimentId in this.experimentDataExportRequests) {
        if (this.experimentDataExportRequests[experimentId].showAlert) {
          const dataExportRequest = this.dataExportRequest(experimentId);

          if (dataExportRequest?.ready) {
            experimentsToShow.push(
              {
                experimentId: dataExportRequest.experimentId,
                showDownloadLink: true,
                showRecreateLink: false,
                text: `Your data export for experiment "${dataExportRequest.experimentTitle}" is ready.`,
                type: "success"
              }
            );
            continue;
          }

          if (dataExportRequest?.processing || dataExportRequest?.reprocessing) {
            experimentsToShow.push(
              {
                experimentId: dataExportRequest.experimentId,
                showDownloadLink: false,
                showRecreateLink: false,
                text: `The data export for experiment "${dataExportRequest.experimentTitle}" is being processed. Please do not navigate away from this page.`,
                type: "info"
              }
            );
            continue;
          }

          if (dataExportRequest?.outdated) {
            experimentsToShow.push(
              {
                experimentId: dataExportRequest.experimentId,
                showDownloadLink: false,
                showRecreateLink: true,
                text: `There have been new submissions since the last requested data export for experiment "${dataExportRequest.experimentTitle}".`,
                type: "warning"
              }
            );
            continue;
          }

          if (dataExportRequest?.error) {
            experimentsToShow.push(
              {
                experimentId: dataExportRequest.experimentId,
                showDownloadLink: false,
                showRecreateLink: false,
                text: `There was an error processing the requested data export for experiment "${dataExportRequest.experimentTitle}". Please try again or contact support.`,
                type: "error"
              }
            );
            continue;
          }
        }
      }

      return experimentsToShow;
    }
  },

  methods: {
    ...mapActions({
      fetchExperiments: "experiment/fetchExperiments",
      createExperiment: "experiment/createExperiment",
      deleteExperiment: "experiment/deleteExperiment",
      resetConsent: "consent/resetConsent",
      resetAssessments: "assessments/resetAssessments",
      resetAssignment: "assignments/resetAssignment",
      resetAssignments: "assignments/resetAssignments",
      resetConditions: "conditions/resetConditions",
      resetExportData: "exportData/resetExportData",
      resetExposures: "exposures/resetExposures",
      resetOutcome: "outcomes/resetOutcome",
      resetOutcomePotentials: "outcomes/resetOutcomePotentials",
      resetParticipants: "participants/resetParticipants",
      resetResultsDashboard: "resultsDashboard/resetResultsDashboard",
      resetSubmissions: "submissions/resetSubmissions",
      resetTreatments: "treatments/resetTreatments",
      deleteEditMode: "navigation/deleteEditMode",
      retrieveDataExportRequest: "dataexportrequest/retrieve",
      prepareDataExportRequest: "dataexportrequest/prepare",
      resetDataExportRequest: "dataexportrequest/reset",
      pollDataExportRequest: "dataexportrequest/poll",
      pollDataExportRequests: "dataexportrequest/pollList",
      dataExportRequestAcknowledge: "dataexportrequest/acknowledge"
    }),
    async handleDelete(e) {
      if (e?.experimentId) {
        const reallyDelete = await this.$swal({
          icon: "question",
          text: `Do you really want to delete "${e.title}"?`,
          showCancelButton: true,
          confirmButtonText: "Yes, delete it",
          cancelButtonText: "No, cancel",
        });
        // if confirmed, delete experiment
        if (reallyDelete.isConfirmed) {
          try {
            this.deleteExperiment(e.experimentId);
          } catch (error) {
            this.$swal({
              text: "Could not delete experiment.",
              icon: "error"
            });
          }
        }
      }
    },
    handleNavigate(experimentId) {
      const selectedExperiment =  this.experiments.filter((experiment) => experiment.experimentId === experimentId);
      const {exposureType, participationType, distributionType} = selectedExperiment[0];
      const isExperimentIncomplete = [exposureType, participationType, distributionType].some((value) => value === "NOSET");

      if (isExperimentIncomplete) {
        this.$router.push({
          name: "ExperimentDesignIntro",
          params: {
            experimentId: experimentId
          }
        });
      } else {
        this.$router.push({
          name: "ExperimentSummary",
          params: {
            experimentId: experimentId
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
              name: "ExperimentDesignIntro",
              params: {
                experimentId: response.data.experimentId
              }
            });
          } else {
            this.$swal({
              text: `Error Status: ${response?.status} - There was an issue creating an experiment`,
              icon: "error"
            })
          }
        }).catch(response => {
          console.log("startExperiment -> createExperiment | catch", {response})
        })
    },
    async handleAlertDataExportDownloadRequest(experimentId) {
      this.experimentDataExportRequests.downloadLinkClicked = true;
      await this.handleDataExportRequest(experimentId);
    },
    async handleDataExportRequest(experimentId) {
      let dataExportRequest = this.dataExportRequest(experimentId);
      await this.pollDataExportRequest([
        experimentId,
        dataExportRequest ? (dataExportRequest.ready || dataExportRequest.downloaded) : false
      ]);

      if (dataExportRequest?.ready || dataExportRequest?.downloaded) {
        // retrieve file
        await this.retrieveDataExportRequest([
          experimentId,
          dataExportRequest
        ]);

        if (dataExportRequest?.ready || dataExportRequest?.downloaded) {
          // file has been delivered
          return;
        }
      }

      if (dataExportRequest?.processing) {
        this.$swal({
          icon: "info",
          text: `The data export for experiment "${dataExportRequest.experimentTitle}" is still being processed. You will be notified when the export is ready for download.
            Please do not navigate away from this page.`,
          confirmButtonText: "OK"
        });
        return;
      }

      if (dataExportRequest?.reprocessing) {
        this.$swal({
          icon: "info",
          text: `New submissons have occurred since the requested set of exported data for experiment "${dataExportRequest.experimentTitle}" was processed. A new export is being created.
            You will be notified when the export is ready for download. Please do not navigate away from this page.`,
          confirmButtonText: "OK"
        });
        return;
      }

      const dataExportRequestConfirm = await this.$swal({
        icon: "info",
        text: `Depending on its size, it could take several minutes to retrieve your data export.
          You will see an alert when the export is ready to download. After you click "ok," please stay on this page until your download is ready.`,
        showCancelButton: true,
        confirmButtonText: "OK"
      });

      if (dataExportRequestConfirm.isConfirmed) {
        await this.prepareDataExportRequest([
          experimentId
        ]);

        dataExportRequest = this.dataExportRequest(experimentId);
        this.experimentDataExportRequests = {
          ...this.experimentDataExportRequests,
          [experimentId]: {
            showAlert: dataExportRequest?.processing || dataExportRequest?.reprocessing,
            polling: {
              active: true,
              id: null
            }
          }
        };
      }
    },
    async handleDataExportRequestPolling(experimentId) {
      await this.pollDataExportRequest([
        experimentId,
        false
      ]);
      const dataExportRequest = this.dataExportRequest(experimentId);
      this.experimentDataExportRequests = {
        ...this.experimentDataExportRequests,
        [experimentId]: {
          showAlert: dataExportRequest.ready || dataExportRequest.error || dataExportRequest.processing || dataExportRequest.reprocessing || dataExportRequest.outdated,
          polling: {
            ...this.experimentDataExportRequests[experimentId].polling,
            active: dataExportRequest.processing || dataExportRequest.reprocessing
          }
        }
      };
    },
    async handleDataExportRequestAlertDismiss(experimentId) {
      this.experimentDataExportRequests = {
        ...this.experimentDataExportRequests,
        [experimentId]: {
          showAlert: false,
          polling: {
            active: false,
            id: this.experimentDataExportRequests[experimentId].polling.id
          }
        }
      };
      this.experimentDataExportRequests.downloadLinkClicked = false;
      const dataExportRequest = this.dataExportRequest(experimentId);

      if (dataExportRequest?.error) {
        this.dataExportRequestAcknowledge([
          experimentId,
          dataExportRequest.id,
          "ERROR_ACKNOWLEDGED"
        ]);
      }

      if (dataExportRequest?.ready) {
        this.dataExportRequestAcknowledge([
          experimentId,
          dataExportRequest.id,
          "READY_ACKNOWLEDGED"
        ]);
      }

      if (dataExportRequest?.outdated) {
        this.dataExportRequestAcknowledge([
          experimentId,
          dataExportRequest.id,
          "OUTDATED_ACKNOWLEDGED"
        ]);
      }
    },
    dataExportRequest(experimentId) {
      return this.dataExportRequests?.find(dataExportRequest => dataExportRequest.experimentId === parseInt(experimentId));
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
    this.resetResultsDashboard();
    this.resetSubmissions();
    this.resetTreatments();
    this.deleteEditMode();
    this.resetDataExportRequest();

    // get experiments list
    await this.fetchExperiments();

    // get existing data export requests
    await this.pollDataExportRequests([
      this.experiments.map(experiment => experiment.experimentId),
      false
    ]);

    // set up data export request alerts
    this.experiments.forEach(
      experiment => {
        const experimentId = experiment.experimentId;
        const dataExportRequest = this.dataExportRequest(experimentId);
        this.experimentDataExportRequests = {
          ...this.experimentDataExportRequests,
          [experimentId]: {
            showAlert: dataExportRequest ? (
              dataExportRequest.ready ||
              dataExportRequest.processing ||
              dataExportRequest.reprocessing ||
              dataExportRequest.outdated ||
              dataExportRequest.error
            ) : false,
            polling: {
              active: false,
              id: null
            }
          }
        }
      }
    );

    this.isLoaded = true;
  },
  beforeDestroy() {
    // clear data export request polling schedulers
    for (const experimentId in this.experimentDataExportRequests) {
      if (!this.experimentDataExportRequests[experimentId].polling) {
        continue;
      }
      if (this.experimentDataExportRequests[experimentId].polling.id) {
        window.clearInterval(this.experimentDataExportRequests[experimentId].polling.id);
      }
    }
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
	background: url("~@/assets/terracotta_appbg.jpg") no-repeat center center;
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
.alert-data-export-request {
  min-width: 100%;
  > .v-alert {
    margin: 0 auto;
    & a {
      color: white;
    }
  }
}
</style>
