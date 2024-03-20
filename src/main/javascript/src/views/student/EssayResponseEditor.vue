<template>
  <response-row>
    <v-textarea v-model="response" @input="onInput" :rows="10" :counter="true" v-if="!readonly">
      <template #counter>
        <div class="counter">
          {{ wordCount }} word{{ wordCount !== 1 ? "s" : "" }}
        </div>
      </template>
    </v-textarea>
    <v-textarea v-model="studentResponse" @input="onInput" :rows="10" :counter="true" v-if="readonly" readonly></v-textarea>
  </response-row>
</template>

<script>
import Countable from "countable";
import ResponseRow from "./ResponseRow.vue";

export default {
  props: ["value", "readonly", "answer"],
  components: { ResponseRow },
  computed: {
    studentResponse() {
      return this.answer?.response;
    }
  },
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
