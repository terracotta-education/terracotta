<template>
<tool-tip
  v-if="helpUrl"
  :url="helpUrl"
  icon="mdi-help-circle-outline"
  icon-label="Get help icon"
  icon-color="primary"
  aria-label="Get help here"
  content="Link to Terracotta Knowledge Base"
  alignment="bottom"
/>
</template>

<script>
import { mapGetters } from "vuex";
import ToolTip from "@/components/ToolTip.vue";

export default {
  name: "Help",
  components: {
    ToolTip
  },
  computed: {
    ...mapGetters({
      configurations: "configuration/get"
    }),
    helpUrl() {
      return this.configurations?.helpUrl;
    }
  }
}
</script>
