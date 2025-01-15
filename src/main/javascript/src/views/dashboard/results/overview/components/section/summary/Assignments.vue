<template>
  <div
    class="container-summary"
  >
    <SummaryData
      :title="title"
      :value="count"
      :message="tooltip"
      :icon="headerIcon"
      :iconBgColor="`#FEF3E7`"
    />
  </div>
</template>

<script>
import icon from "@/assets/assignments.svg";
import SummaryData from "./components/SummaryData.vue";

export default {
  name: "Assignments",
  props: [
    "assignmentCount"
  ],
  components: {
    SummaryData
  },
  data: () => ({
    tooltip: "Fun assignments summary tooltip here...",
    headerIcon: icon,
    title: "Components"
  }),
  computed: {
    count() {
      return this.assignmentCount || 0;
    }
  }
}
</script>
