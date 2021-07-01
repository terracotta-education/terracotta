<template>
  <div class="mt-4" v-if="listOfParticipants.length > 0">
    <v-row class="mx-3">
      <v-row align="center" class="mx-3">
        <v-checkbox
          on-icon="$checkboxIndeterminate"
          color="primary"
          :value="tempSelectedInAGroup.length > 0"
          @change="handleOnChange(tempSelectedInAGroup.length)"
        />
        {{ tempSelectedInAGroup.length }} Selected
      </v-row>
      <!-- Dropdown Menu -->
      <v-menu offset-y>
        <template v-slot:activator="{ on, attrs }">
          <v-btn color="primary" v-bind="attrs" v-on="on">MOVE TO</v-btn>
        </template>
        <v-list>
          <v-list-item-group>
            <template v-for="(option, index) in moveToOptions">
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
    <v-list class="mt-5" outlined rounded>
      <v-list-item-group v-model="tempSelectedInAGroup" multiple>
        <template v-for="(participant, index) in listOfParticipants">
          <v-list-item :key="participant.userId" :value="participant">
            <v-list-item-action>
              <v-checkbox
                color="primary"
                :input-value="tempSelectedInAGroup.includes(participant)"
              ></v-checkbox>
            </v-list-item-action>

            <v-list-item-content>
              <v-list-item-title>
                {{ participant.user.displayName }}
              </v-list-item-title>
            </v-list-item-content>
          </v-list-item>
          <v-divider
            v-if="index !== listOfParticipants.length - 1"
            class="mx-4"
            :key="participant.userId"
          />
        </template>
      </v-list-item-group>
    </v-list>
  </div>
</template>
<script>
export default {
  name: "ListParticipants",
  props: [
    "listOfParticipants",
    "moveToOptions",
    "moveToHandler",
    "selectedOption",
  ],
  data() {
    return {
      tempSelectedInAGroup: [],
    };
  },
  methods: {
    moveToHandlerComponent(option, tempSelectedInAGroup) {
      this.tempSelectedInAGroup = [];

      this.moveToHandler(option, tempSelectedInAGroup);
    },
    handleOnChange(value) {
      if (value === 0) {
        this.tempSelectedInAGroup = this.listOfParticipants;
      } else {
        this.tempSelectedInAGroup = [];
      }
    },
  },
};
</script>

<style lang="scss">
// Edge Case - When Selecting and Unselecting All options button,
//             and then when a participant is selected, Minus sign
//             is displaying in different color
.mdi-minus-box {
  color: #1d9dff !important;
}
</style>
