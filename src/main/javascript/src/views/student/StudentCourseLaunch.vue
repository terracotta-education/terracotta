<template>
  <v-alert
    class="student-course-launch ma-4"
    type="info"
    variant="outlined"
  >
    {{ COURSE_LAUNCH_MESSAGE }}
  </v-alert>
</template>

<script setup>
import { onMounted } from "vue";
import Swal from "sweetalert2";

import { COURSE_LAUNCH_MESSAGE } from "./student-course-launch";

defineOptions({
  name: "StudentCourseLaunch"
});

const emit = defineEmits(["loaded"]);

// a student who opens Terracotta from the course navigation instead of an assignment: there's
// nothing to open here, so point them to where their Terracotta assignments are
onMounted(() => {
  emit("loaded");
  Swal.fire({
    target: "#app",
    icon: "info",
    text: COURSE_LAUNCH_MESSAGE
  });
});
</script>
