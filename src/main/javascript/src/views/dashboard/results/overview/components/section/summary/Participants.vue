<template>
  <div
    class="container-summary"
  >
    <SummaryData
      :title="title"
      :value="count"
      :message="tooltip"
      :icon="headerIcon"
      :iconBgColor="`#FEF8E6`"
      :showTooltip="true"
    />
    <PercentBar
      :value="toPercent(consentRate)"
      class="progress-bar"
    />
  </div>
</template>

<script>
import { percent } from "@/helpers/dashboard/utils.js";
import icon from "@/assets/participants.svg";
import PercentBar from "./components/PercentBar.vue";
import SummaryData from "./components/SummaryData.vue";

export default {
  name: "Participants",
  props: [
    "participantsData"
  ],
  components: {
    PercentBar,
    SummaryData
},
  data: () => ({
    tooltip: "The number of people who consented to participate in the experiment",
    headerIcon: icon,
    title: "Participants"
  }),
  computed: {
    sectionData() {
        return this.participantsData || {};
    },
    count() {
      return this.sectionData.count || 0;
    },
    enrollment() {
      return this.sectionData.classEnrollment || 0;
    },
    consentRate() {
      return this.sectionData.consentRate || 0;
    }
  },
  methods: {
    toPercent(value) {
      return percent(value);
    }
  }
}
</script>

<style scoped>
.progress-bar {
  width: 90%;
}
</style>
