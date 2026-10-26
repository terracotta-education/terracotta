<template>
  <div
    v-if="alerts.length"
    class="new-submissions-alerts"
  >
    <v-alert
      v-for="alert in alerts"
      :key="alert.archive.id"
      :type="alert.failed ? 'error' : alert.preparing ? 'info' : 'warning'"
      class="new-submissions-alert mb-3"
      elevation="0"
      variant="outlined"
      closable
      @click:close="dismiss(alert)"
    >
      <template v-if="alert.failed">
        There was an error preparing the new file archive for <b>{{ alert.archive.assignmentTitle }}</b>.
        <a
          href="#"
          @click.prevent="download(alert)"
        >
          <b><i>Click here to try again</i></b>.
        </a>
      </template>
      <template v-else-if="alert.preparing">
        Preparing a new file archive for <b>{{ alert.archive.assignmentTitle }}</b>. It downloads when it's ready.
      </template>
      <template v-else>
        There have been new submissions for <b>{{ alert.archive.assignmentTitle }}</b> since you last
        downloaded the file archive for this assignment.
        <a
          href="#"
          @click.prevent="download(alert)"
        >
          <b><i>Click here to download a new archive</i></b>.
        </a>
      </template>
    </v-alert>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from "vue";

import { assignmentFileArchiveService } from "@/services/assignment-file-archive.service";

defineOptions({
  name: "NewSubmissionsAlerts"
});

const props = defineProps({
  experimentId: {
    type: String,
    required: true
  }
});

const POLL_INTERVAL_MS = 5000;

// one per assignment whose latest file archive has new submissions since it was made
const alerts = ref([]);
const timers = new Set();

const path = archive => [props.experimentId, archive.exposureId, archive.assignmentId];

const remove = alert => {
  alerts.value = alerts.value.filter(other => other !== alert);
};

// keeps it dismissed until there are submissions newer than this (the same as on the
// assignment's submissions page)
const dismiss = alert => {
  remove(alert);
  assignmentFileArchiveService.acknowledgeOutdated(...path(alert.archive), alert.archive.id);
};

const waitUntilReady = alert => {
  const timer = setInterval(async () => {
    const archive = await assignmentFileArchiveService.poll(...path(alert.archive), false);

    if (archive?.status === "READY") {
      clearInterval(timer);
      timers.delete(timer);
      await assignmentFileArchiveService.retrieve(...path(alert.archive), archive);
      remove(alert);
    } else if (archive?.status === "ERROR" || archive?.error) {
      clearInterval(timer);
      timers.delete(timer);
      alert.preparing = false;
      alert.failed = true;
    }
  }, POLL_INTERVAL_MS);

  timers.add(timer);
};

const download = async alert => {
  alert.failed = false;
  alert.preparing = true;

  const archive = await assignmentFileArchiveService.prepare(...path(alert.archive));

  if (!archive || archive.error) {
    alert.preparing = false;
    alert.failed = true;
    return;
  }

  waitUntilReady(alert);
};

onMounted(async () => {
  const outdated = await assignmentFileArchiveService.getOutdated(props.experimentId);

  if (Array.isArray(outdated)) {
    alerts.value = outdated.map(archive => ({
      archive,
      preparing: false,
      failed: false
    }));
  }
});

onBeforeUnmount(() => {
  timers.forEach(clearInterval);
  timers.clear();
});
</script>

<style lang="scss" scoped>
.new-submissions-alert {
  & a {
    color: inherit !important;
    cursor: pointer;
  }
}
</style>
