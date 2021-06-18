<template>
  <div>
    <h1 class="my-3">
      Which students can participate in the study?
    </h1>

    <!-- Groups Section -->
    <p>Groups</p>
    <v-expansion-panels class="v-expansion-panels--icon" flat>
      <v-expansion-panel>
        <v-expansion-panel-header>
          Participating ({{ participating.length }})
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <ListParticipants
            :listOfParticipants="participating"
            :moveToHandler="moveToHandler"
            :moveToOptions="moveToOptions"
            selectedOption="0"
          />
        </v-expansion-panel-content>
      </v-expansion-panel>

      <v-expansion-panel>
        <v-expansion-panel-header>
          Not Participating ({{ notParticipating.length }})
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <ListParticipants
            :listOfParticipants="notParticipating"
            :moveToOptions="moveToOptions"
            :moveToHandler="moveToHandler"
            selectedOption="1"
          />
        </v-expansion-panel-content>
      </v-expansion-panel>

      <v-expansion-panel>
        <v-expansion-panel-header>
          Unassigned ({{ unassigned.length }})
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <ListParticipants
            :listOfParticipants="unassigned"
            :moveToOptions="moveToOptions"
            :moveToHandler="moveToHandler"
            selectedOption="2"
          />
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>

    <div class="mt-5">
      <v-btn elevation="0" color="primary" @click="submitParticipants()"
        >Continue
      </v-btn>
    </div>
  </div>
</template>

<script>
import ListParticipants from "../../../components/ListParticipants.vue";
import { mapActions, mapGetters } from "vuex";
import store from "@/store";

export default {
  name: "ParticipationTypeManualSelection",
  props: ["experiment"],
  components: {
    ListParticipants,
  },
  data() {
    return {
      moveToOptions: ["Participating", "Not Participating", "Unassigned"],
    };
  },
  computed: {
    ...mapGetters({
      participants: "participants/participants",
    }),

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
      
      if (option === "Participating") {
        updatedParticipants = this.updateParticipantConsent(selectedIds, true);
      }

      if (option === "Not Participating") {
        updatedParticipants = this.updateParticipantConsent(
          selectedIds,
          false
        );
      }

      if (option === "Unassigned") {
        updatedParticipants = this.updateParticipantConsent(selectedIds, null);
      }
      this.setParticipantsGroup(updatedParticipants);
    },

    submitParticipants() {
      const _this = this;

      _this
        .updateParticipants(_this.experiment.experimentId)
        .then((response) => {
          if (response?.status === 200) {
            _this.$router.push({
              name: "ParticipationDistribution",
              params: { experiment: this.experiment.experimentId },
            });
          } else {
            alert(response.error);
          }
        })
        .catch((response) => {
          console.log("updateConditions | catch", { response });
        });
    },
  },
  beforeRouteEnter(to, from, next) {
    // don't load new data after participant selection screen
    return store
      .dispatch("participants/fetchParticipants", to.params.experiment_id)
      .then(next, next);
  },
  beforeRouteUpdate(to, from, next) {
    // don't load new data after participant selection screen
    return store
      .dispatch("participants/fetchParticipants", to.params.experiment_id)
      .then(next, next);
  },
};
</script>
