import { defineStore } from "pinia";

import { participantService } from "@/services";

export const participants = defineStore("participants", {
  state: () => ({
    participants: [],
    participant: null,
    groups: []
  }),

  getters: {
    hasParticipants: state => state.participants.length > 0
  },

  actions: {
    // returns null (rather than []) on failure, so callers can tell "fetch failed" apart from
    // "genuinely no participants" and show an error instead of silently rendering an empty list
    async fetchParticipants(payload) {
      // payload = experimentId, refresh
      try {
        const data = await participantService.getAll(...payload);

        // a failed HTTP response resolves to an error object (e.g. {status, error}) rather than
        // throwing, so an array check is needed here to catch that case as a failure too
        if (!Array.isArray(data)) {
          console.error(
            "participants/fetchParticipants | non-array response",
            { data }
          );

          this.participants = [];

          return null;
        }

        this.participants = data;

        return this.participants;
      } catch (error) {
        console.error(
          "participants/fetchParticipants | catch",
          error
        );

        this.participants = [];

        return null;
      }
    },

    setParticipantsGroup(participantsList) {
      this.participants = participantsList;
    },

    setParticipants(participantsList) {
      this.setParticipantsGroup(participantsList);
    },

    async updateParticipants(experimentId) {
      try {
        const requestBody = this.participants.map(p => ({
          participantId: p.participantId,
          consent: p.consent,
          dropped: p.dropped,
          groupId: p.groupId
        }));

        return await participantService.updateParticipants(
          experimentId,
          requestBody
        );
      } catch (error) {
        console.error(
          "participants/updateParticipants | catch",
          error
        );

        return null;
      }
    },

    async updateParticipant(payload) {
      try {
        const { experimentId, participantData } = payload;

        const response = await participantService.updateParticipant(
          experimentId,
          participantData
        );

        this.participant = participantData;
        this.upsertParticipant(participantData);

        return response;
      } catch (error) {
        console.error(
          "participants/updateParticipant | catch",
          error
        );

        return null;
      }
    },

    resetParticipants() {
      this.participants = [];
      this.participant = null;
      this.groups = [];
    },

    upsertParticipant(participantData) {
      if (!participantData?.participantId) {
        return;
      }

      const index = this.participants.findIndex(
        item =>
          parseInt(item.participantId) ===
          parseInt(participantData.participantId)
      );

      if (index >= 0) {
        this.participants.splice(index, 1, participantData);
      } else {
        this.participants.push(participantData);
      }
    }
  }
});
