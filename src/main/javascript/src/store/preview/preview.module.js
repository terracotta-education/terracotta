import {previewService} from "@/services";

const state = {}

const actions = {
  async treatment({state}, payload) {
    // payload = experimentId, conditionId, treatmentId, previewId, ownerId
    try {
      const response = await previewService.treatmentPreview(...payload);

      if (response) {
        return response.data;
      }
    } catch (error) {
      console.error("treatment catch", {error, state});
    }
  }
}

export const preview = {
  namespaced: true,
  state,
  actions
}
