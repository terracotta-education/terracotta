import { defineStore } from "pinia";

import { experimentService } from "@/services";

const IMPORT_STATUS_FLAGS = importRequest => ({
  ...importRequest,
  complete: importRequest?.status === "COMPLETE",
  completeAcknowledged:
    importRequest?.status === "COMPLETE_ACKNOWLEDGED",
  error: importRequest?.status === "ERROR",
  errorAcknowledged:
    importRequest?.status === "ERROR_ACKNOWLEDGED",
  processing: importRequest?.status === "PROCESSING"
});

export const experiment = defineStore("experiment", {
  state: () => ({
    experiment: null,
    experiments: [],
    importRequests: []
  }),

  getters: {
    conditions: state => state.experiment?.conditions || [],

    hasExperiment: state => Boolean(state.experiment),

    hasExperiments: state => state.experiments.length > 0
  },

  actions: {
    resetExperiment() {
      this.experiment = null;
      this.importRequests = [];
    },

    resetExperiments() {
      this.experiments = [];
      this.importRequests = [];
    },

    async createExperiment() {
      try {
        return await experimentService.create();
      } catch (error) {
        console.error("experiment/createExperiment | catch", error);

        return null;
      }
    },

    async fetchExperimentById(experimentId) {
      try {
        const response =
          await experimentService.getById(experimentId);

        if (response?.status === 200) {
          this.setExperiment(response.data);
        }

        return response;
      } catch (error) {
        console.error(
          "experiment/fetchExperimentById | catch",
          error
        );

        return null;
      }
    },

    async fetchExperiments() {
      try {
        const response = await experimentService.getAll();

        if (response?.status === 200) {
          this.experiments = response.data;
        }

        return response;
      } catch (error) {
        console.error(
          "experiment/fetchExperiments | catch",
          error
        );

        return null;
      }
    },

    async updateExperiment(experiment) {
      try {
        const response =
          await experimentService.update(experiment);

        if (response?.status === 200) {
          this.setExperiment(experiment);
        }

        return response;
      } catch (error) {
        console.error(
          "experiment/updateExperiment | catch",
          error
        );

        return null;
      }
    },

    async deleteExperiment(experimentId) {
      try {
        const response =
          await experimentService.delete(experimentId);

        if (response?.status === 200) {
          this.experiments = this.experiments.filter(
            e => e.experimentId !== experimentId
          );
        }

        return response;
      } catch (error) {
        console.error(
          "experiment/deleteExperiment | catch",
          error
        );

        return null;
      }
    },

    async exportExperiment(experimentId) {
      try {
        return await experimentService.export(experimentId);
      } catch (error) {
        console.error(
          "experiment/exportExperiment | catch",
          error
        );

        return null;
      }
    },

    async importExperiment(payload) {
      try {
        const importRequest =
          await experimentService.import(payload);

        this.upsertImportRequest(importRequest);

        return importRequest;
      } catch (error) {
        console.error(
          "experiment/importExperiment | catch",
          error
        );

        return null;
      }
    },

    async pollImport(payload) {
      try {
        const importRequest =
          await experimentService.pollImport(payload);

        this.upsertImportRequest(importRequest?.data);

        return importRequest;
      } catch (error) {
        console.error(
          "experiment/pollImport | catch",
          error
        );

        return null;
      }
    },

    async pollImports() {
      try {
        const importRequests =
          await experimentService.pollImports();

        this.importRequests = (importRequests?.data || [])
          .filter(r => r?.id)
          .map(IMPORT_STATUS_FLAGS);

        return importRequests;
      } catch (error) {
        console.error(
          "experiment/pollImports | catch",
          error
        );

        return null;
      }
    },

    async acknowledgeImport(payload) {
      try {
        await experimentService.acknowledgeImport(...payload);
        this.importRequests = [];
      } catch (error) {
        console.error(
          "experiment/acknowledgeImport | catch",
          error
        );
      }
    },

    resetImportRequests() {
      this.importRequests = [];
    },

    setExperiment(experiment) {
      this.experiment = experiment;

      if (!experiment?.experimentId) {
        return;
      }

      const index = this.experiments.findIndex(
        item => item.experimentId === experiment.experimentId
      );

      if (index >= 0) {
        this.experiments.splice(index, 1, experiment);
      } else {
        this.experiments.push(experiment);
      }
    },

    setConditions(conditions) {
      if (!this.experiment) {
        return;
      }

      this.experiment = {
        ...this.experiment,
        conditions
      };
    },

    setCondition(condition) {
      if (!this.experiment?.conditions) {
        return;
      }

      const index = this.experiment.conditions.findIndex(
        item => item.conditionId === condition.conditionId
      );

      if (index >= 0) {
        this.experiment.conditions.splice(index, 1, condition);
      } else {
        this.experiment.conditions.push(condition);
      }
    },

    deleteCondition(condition) {
      if (!this.experiment?.conditions) {
        return;
      }

      this.experiment = {
        ...this.experiment,
        conditions: this.experiment.conditions.filter(
          item => item.conditionId !== condition.conditionId
        )
      };
    },

    upsertImportRequest(importRequest) {
      if (!importRequest?.id) {
        return;
      }

      const normalizedRequest = IMPORT_STATUS_FLAGS(importRequest);

      const index = this.importRequests.findIndex(
        item => item.id === normalizedRequest.id
      );

      if (index >= 0) {
        this.importRequests.splice(index, 1, normalizedRequest);
      } else {
        this.importRequests.push(normalizedRequest);
      }
    }
  }
});
