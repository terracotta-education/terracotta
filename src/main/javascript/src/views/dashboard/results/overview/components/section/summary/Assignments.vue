<template>
<div
  class="container-summary"
>
  <summary-data
    :title="title"
    :value="count"
    :message="tooltip"
    :icon="headerIcon"
    iconBgColor="#fef3e7"
  />
</div>
</template>

<script>
import icon from "@/assets/assignments.svg";
import SummaryData from "./components/SummaryData.vue";

export default {
  name: "Assignments",
  components: {
    SummaryData
  },
  props: {
    assignmentCount: {
      type: Number,
      required: false,
      default: 0
    }
  },

  data: () => ({
    tooltip: "",
    headerIcon: icon,
    title: "Components"
  }),
  computed: {
    count() {
      return this.assignmentCount;
    }
  }
}
</script>
