<template>
<v-container
  class="zero-state"
>
  <v-row
    v-if="importRequestAlerts.length > 0"
    class="py-2"
  >
    <div
      v-for="importRequestAlert in importRequestAlerts"
      :key="importRequestAlert.id"
      class="alert-request pb-2 px-3"
    >
      <v-alert
        v-if="experimentImportRequests[importRequestAlert.id]"
        v-model="experimentImportRequests[importRequestAlert.id].showAlert"
        @input="handleImportRequestAlertDismiss(importRequestAlert.id)"
        :type="importRequestAlert.type"
        elevation="0"
        dismissible
      >
        {{ importRequestAlert.text }}
        <ul
          v-if="importRequestAlert.showErrors"
        >
          <li
            v-for="(error, i) in importRequestAlert.errors"
            :key="i"
          >
            {{ error }}
          </li>
        </ul>
      </v-alert>
    </div>
  </v-row>
  <div
    class="terracotta-appbg"
  ></div>
  <v-row
    justify="center"
    class="text-center"
  >
    <v-col
      col="8"
      class="mt-15"
    >
      <v-img
        src="@/assets/terracotta_logo_vertical.svg"
        alt="Terracotta Logo"
        class="terrcotta-logo mb-13 mx-auto"
        max-height="127"
        max-width="176"
      />
      <h1
        class="experimental-header mb-5"
      >
        Experimental research in the LMS
      </h1>
      <p
        class="mb-10"
      >
        Welcome to Terracotta, the platform that supports teachers' and researchers' abilities to easily run experiments in live classes.
      </p>
      <v-row>
        <v-btn
          @click="startExperiment"
          class="experiment-btn"
          color="primary"
          elevation="0"
        >
          CREATE AN EXPERIMENT
        </v-btn>
      </v-row>
      <v-row>
        <v-btn
          v-if="experimentExportEnabled"
          @click="handleImportExperiment"
          class="experiment-btn"
          color="primary"
          text
        >
          OR IMPORT AN EXPERIMENT
        </v-btn>
      </v-row>
      <p
        class="mt-10"
      >
        New to Terracotta? You can check out our
        <a
          href="https://www.terracotta.education/help-center/quick-start-guide"
          target="_blank"
          rel="noopener"
          class="user-help-link"
        >Quick Start Guide</a>
        and
        <a
          href="https://terracotta-education.atlassian.net/wiki/spaces/TC/overview"
          target="_blank"
          rel="noopener"
          class="user-help-link"
        >Knowledge Base</a>.
      </p>
    </v-col>
  </v-row>
</v-container>
</template>

<script>
export default {
  props: {
    experimentExportEnabled: {
      type: Boolean,
      required: true
    },
    experimentImportRequests: {
      type: Object,
      required: true
    },
    importRequestAlerts: {
      type: Array,
      required: true
    }
  },
  methods: {
    handleImportRequestAlertDismiss(id) {
      this.$emit("handleImportRequestAlertDismiss", id);
    },
    startExperiment() {
      this.$emit("startExperiment");
    },
    handleImportExperiment() {
      this.$emit("handleImportExperiment");
    }
  },
}
</script>

<style lang="scss">
.zero-state {
  & .terracotta-appbg {
    background: url("~@/assets/terracotta_appbg.jpg") no-repeat center center;
    background-size: cover;
    height: 100%;
    width: 100%;
    position: fixed;
    top: 0;
    left: 0;
    opacity: 0.5;
  }
  & .terracotta-appbg + * {
    position: relative; /*place the content above the terracotta-appbg*/
  }
  & div.v-tooltip__content {
    max-width: 400px;
    opacity: 1.0 !important;
    background-color: rgba(55,61,63, 1.0) !important;
    a {
      color: #afdcff;
    }
  }
  & .alert-request {
    min-width: 100%;
    z-index: 1000;
    > .v-alert {
      margin: 0 auto;
      & a {
        color: white;
      }
    }
  }
  & h1 {
    &.experimental-header {
      font-size: 48px;
      font-weight: 200;
      text-align: center;
    }
  }
  & button {
    &.experiment-btn {
      margin: 0 auto;
      max-width: fit-content;
    }
  }
  & a {
    &.user-help-link {
      color: rgba(0, 0, 0, .87) !important;
    }
  }
}
</style>
