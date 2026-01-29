<template>
  <div
    class="px-3"
  >
    <v-alert
      v-if="alertType === alertData.types.initial"
      v-model="alertDisplay"
      type="error"
      icon="mdi-alert-circle-outline"
      class="alert initial my-0"
      elevation="0"
      dismissible
      outlined
      dense
    >
      <p>
        <b>This attempt will expire on {{ alertData.date }}.</b>
      </p>
      <p>
        If you can't complete your assignment now and still have time before it's due,
        <b>begin a new timed session by closing and reopening this assignment</b>.
        Any unfinished work will be lost if you exit, or if the allotted time expires, before you submit.
      </p>
    </v-alert>
    <v-alert
      v-if="alertType === alertData.types.warning"
      v-model="alertDisplay"
      type="warning"
      icon="mdi-clock-outline"
      class="alert warning my-0"
      elevation="0"
      outlined
      dense
    >
      <template v-slot:close="{ }">
        <v-icon
          @click="handleWarningMinimize()"
        >
          {{  warningMinimized ? "mdi-chevron-down" : "mdi-chevron-up" }}
        </v-icon>
      </template>
      <p>
        <b>This attempt will expire in <span class="warn-expiration-time">{{ alertData.date }}</span>.</b>
      </p>
      <p
        v-if="!warningMinimized"
      >
        If you can't complete your assignment now and still have time before it's due,
        <b>begin a new timed session by closing and reopening this assignment</b>.
        Any unfinished work will be lost if you exit, or if the allotted time expires, before you submit.
      </p>
    </v-alert>
    <v-alert
      v-if="alertType === alertData.types.expired"
      v-model="alertDisplay"
      type="error"
      icon="mdi-close-circle-outline"
      class="alert expired my-0"
      elevation="0"
      outlined
      dense
    >
      <p>
        <b><span class="warn-expiration-time">This attempt has expired.</span></b>
        Your session ended on {{ alertData.date }}.
        If you attempt to submit now, your work will be lost. Copy any completed work into a document, click the Assignment tab in the menu on the left to open a new, blank assignment.
      </p>
    </v-alert>
  </div>
</template>

<script>
export default {
  props: {
    alert:{
      type: Object,
      required: true
    }
  },
  data: () => ({
    alertData: null,
    warningMinimized: false
  }),
  watch: {
    alert: {
      handler(newAlert) {
        this.alertData = newAlert;
      },
      immediate: true,
      deep: true
    },
    alertType: {
      handler(newType, oldType) {
        if (newType !== oldType) {
          this.alertDisplay = true;
        }
      },
      immediate: true
    }
  },
  computed: {
    alertType: {
      get() {
        return this.alertData.type;
      },
      set(value) {
        this.alertData.type = value;
      }
    },
    alertDisplay: {
      get() {
        return this.alertData.display;
      },
      set(value) {
        this.alertData.display = value;
      }
    }
  },
  methods: {
    handleWarningMinimize() {
      this.alertDisplay = true;
      this.warningMinimized = !this.warningMinimized;
    }
  }
};
</script>

<style lang="scss" scoped>
.alert {
  position: sticky;
  top: 0;
  z-index: 9999;
  min-width: 100%;
  max-height: fit-content;
  &.initial {
    background-color: #fdf3f8 !important;
    border: 1px solid #ffd6ea !important;
  }
  &.warning {
    background-color: #fff7ed !important;
    border: 1px solid #fed8aa !important;
  }
  &.expired {
    background-color: #fef3f2 !important;
    border: 1px solid #fecaca !important;
  }
  & p {
    margin-bottom: 0;
    padding-bottom: 0;
    color: #273540 !important;
  }
  & .warn-expiration-time {
    color: #c3420d;
  }
}
</style>
