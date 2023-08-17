<template>
  <div
    class="container-summary"
  >
    <SummaryData
      :title="title"
      :value="experienced.type"
      :message="tooltip"
      :icon="headerIcon"
      :iconBgColor="`#E8E6FE`"
      :valueFontSize="valueFontSize"
    />
    <p>Exposed to {{ experienced.description }} condition</p>
  </div>
</template>

<script>
import icon from "@/assets/experimenttype.svg";
import SummaryData from "./components/SummaryData.vue";

export default {
  name: "Exposures",
  props: [
    "exposureType"
  ],
  components: {
    SummaryData
  },
  data: () => ({
    tooltip: "Fun exposures summary tooltip here...",
    headerIcon: icon,
    title: "Experiment type",
    valueFontSize: "1em"
  }),
  computed: {
    experienced() {
      switch(this.exposureType) {
        case "BETWEEN":
          return {
            type: "Between-Subject",
            description: "one"
          };
        case "WITHIN":
          return {
            type: "Within-Subject",
            description: "every"
          };
        default:
          return {
            type: "N/A",
            description: "N/A"
          }
      }
    }
  }
}
</script>

<style scoped>
p {
    max-width: fit-content;
    align-self: flex-start;
    font-size: small;
  }
</style>
