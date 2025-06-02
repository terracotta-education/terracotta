<template>
<div
  v-if="isSingleConditionExperiment"
>
  <v-btn
    v-if="hasExisting"
    @click="handleVersionSelection('single')"
    color="primary"
    elevation="0"
  >
    Add Assignment
  </v-btn>
  <v-btn
    v-else
    @click="handleVersionSelection('single')"
    v-on="on"
    v-bind="attrs"
    class="btn-create-first-assignment"
    elevation="0"
  >
    Create Assignment
  </v-btn>
</div>
<div
  v-else
>
  <v-menu
    v-model="addAssignmentDialogOpen"
    :close-on-content-click="false"
    :open-on-click="true"
    :open-on-hover="false"
    content-class="add-assignment-dialog"
    transition="scale-transition"
    origin="right top"
    bottom
    left
    offset-y
  >
    <template
      v-slot:activator="{ on, attrs }"
    >
      <v-btn
        v-if="hasExisting"
        v-on="on"
        v-bind="attrs"
        color="primary"
        elevation="0"
        :disabled="disableAddAssignmentButton"
      >
        Add Assignment
      </v-btn>
      <v-btn
        v-else
        v-on="on"
        v-bind="attrs"
        class="btn-create-first-assignment"
        elevation="0"
      >
        Create Assignment
      </v-btn>
    </template>
    <span class="add-assignment-dialog">
      <div class="add-assignment-version-option">
        <v-btn
          @click="handleVersionSelection('multiple')"
          color="primary"
          elevation="0"
        >
          With Different Versions
        </v-btn>
        <p>
          Create <u>multiple</u> treatments of your assignment so your students can experience different conditions.
        </p>
      </div>
      <div class="add-assignment-version-option">
        <v-btn
          @click="handleVersionSelection('single')"
          color="primary"
          elevation="0"
        >
          With Only One Version
        </v-btn>
        <p>
          Create <u>one</u> assignment so all students experience the same condition (e.g., a questionnaire).
        </p>
      </div>
    </span>
  </v-menu>
</div>
</template>

<script>
export default {
  name: "AddAssignmentDialog",
  props: {
    hasExisting: {
      type: Boolean,
      default: false
    },
    isSingleConditionExperiment: {
      type: Boolean,
      default: false
    }
  },
  data: () => ({
    addAssignmentDialogOpen: false,
    disableAddAssignmentButton: false
  }),
  watch: {
    addAssignmentDialogOpen: {
      handler(newVal) {
        this.disableAddAssignmentButton = newVal;
      }
    }
  },
  methods: {
    handleVersionSelection(version) {
      this.$emit(version);
    }
  }
}
</script>

<style lang="scss" scoped>
.btn-create-first-assignment {
  border-radius: 24px;
  width: fit-content;
  min-height: 48px;
  background-color: white !important;
  border: 1px solid;
}
div.v-menu__content.add-assignment-dialog {
  width: 350px;
  background-color: white;
  padding: 5px 5px 0 5px;
  > span {
    > div.add-assignment-version-option {
      border: thin solid lightgrey;
      border-radius: 5px;
      padding: 5px;
      margin-bottom: 5px;
      text-align: center;
      > p {
        margin-bottom: 0 !important;
        padding-bottom: 0 !important;
        text-align: left;
      }
    }
  }
}
</style>
