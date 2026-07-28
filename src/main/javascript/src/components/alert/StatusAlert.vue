<template>
  <v-alert
    :model-value="alertStore.hasAlert"
    :type="alertStore.alertType"
    class="status-alert"
    closable
    variant="outlined"
    @update:model-value="handleDismiss"
  >
    {{ alertStore.alertMessage }}
  </v-alert>
</template>

<script setup>
import { alert } from "@/store/alert.module";

defineOptions({
  name: "StatusAlert"
});

const alertStore = alert();

const handleDismiss = () => {
  alertStore.clear();
};
</script>

<style scoped lang="scss">
.status-alert {
  max-width: fit-content;
  margin: 0 auto;
}
</style>
