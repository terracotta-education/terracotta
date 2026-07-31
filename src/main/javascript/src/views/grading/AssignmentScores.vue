<template>
<div>
  <div
    v-if="isLoading"
    class="pa-4"
  >
    <v-progress-circular indeterminate color="primary" />
  </div>
  <div
    v-else-if="!experiment || !assignment?.assignmentId"
    class="pa-4"
  >
    <v-alert type="error" variant="outlined">
      Unable to load assignment data.
    </v-alert>
  </div>
  <div
    v-else-if="participantsLoadFailed"
    class="pa-4"
  >
    <v-alert type="error" variant="outlined">
      Unable to load participants.
    </v-alert>
  </div>
  <div v-else>
    <div
      v-if="showFileRequestAlert"
      class="pb-2"
    >
      <v-alert
        v-model="showFileRequestAlert"
        :type="fileRequestAlert.type"
        @click:close="handleFileRequestAlertDismiss"
        class="alert-file-request"
        elevation="0"
        closable
        variant="outlined"
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
            variant="outlined"
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
        {{ selectedTreatment.assessmentDto?.title }}
      </h3>
      <form
        @submit.prevent="saveExit"
      >
        <v-table
          class="mb-9 v-data-table--light-header"
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
                Score (out of {{ selectedTreatment.assessmentDto?.maxPoints }})
              </th>
            </tr>
          </thead>
          <tbody>
            <template
              v-for="(participant, pidx) in participantsWithSubmissionsByTreatmentId.get(selectedTreatment.treatmentId)"
              :key="pidx"
            >
              <tr v-if="participant.submission">
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
                    class="link-student-name"
                  >
                    {{ participant.user.displayName }}
                  </router-link>
                </td>
                <td>
                  <span>{{ participant.scoreToDisplay }}</span>
                </td>
              </tr>
            </template>
          </tbody>
        </v-table>
      </form>
      <hr v-if="index !== selectedAssignmentTreatments.length - 1" />
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from "vue";
import { useRoute, useRouter } from "vue-router";

import { experiment as experimentModule } from "@/store/experiment.module";
import { assignment as assignmentModule } from "@/store/assignment.module";
import { participants as participantsModule } from "@/store/participants.module";
import { assignmentFileArchive as assignmentFileArchiveModule } from "@/store/assignment-file-archive.module";

defineOptions({ name: "AssignmentScores" });

const experimentStore = experimentModule();
const assignmentStore = assignmentModule();
const participantsStore = participantsModule();
const assignmentFileArchiveStore = assignmentFileArchiveModule();

const route = useRoute();
const router = useRouter();

// ---------------- ROUTE PARAMS ----------------
const experimentId = computed(() => parseInt(route.params.experimentId));
const exposureId = computed(() => parseInt(route.params.exposureId));
const assignmentId = computed(() => parseInt(route.params.assignmentId));

// ---------------- STORE ----------------
const experiment = computed(() => experimentStore.experiment);
const assignment = computed(() => assignmentStore.assignment);
const participants = computed(() => participantsStore.participants);
const fileRequest = computed(() => assignmentFileArchiveStore.fileRequest);

// ---------------- SCORES ----------------
const round = n => (n % 1 ? n.toFixed(2) : n);

const calculateScore = (subs, scheme) => {
  const scoreList = [...subs]
    .sort((a, b) => a.dateSubmitted - b.dateSubmitted)
    .reverse()
    .map(s => s.gradeOverridden ? s.totalAlteredGrade : s.alteredCalculatedGrade);

  if (!scoreList.length) return "N/A";

  switch (scheme) {
    case "AVERAGE":
      return round(scoreList.reduce((a, b) => a + b, 0) / scoreList.length);
    case "HIGHEST":
      return Math.max(...scoreList);
    default:
      return scoreList[0];
  }
};

const getLatestSubmission = subs => {
  if (!subs.length) return null;
  return [...subs].sort((a, b) => a.dateSubmitted - b.dateSubmitted).reverse()[0];
};

const getParticipantWithSubmission = (participantList, treatment) => {
  const submissionsByParticipantId = new Map();

  (treatment.assessmentDto?.submissions || []).forEach(s => {
    const list = submissionsByParticipantId.get(s.participantId) || [];
    list.push(s);
    submissionsByParticipantId.set(s.participantId, list);
  });

  return participantList.map(p => {
    const subs = submissionsByParticipantId.get(p.participantId) || [];
    return {
      ...p,
      submission: getLatestSubmission(subs),
      scoreToDisplay: calculateScore(subs, treatment.assessmentDto.multipleSubmissionScoringScheme)
    };
  });
};

// memoized per treatment; only recomputes when participants or the assignment's treatments actually change,
// instead of on every re-render (e.g. the 5s file-request poll)
const participantsWithSubmissionsByTreatmentId = computed(() => {
  const map = new Map();

  selectedAssignmentTreatments.value.forEach(treatment => {
    map.set(
      treatment.treatmentId,
      getParticipantWithSubmission(participants.value, treatment)
    );
  });

  return map;
});

