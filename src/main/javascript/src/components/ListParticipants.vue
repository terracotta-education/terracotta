<template>
<div
  v-if="participantsList.length > 0"
  class="list-participants-container mt-4"
>
  <v-row
    class="mx-3 mb-2"
  >
    <v-row
      align="center"
      class="mx-6"
    >
      <v-checkbox
        @change="handleOnChange(tempSelectedInAGroup.length)"
        :value="tempSelectedInAGroup.length > 0"
        aria-label="select all participants"
        on-icon="$checkboxIndeterminate"
        color="primary"
      />
      {{ tempSelectedInAGroup.length }} Selected
    </v-row>
    <!-- Dropdown Menu -->
    <v-menu
      offset-y
    >
      <template
        v-slot:activator="{ on, attrs }"
      >
        <v-btn
          v-bind="attrs"
          v-on="on"
          color="primary"
        >
          MOVE TO
        </v-btn>
      </template>
      <v-list>
        <v-list-item-group
          aria-label="select destination to move participants to"
        >
          <template
            v-for="(option, index) in moveToOptions"
          >
            <v-list-item
              v-if="index.toString() !== selectedOption"
              :key="option"
            >
              <v-list-item-action
                v-on:click="
                  moveToHandlerComponent(option, tempSelectedInAGroup)
                "
              >
                <v-list-item-content>
                  <v-list-item-title>{{ option }}</v-list-item-title>
                </v-list-item-content>
              </v-list-item-action>
            </v-list-item>
          </template>
        </v-list-item-group>
      </v-list>
    </v-menu>
  </v-row>
  <!-- List of available participants -->
  <v-list-item-group
    v-model="tempSelectedInAGroup"
    aria-label="select participants to move to another group"
    class="participant-list-group mx-3 px-2"
    multiple
  >
    <template>
      <div
        v-for="(participant) in participantsList"
        :key="participant.userId"
        class="participant-item-container my-2"
      >
        <v-list-item
          :value="participant"
          class="participant-item"
        >
          <v-icon
            :color="tempSelectedInAGroup.includes(participant) ? 'primary' : 'grey'"
          >
            {{ tempSelectedInAGroup.includes(participant) ? "mdi-checkbox-marked" : "mdi-checkbox-blank-outline" }}
            </v-icon>
          <v-list-item-title>
            {{ participant.user.displayName }}
          </v-list-item-title>
        </v-list-item>
      </div>
    </template>
  </v-list-item-group>
</div>
</template>

<script>
export default {
  name: "ListParticipants",
  props: {
    listOfParticipants: {
      type: Array,
      required: true
    },
    moveToOptions: {
      type: Array,
      required: true
    },
    moveToHandler: {
      type: Function,
      required: true
    },
    selectedOption: {
      type: String,
      required: true
    }
  },
  data: () => ({
    tempSelectedInAGroup: [],
    participantsList: []
  }),
  watch: {
    listOfParticipants() {
      this.participantsList = this.listOfParticipants;
    }
  },
  methods: {
    moveToHandlerComponent(option, tempSelectedInAGroup) {
      this.tempSelectedInAGroup = [];
      this.moveToHandler(option, tempSelectedInAGroup);
    },
    handleOnChange(value) {
      this.tempSelectedInAGroup = (value === 0) ?
      this.listOfParticipants : []
    }
  },
  async mounted() {
    this.participantsList = this.listOfParticipants;
  }
};
</script>

<style lang="scss" scoped>
// Edge Case - When Selecting and Unselecting All options button, and then when a participant is selected, minus sign is displaying in different color
.mdi-minus-box {
  color: map-get($blue, "base") !important;
}
.participant-list-group {
  border: 1px solid lightgrey;
  border-radius: 10px;
  & .participant-item-container {
    border-radius: 24px;
    overflow: hidden;
    & .participant-item {
      border-bottom: 1px solid lightgrey;
    }
  }
}
</style>
