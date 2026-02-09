<template>
<div
  class="container-summary"
>
  <summary-data
    :title="title"
    :value="count"
    :message="tooltip"
    :icon="headerIcon"
    iconBgColor="#e5faff"
  />
</div>
</template>

<script>
import icon from "@/assets/conditions.svg";
import SummaryData from "./components/SummaryData.vue";

export default {
  name: "Conditions",
  components: {
    SummaryData
  },
  props: {
    conditionCount: {
      type: Number,
      required: false,
      default: 0
    }
  },
  data: () => ({
    tooltip: "",
    headerIcon: icon,
    title: "Conditions"
  }),
  computed: {
    count() {
      return this.conditionCount;
    }
  }
}
</script>