// ---------------- LOADING ----------------
const isLoading = ref(true);
const participantsLoadFailed = ref(false);

// ---------------- FILE REQUEST ----------------
const showFileRequestAlert = ref(false);
const fileRequestPolling = ref(false);
let intervalId = null;

const fileArchive = computed(() => {
  const fr = fileRequest.value;
  if (fr?.ready) return { showStatus: true, color: "success", icon: "mdi-check-circle", status: "File archive ready" };
  if (fr?.processing || fr?.reprocessing) return { showStatus: true, color: "info", icon: "mdi-loading mdi-spin", status: "Processing…" };
  if (fr?.error) return { showStatus: true, color: "error", icon: "mdi-alert-circle", status: "Error preparing archive" };
  return { showStatus: false };
});

const fileRequestAlert = computed(() => {
  const fr = fileRequest.value;
  if (fr?.ready) return { type: "success", text: "Your file archive is ready.", showDownloadLink: true };
  if (fr?.processing || fr?.reprocessing) return { type: "info", text: "Your file archive is being prepared. Please wait.", showDownloadLink: false };
  if (fr?.error) return { type: "error", text: "There was an error preparing your file archive.", showDownloadLink: false };
  return { type: "info", text: "", showDownloadLink: false };
});

const handleFileRequestAlertDismiss = () => {
  showFileRequestAlert.value = false;
};

const handleAlertFileRequest = async () => {
  if (fileRequest.value?.ready) {
    await assignmentFileArchiveStore.retrieve([
      experimentId.value,
      exposureId.value,
      assignmentId.value,
      fileRequest.value
    ]);
    showFileRequestAlert.value = false;
  }
};

const poll = async () => {
  await assignmentFileArchiveStore.poll([
    experimentId.value,
    exposureId.value,
    assignmentId.value,
    false
  ]);
};

const startPolling = fn => { intervalId = setInterval(fn, 5000); };
const stopPolling = () => { if (intervalId) clearInterval(intervalId); };

const handleFileRequestPolling = async () => {
  await poll();
  fileRequestPolling.value = fileRequest.value?.processing || fileRequest.value?.reprocessing;
  showFileRequestAlert.value = fileRequest.value?.ready || fileRequest.value?.error || fileRequestPolling.value;
};

const handleFileRequest = async () => {
  await poll();
  if (fileRequest.value?.ready) {
    await assignmentFileArchiveStore.retrieve([
      experimentId.value,
      exposureId.value,
      assignmentId.value,
      fileRequest.value
    ]);
    return;
  }
  await assignmentFileArchiveStore.prepare([
    experimentId.value,
    exposureId.value,
    assignmentId.value
  ]);
  fileRequestPolling.value = true;
  showFileRequestAlert.value = true;
};

// ---------------- DATA ----------------
const loadData = async () => {
  isLoading.value = true;
  participantsLoadFailed.value = false;
  assignmentFileArchiveStore.reset();

  const [, participantsResult] = await Promise.all([
    assignmentStore.fetchAssignment([
      experimentId.value,
      exposureId.value,
      assignmentId.value,
      true
    ]),
    participantsStore.fetchParticipants([experimentId.value])
  ]);

  participantsLoadFailed.value = participantsResult === null;

  if (hasFileSubmissionQuestions.value) {
    await assignmentFileArchiveStore.poll([
      experimentId.value,
      exposureId.value,
      assignmentId.value,
      false
    ]);

    showFileRequestAlert.value =
      fileRequest.value?.ready ||
      fileRequest.value?.processing ||
      fileRequest.value?.error;
  }

  isLoading.value = false;
};

// ---------------- COMPUTED ----------------
const selectedAssignmentTreatments = computed(() => {
  return assignment.value?.treatments || [];
});

const hasFileSubmissionQuestions = computed(() => {
  return assignment.value?.treatments?.some(t =>
    t.assessmentDto?.questions?.some(q => q.questionType === "FILE")
  );
});

const hasFileSubmissions = computed(() => {
  return assignment.value?.treatments
    ?.filter(t =>
      t.assessmentDto?.questions?.some(q => q.questionType === "FILE")
    )
    .some(t => (t.assessmentDto?.submissions?.length ?? 0) > 0);
});

// ---------------- NAV ----------------
const saveExit = () => {
  router.push({
    name: router.currentRoute.value.meta.previousStep
  });
};

// ---------------- WATCH ----------------
watch(fileRequestPolling, enabled => {
  if (enabled) {
    startPolling(handleFileRequestPolling);
  } else {
    stopPolling();
  }
});

// ---------------- LIFECYCLE ----------------
onMounted(loadData);

onBeforeUnmount(stopPolling);

defineExpose({ saveExit });
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
a.link-student-name {
  color: unset !important;
}
</style>
