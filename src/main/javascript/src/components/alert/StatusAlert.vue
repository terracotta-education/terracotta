<template>
<v-alert
  :value="hasAlert"
  :type="alert.type"
  @input="handleDismiss"
  class="status-alert"
  dismissible
  outlined
  text
>
  {{ alert.message }}
</v-alert>
</template>

<script>
import { mapActions, mapGetters } from "vuex";

export default {
  name: "StatusAlert",
  computed: {
    ...mapGetters({
      alert: "alert/alert"
    }),
    hasAlert() {
      return this.alert != null && this.alert.message && this.alert.type;
    }
  },
  methods: {
    ...mapActions({
      clearAlert: "alert/clearAlert"
    }),
    handleDismiss() {
      this.clearAlert();
    }
  }
}
</script>

<style scoped lang="scss">
.status-alert {
  max-width: fit-content;
  margin: 0 auto;
}
</style>