<template>
<div>
  <v-switch
    v-model="selection"
    :disabled="readOnly || !enabledByConsent"
    :ripple="false"
    label="Send messages to consented individuals only"
    inset
  />
</div>
</template>

<script>
export default {
  props: {
    selected: {
      type: Boolean
    },
    experiment: {
      type: Object,
      required: true
    },
    readOnly: {
      type: Boolean,
      default: false
    }
  },
  data: () => ({
    selection: null
  }),
  watch: {
    selected: {
      handler(newSelected) {
        if (!this.enabledByConsent) {
          this.selection = false;
          return;
        }

        this.selection = newSelected;
      },
      immediate: true
    },
    selection: {
      handler (newSelection) {
        this.$emit("updated", newSelection);
      },
      immediate: false
    }
  },
  computed: {
    enabledByConsent() {
      return this.experiment?.participationType === "CONSENT" || false;
    }
  },
}
</script>
