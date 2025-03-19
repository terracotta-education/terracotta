<template>
  <div
    v-if="experiment && assignment"
  >
    <div
      v-if="showFileRequestAlert"
      class="pb-2"
    >
      <v-alert
        v-model="showFileRequestAlert"
        @input="handleFileRequestAlertDismiss"
        :type="fileRequestAlert.type"
        class="alert-file-request"
        elevation="0"
        dismissible
      >
        {{ fileRequestAlert.text }}
        <a
          v-if="fileRequestAlert.showDownloadLink"
          @click="handleAlertFileRequest()"
        >
          <b><i>Click here to download</i></b>.
        </a>
      </v-alert>
    </div>
    <template>
      <div
        class="header-row w-100 mb-2"
      >
        <h1
          class="header pb-2"
        >
          {{ assignment.title }}
        </h1>
        <div
          class="btn-row"
        >
          <div
            class="download-files"
          >
            <v-btn
              v-if="hasFileSubmissionQuestions"
              :disabled="!hasFileSubmissions"
              @click="handleFileRequest()"
              color="primary"
              class="btn-download-file"
              outlined
            >
              Retrieve File Submissions
            </v-btn>
            <div
              v-if="fileArchive.showStatus"
              class="file-archive-status"
            >
              <v-icon
                :color="fileArchive.color"
              >
                {{ fileArchive.icon }}
              </v-icon>
              {{ fileArchive.status }}
            </div>
          </div>
        </div>
      </div>
      <div
        v-for="(selectedTreatment, index) in selectedAssignmentTreatments"
        :key="selectedTreatment.treatmentId"
        class="mt-6"
      >
        <h3>
          {{ selectedTreatment.assessmentDto.title }}
        </h3>
        <form
          @submit.prevent="saveExit"
        >
          <v-simple-table
            class="mb-9 v-data-table--light-header"
          >
            <template
              v-slot:default
            >
              <thead>
                <tr>
                  <th
                    class="text-left"
                  >
                    Student Name
                  </th>
                  <th
                    class="text-left"
                    style="width:250px;"
                  >
                    Score (out of {{ selectedTreatment.assessmentDto.maxPoints }})
                  </th>
                </tr>
              </thead>
              <tbody>
                <template
                  v-for="(participant, pidx) in getParticipantWithSubmission(participants, selectedTreatment)"
                >
                  <template
                    v-if="participant.submission"
                  >
                    <tr
                      :key="pidx"
                    >
                      <td>
                        <router-link
                          :to="{
                            name: 'StudentSubmissionGrading',
                            params: {
                              experimentId: experimentId,
                              exposureId: exposureId,
                              assignmentId: assignmentId,
                              assessmentId: participant.submission.assessmentId,
                              conditionId: participant.submission.conditionId,
                              treatmentId: participant.submission.treatmentId,
                              participantId: participant.participantId,
                            },
                          }"
                        >
                          {{ participant.user.displayName }}
                        </router-link>
                      </td>
                      <td>
                        <span>{{ participant.scoreToDisplay }}</span>
                      </td>
                    </tr>
                  </template>
                </template>
              </tbody>
            </template>
          </v-simple-table>
        </form>
        <template
          v-if="index !== selectedAssignmentTreatments.length - 1"
        >
          <hr />
        </template>
      </div>
    </template>
  </div>
</template>

<script>
import { mapActions, mapGetters } from "vuex";

