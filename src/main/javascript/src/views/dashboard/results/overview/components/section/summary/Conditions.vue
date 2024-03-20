<template>
  <div
    class="container-summary"
  >
    <SummaryData
      :title="title"
      :value="count"
      :message="tooltip"
      :icon="headerIcon"
      :iconBgColor="`#E5FAFF`"
    />
  </div>
</template>

<script>
import icon from "@/assets/conditions.svg";
import SummaryData from "./components/SummaryData.vue";

export default {
  name: "Conditions",
  props: [
    "conditionCount"
  ],
  components: {
    SummaryData
  },
  data: () => ({
    tooltip: "Fun conditions summary tooltip here...",
    headerIcon: icon,
    title: "Conditions"
  }),
  computed: {
    count() {
      return this.conditionCount || 0;
    }
  }
}
</script>
