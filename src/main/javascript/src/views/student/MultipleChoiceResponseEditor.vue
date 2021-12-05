<template>
  <div>
    <template v-for="answer in answers">
      <response-row :key="answer.answerId">
        <div class="question-input">
          <v-radio-group v-model="response" @change="emitValueChanged">
            <v-radio class="radioButton" :value="answer.answerId"> </v-radio>
          </v-radio-group>
          <span v-html="answer.html"></span>
        </div>
      </response-row>
    </template>
  </div>
</template>

<script>
import ResponseRow from "./ResponseRow.vue";

export default {
  props: ["answers", "value"],
  components: { ResponseRow },
  data() {
    return {
      response: this.value,
    };
  },
  methods: {
    emitValueChanged() {
      this.$emit("input", this.response);
    },
  },
  watch: {
    value() {
      this.response = this.value;
    },
  },
};
</script>

<style lang="scss" scoped>
.question-input {
  display: flex;
  flex-direction: row;
  align-items: center;
}
.radioButton {
  margin-top: 2px;
}
</style>
