<template>
<v-btn-toggle
  v-model="newSelection"
  color="primary"
  mandatory
  dense
>
  <v-btn
    :value="leftOption"
    :disabled="readOnly"
  >
    {{ leftOption }}
  </v-btn>
  <v-btn
    :value="rightOption"
    :disabled="readOnly"
  >
    {{ rightOption }}
  </v-btn>
</v-btn-toggle>
</template>

<script>
export default {
  props: {
    selectedOption: {
      type: String
    },
    options: {
      type: Array,
      required: true
    },
    readOnly: {
      type: Boolean,
      default: false
    }
  },
  data: () => ({
    newSelection: null
  }),
  computed: {
    leftOption() {
      return this.options[0];
    },
    rightOption() {
      return this.options[1];
    }
  },
  watch: {
    selectedOption: {
      handler (newValue) {
        this.newSelection = newValue;
      },
      immediate: true
    },
    newSelection: {
      handler(newValue) {
        this.$emit("update", newValue);
      },
      immediate: false
    }
  }
}
</script>
