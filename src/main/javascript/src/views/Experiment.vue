<template>
  <router-view :key="route.fullPath" />
</template>

<script setup>
import { useRoute } from "vue-router";

defineOptions({
  name: "ExperimentPage"
});

const route = useRoute();
</script>
