<template>
  <!-- only once the alert is closed (or if it couldn't be shown), so the page isn't blank after it -->
  <v-alert
    v-if="showMessage"
    class="student-course-launch ma-4"
    type="info"
    variant="outlined"
  >
    {{ COURSE_LAUNCH_MESSAGE }}
  </v-alert>
</template>

<script setup>
import { onMounted, ref } from "vue";
import Swal from "sweetalert2";

import { COURSE_LAUNCH_MESSAGE } from "./student-course-launch";

defineOptions({
  name: "StudentCourseLaunch"
});

const emit = defineEmits(["loaded"]);

const showMessage = ref(false);

// a student who opens Terracotta from the course navigation instead of an assignment: there's
// nothing to open here, so point them to where their Terracotta assignments are
onMounted(async () => {
  emit("loaded");

  try {
    await Swal.fire({
      target: "#app",
      icon: "info",
      text: COURSE_LAUNCH_MESSAGE
    });
  } catch (error) {
    console.error("StudentCourseLaunch | the alert couldn't be shown", error);
  } finally {
    showMessage.value = true;
  }
});
</script>
