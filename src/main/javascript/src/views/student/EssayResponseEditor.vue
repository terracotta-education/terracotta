<template>
<response-row>
  <v-textarea
    v-if="!readonly"
    v-model="response"
    :rows="10"
    :counter="true"
    @input="onInput"
  >
    <template
      #counter
    >
      <div
        class="counter"
      >
        {{ wordCount }} word{{ wordCount !== 1 ? "s" : "" }}
      </div>
    </template>
  </v-textarea>
  <v-textarea
    v-if="readonly"
    v-model="studentResponse"
    @input="onInput"
    :rows="10"
    :counter="true"
    readonly
  >
  </v-textarea>
</response-row>
</template>

<script>
import Countable from "countable";
import ResponseRow from "./ResponseRow.vue";

export default {
  name: "EssayResponseEditor",
  components: {
    ResponseRow
  },
  props: [
    "value",
    "readonly",
    "answer"
  ],
  data: () => ({
    response: this.value,
    wordCount: 0
  }),
  watch: {
    value() {
      this.response = this.value;

      if (this.response) {
        this.updateWordCount();
      }
    },
  },
  computed: {
    studentResponse() {
      return this.answer?.response;
    }
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
  }
};
</script>

<style lang="scss" scoped>
.counter {
  font-size: 16px;
  line-height: 16px;
  font-weight: 400;
}
</style>
