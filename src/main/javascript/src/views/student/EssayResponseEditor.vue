<template>
  <response-row>
    <v-textarea v-model="response" @input="onInput" :rows="10" :counter="true">
      <template #counter>
        <div class="counter">
          {{ wordCount }} word{{ wordCount !== 1 ? "s" : "" }}
        </div>
      </template>
    </v-textarea>
  </response-row>
</template>

<script>
import ResponseRow from "./ResponseRow.vue";
import Countable from "countable";

export default {
  props: ["value"],
  components: { ResponseRow },
  data() {
    return {
      response: this.value,
      wordCount: 0,
    };
  },
  methods: {
    onInput() {
      this.emitValueChanged();
      this.updateWordCount();
    },
    emitValueChanged() {
      this.$emit("input", this.response);
    },
    updateWordCount() {
      Countable.count(this.response, (counter) => {
        this.wordCount = counter.words;
      });
    },
  },
  watch: {
    value() {
      this.response = this.value;
      if (this.response) {
        this.updateWordCount();
      }
    },
  },
};
</script>

<style lang="scss" scoped>
.counter {
  font-size: 16px;
  line-height: 16px;
  font-weight: 400;
}
</style>
