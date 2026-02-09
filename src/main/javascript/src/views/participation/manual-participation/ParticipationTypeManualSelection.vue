<template>
<div>
  <h1
    class="my-3"
  >
    Which students can participate in the study?
  </h1>
  <!-- Groups Section -->
  <p>Groups</p>
  <v-expansion-panels
    class="v-expansion-panels--icon"
    flat
  >
    <v-expansion-panel
      @click="panelExpansion"
    >
      <v-expansion-panel-header>
        Participating ({{ participating.length }})
      </v-expansion-panel-header>
      <v-expansion-panel-content>
        <list-participants
          :listOfParticipants="participating"
          :moveToHandler="moveToHandler"
          :moveToOptions="moveToOptions"
          selectedOption="0"
        />
      </v-expansion-panel-content>
    </v-expansion-panel>
    <v-expansion-panel
      @click="panelExpansion"
    >
      <v-expansion-panel-header>
        Not participating ({{ notParticipating.length }})
      </v-expansion-panel-header>
      <v-expansion-panel-content>
        <list-participants
          :listOfParticipants="notParticipating"
          :moveToOptions="moveToOptions"
          :moveToHandler="moveToHandler"
          selectedOption="1"
        />
      </v-expansion-panel-content>
    </v-expansion-panel>
    <v-expansion-panel
      @click="panelExpansion"
    >
      <v-expansion-panel-header>
        Unassigned ({{ unassigned.length }})
      </v-expansion-panel-header>
      <v-expansion-panel-content>
        <list-participants
          :listOfParticipants="unassigned"
          :moveToOptions="moveToOptions"
          :moveToHandler="moveToHandler"
          selectedOption="2"
        />
      </v-expansion-panel-content>
    </v-expansion-panel>
  </v-expansion-panels>
  <div
    class="mt-5"
  >
    <v-btn
      @click="submitParticipants(nextPage('ParticipationDistribution'))"
      elevation="0"
      color="primary"
    >
      Continue
    </v-btn>
  </div>
</div>
</template>

<script>
import { mapActions, mapGetters } from "vuex";
import { deleteAttributesFromElement } from "@/helpers/ui-utils.js";
import ListParticipants from "@/components/ListParticipants.vue";
import store from "@/store";

export default {
  name: "ParticipationTypeManualSelection",
  props: {
    experiment: {
      type: Object,
      required: true
    }
  },
  components: {
    ListParticipants,
  },
  data: () => ({
    moveToOptions: [
      "Participating",
      "Not participating",
      "Unassigned"
      ],
  }),
  computed: {
    ...mapGetters({
      participants: "participants/participants",
      editMode: "navigation/editMode"
    }),
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || "Home";
    },
    experimentId() {
      return this.experiment.experimentId;
    },
    participating() {
      return this.groupParticipants(true);
    },
    notParticipating() {
      return this.groupParticipants(false);
    },
    unassigned() {
      return this.groupParticipants(null);
    },
    conditions() {
        return this.experiment.conditions;
    },
    singleConditionExperiment() {
        return this.conditions.length === 1;
    }
  },
  methods: {
    ...mapActions({
      fetchParticipants: "participants/fetchParticipants",
      setParticipantsGroup: "participants/setParticipantsGroup",
      updateParticipants: "participants/updateParticipants",
    }),
    groupParticipants(value) {
      return this.participants.filter(
        (participant) => participant.consent === value
      );
    },
    getParticipantIds(participants) {
      return participants.map((participant) => participant.user.userId);
    },
    updateParticipantConsent(selectedIds, value) {
      return this.participants.map((participant) => {
        if (selectedIds.includes(participant.user.userId)) {
          participant.consent = value;
        }
        return participant;
      });
    },
    moveToHandler(option, tempSelected) {
      const selectedIds = this.getParticipantIds(tempSelected);
      let updatedParticipants = [];

      switch (option) {
        case "Participating":
          updatedParticipants = this.updateParticipantConsent(selectedIds, true);
          break;
        case "Not participating":
          updatedParticipants = this.updateParticipantConsent(
            selectedIds,
            false
          );
          break;
        case "Unassigned":
          updatedParticipants = this.updateParticipantConsent(selectedIds, null);
          break;
      }

      this.setParticipantsGroup(updatedParticipants);
    },
    submitParticipants(path) {
      const _this = this;

      _this
        .updateParticipants(_this.experiment.experimentId)
        .then((response) => {
          if (response?.status === 200) {
            _this.$router.push({
              name: path,
              params: { experiment: this.experiment.experimentId },
            });
          } else {
            this.$swal(response.error);
          }
        })
        .catch((response) => {
          console.log("submitParticipants | catch", { response });
        });
    },
    saveExit() {
      this.submitParticipants(this.getSaveExitPage)
    },
    nextPage(toPage) {
      if (this.singleConditionExperiment) {
          return "ParticipationSummary";
      }

      return toPage;
    },
    panelExpansion() {
      setTimeout(() => {
        deleteAttributesFromElement(".v-expansion-panel", ["aria-expanded"]);
      }, 1000);
    }
  },
  async created() {
    await this.fetchParticipants(this.experiment.experimentId);
  },
  mounted() {
    deleteAttributesFromElement(".v-expansion-panel", ["aria-expanded"]);
  },
  beforeRouteUpdate(to, from, next) {
    // don't load new data after participant selection screen
    return store
      .dispatch("participants/fetchParticipants", to.params.experimentId)
      .then(next, next);
  },
};
</script>