export default {
  name: "AssignmentScores",
  data: () => ({
    fileRequestPollingId: null,
    fileRequestPolling: false,
    fileDownloadLinkClicked: false,
    showFileRequestAlert: false
  }),
  watch: {
    fileRequestPolling: {
      handler: function (enabled) {
        if (enabled) {
          // create file request polling scheduler
          this.fileRequestPollingId = window.setInterval(() => {
            this.handleFileRequestPolling()
          }, 5000);
        } else {
          // clear file request polling scheduler
          this.fileRequestPollingId = window.clearInterval(this.fileRequestPollingId);
        }
      },
      immediate: false
    },
  },
  computed: {
    ...mapGetters({
      experiment: "experiment/experiment",
      assignment: "assignment/assignment",
      participants: "participants/participants",
      editMode: "navigation/editMode",
      fileRequest: "assignmentfilearchive/fileRequest"
    }),
    assignmentId() {
      return this.assignment?.assignmentId || parseInt(this.$route.params.assignmentId);
    },
    exposureId() {
      return parseInt(this.$route.params.exposureId);
    },
    experimentId() {
      return this.experiment?.experimentId || parseInt(this.$route.params.experimentId);
    },
    selectedAssignmentTreatments() {
      return this.assignment.treatments;
    },
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || "ExperimentSummaryStatus";
    },
    hasFileSubmissionQuestions() {
      return this.assignment?.treatments?.some(
        treatment => {
          if (treatment.assessmentDto.questions.some(question => question.questionType === "FILE")) {
            return true;
          }
        }
      );
    },
    hasFileSubmissions() {
      return this.assignment?.treatments?.filter(
        treatment => {
          if (treatment.assessmentDto.questions.some(question => question.questionType === "FILE")) {
            return true;
          }
        }
      )
      .some(treatment => treatment.assessmentDto.submissions.length > 0);
    },
    fileArchiveAvailable() {
      return this.fileRequest?.ready;
    },
    fileArchive() {
      if (this.fileRequest?.ready || this.fileRequest?.downloaded) {
        return {
          status: "Files ready to download",
          color: "success",
          icon: "mdi-check",
          showStatus: this.showFileRequestStatus
        }
      }

      if (this.fileRequest?.processing || this.fileRequest?.reprocessing) {
        return {
          status: "Files are being processed",
          color: "info",
          icon: "mdi-clock",
          showStatus: this.showFileRequestStatus
        }
      }

      if (this.fileRequest?.error) {
        return {
          status: "File processing error",
          color: "error",
          icon: "mdi-exclamation",
          showStatus: this.showFileRequestStatus
        }
      }

      return {
        show: false
      }
    },
    showFileRequestStatus() {
      return !this.showFileRequestAlert &&
        [
          this.fileRequest?.processing,
          this.fileRequest?.reprocessing,
          this.fileRequest?.ready,
          this.fileRequest?.downloaded
        ].some(e => e === true);
    },
    fileRequestAlert() {
      if (this.fileRequest?.ready) {
        return {
          showDownloadLink: true,
          text: "Your files are ready.",
          type: "success"
        }
      }

      if (this.fileRequest?.processing || this.fileRequest?.reprocessing) {
        return {
          showDownloadLink: false,
          text: "Your files are being processed. Please do not navigate away from this page.",
          type: "info"
        }
      }

      if (this.fileRequest?.error) {
        return {
          showDownloadLink: false,
          text: "There was an error processing the requested assignment submission files. Please try again or contact support.",
          type: "error"
        }
      }

      return {};
    }
  },
  methods: {
    ...mapActions({
      fetchParticipants: "participants/fetchParticipants",
      fetchAssignment: "assignment/fetchAssignment",
      retrieveFileRequest: "assignmentfilearchive/retrieve",
      prepareFileRequest: "assignmentfilearchive/prepare",
      resetFileRequest: "assignmentfilearchive/reset",
      pollFileRequest: "assignmentfilearchive/poll",
      fileRequestErrorAcknowledge: "assignmentfilearchive/acknowledgeError"
    }),
    getParticipantWithSubmission(participants, treatment) {
      return participants.map(p => {
        const subs = treatment.assessmentDto.submissions;
        const psubs = subs.filter(s => s.participantId === p.participantId);
        const scoreToDisplay = this.calculateScore(psubs, treatment.assessmentDto.multipleSubmissionScoringScheme);
        const latest = this.getLatestSubmissionFromSet(psubs);
        return {
          ...p,
          submission: latest,
          scoreToDisplay: scoreToDisplay
        }
      });
    },
    getLatestSubmissionFromSet(subs) {
      if (!subs.length) {
        return null;
      }
      if (subs.length < 2) {
        return subs[0];
      }
      return [...subs].sort((a, b) => a.dateSubmitted - b.dateSubmitted).reverse()[0];
    },
    getParticipantName(participantId) {
      return this.participants?.filter((participant) => participant.participantId === participantId)[0]?.user.displayName;
    },
    async saveExit() {
      try {
        this.$router.push({
          name: this.$router.currentRoute.meta.previousStep,
        });
      } catch (error) {
        this.$swal("There was a problem saving assignment scores.");
      }
    },
    calculateScore(participantSubmissions, scheme) {
      // scores sorted by date descending
      const scores = participantSubmissions
        .sort((a, b) => a.dateSubmitted - b.dateSubmitted).reverse()
        .map((ps) => ps.gradeOverridden ? ps.totalAlteredGrade : ps.alteredCalculatedGrade);

      if (!scores.length) {
        return "N/A";
      }

      switch(scheme) {
        case "AVERAGE":
          return this.round((scores.reduce((a, b) => a + b, 0)) / scores.length);
        case "HIGHEST":
          return Math.max(...scores);
        case "MOST_RECENT":
        default:
          // latest score is first in array
          return scores[0];
      }
    },
    round(n) {
      return n % 1 ? n.toFixed(2) : n;
    },
    async loadData() {
      this.resetFileRequest();
      await this.fetchAssignment([
        this.experimentId,
        this.exposureId,
        this.assignmentId,
        true,
      ]);
      await this.fetchParticipants(this.experimentId);
      await this.pollFileRequest([
        this.experimentId,
        this.exposureId,
        this.assignmentId,
        false
      ]);
      this.showFileRequestAlert = this.fileRequest ? (this.fileRequest.ready || this.fileRequest.processing || this.fileRequest.reprocessing || this.fileRequest.error) : false;
    },
    async handleAlertFileRequest() {
      this.fileDownloadLinkClicked = true;
      await this.handleFileRequest();
    },
    async handleFileRequest() {
      await this.pollFileRequest([
        this.experimentId,
        this.exposureId,
        this.assignmentId,
        this.fileRequest ? (this.fileRequest.ready || this.fileRequest.downloaded) : false
      ]);

      if (this.fileRequest?.ready || this.fileRequest?.downloaded) {
        // retrieve file
        await this.retrieveFileRequest([
          this.experimentId,
          this.exposureId,
          this.assignmentId,
          this.fileRequest
        ]);

        if (this.fileRequest?.ready || this.fileRequest?.downloaded) {
          // file has been delivered
          return;
        }
      }

      if (this.fileRequest?.processing) {
        this.$swal({
          icon: "info",
          text: `Assignment files are still being processed. You will be notified when the files are ready for download.
            Please do not navigate away from this page.`,
          confirmButtonText: "OK"
        });
        return;
      }

      if (this.fileRequest?.reprocessing) {
        this.$swal({
          icon: "info",
          text: `New submissons have occurred since the requested set of assignment files were processed. A new set is being created.
            You will be notified when the files are ready for download. Please do not navigate away from this page.`,
          confirmButtonText: "OK"
        });
        return;
      }

      const fileRequestConfirm = await this.$swal({
        icon: "info",
        text: `Depending on the number of submissions, it could take several minutes to retrieve your files.
          You will see an alert when the files are ready to download. After you click “ok”, please stay on this page until your download is ready.`,
        showCancelButton: true,
        confirmButtonText: "OK"
      });

      if (fileRequestConfirm.isConfirmed) {
        await this.prepareFileRequest([
          this.experimentId,
          this.exposureId,
          this.assignmentId
        ]);
      }

      this.fileRequestPolling = this.fileRequest?.processing || this.fileRequest?.reprocessing;
      this.showFileRequestAlert = this.fileRequestPolling;
    },
    async handleFileRequestPolling() {
      await this.pollFileRequest([
        this.experimentId,
        this.exposureId,
        this.assignmentId,
        false
      ]);

      this.fileRequestPolling = this.fileRequest.processing || this.fileRequest.reprocessing;
      this.showFileRequestAlert = this.fileRequest.ready || this.fileRequest.error || this.fileRequestPolling;
    },
    async handleFileRequestAlertDismiss() {
      this.showFileRequestAlert = false;
      this.fileDownloadLinkClicked = false;
      this.fileRequestPolling = false;

      if (this.fileRequest?.error) {
        this.fileRequestErrorAcknowledge([
          this.experimentId,
          this.exposureId,
          this.assignmentId,
          this.fileRequest.id
        ]);
      }
    }
  },
  beforeRouteUpdate() {
    this.loadData();
  },
  async mounted() {
    this.loadData();
  },
  beforeDestroy() {
    // clear file request polling scheduler
    if (this.fileRequestPollingId !== null) {
      window.clearInterval(this.fileRequestPollingId);
    }
  }
};
</script>

<style lang="scss" scoped>
.header-row {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  > h1.header {
    max-width: fit-content;
    max-height: fit-content;
    line-height: 1.5;
  }
  & .btn-row {
  display: flex;
  flex-direction: row;
  justify-content:right;
  > .download-files {
    max-width: fit-content;
    display: flex;
    flex-direction: column;
    & .btn-download-file {
      max-width: fit-content;
    }
    & .file-archive-status {
      max-width: fit-content;
      margin: 0 auto;
    }
  }
}
}

.alert-file-request {
  margin: 0 auto;
  & a {
    color: white;
  }
}
</style>
